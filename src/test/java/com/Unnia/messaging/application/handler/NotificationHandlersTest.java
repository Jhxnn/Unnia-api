package com.Unnia.messaging.application.handler;

import com.Unnia.events.domain.event.EventCancelled;
import com.Unnia.events.domain.event.EventRescheduled;
import com.Unnia.events.domain.event.RegistrationCancelled;
import com.Unnia.events.domain.event.VolunteerRegistered;
import com.Unnia.events.domain.model.Period;
import com.Unnia.identity.domain.event.NgoRejected;
import com.Unnia.identity.domain.event.NgoVerified;
import com.Unnia.messaging.application.Notifier;
import com.Unnia.messaging.application.fakes.FakeMessagingDirectories;
import com.Unnia.messaging.application.fakes.InMemoryNotificationRepository;
import com.Unnia.messaging.application.port.ParticipantDirectory.Kind;
import com.Unnia.messaging.domain.event.MessageSent;
import com.Unnia.messaging.domain.model.ConversationId;
import com.Unnia.messaging.domain.model.MessageId;
import com.Unnia.messaging.domain.model.Notification;
import com.Unnia.messaging.domain.model.NotificationType;
import com.Unnia.shared.application.MutableClock;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.domain.id.EventId;
import com.Unnia.shared.domain.id.NgoId;
import com.Unnia.shared.domain.id.VolunteerId;
import com.Unnia.social.domain.event.AccountFollowed;
import com.Unnia.social.domain.event.CommentPosted;
import com.Unnia.social.domain.event.ReactionAdded;
import com.Unnia.social.domain.model.CommentId;
import com.Unnia.social.domain.model.PostId;
import com.Unnia.social.domain.model.ReactionId;
import com.Unnia.social.domain.model.ReactionTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationHandlersTest {

	private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");

	private FakeMessagingDirectories directories;
	private InMemoryNotificationRepository notifications;
	private Notifier notifier;

	@BeforeEach
	void setUp() {
		directories = new FakeMessagingDirectories();
		notifications = new InMemoryNotificationRepository();
		notifier = new Notifier(notifications, new MutableClock(NOW));
	}

	private Notification onlyNotification() {
		assertThat(notifications.all()).hasSize(1);
		return notifications.all().get(0);
	}

	@Test
	void eventHandlersExposeTheirEventType() {
		assertThat(new VolunteerRegisteredHandler(directories, notifier).eventType()).isEqualTo(VolunteerRegistered.class);
		assertThat(new RegistrationCancelledHandler(directories, notifier).eventType())
				.isEqualTo(RegistrationCancelled.class);
		assertThat(new EventCancelledHandler(directories, notifier).eventType()).isEqualTo(EventCancelled.class);
		assertThat(new EventRescheduledHandler(directories, notifier).eventType()).isEqualTo(EventRescheduled.class);
		assertThat(new NgoVerifiedHandler(notifier).eventType()).isEqualTo(NgoVerified.class);
		assertThat(new NgoRejectedHandler(notifier).eventType()).isEqualTo(NgoRejected.class);
		assertThat(new AccountFollowedHandler(notifier).eventType()).isEqualTo(AccountFollowed.class);
		assertThat(new CommentPostedHandler(directories, notifier).eventType()).isEqualTo(CommentPosted.class);
		assertThat(new ReactionAddedHandler(directories, notifier).eventType()).isEqualTo(ReactionAdded.class);
		assertThat(new MessageSentHandler(notifier).eventType()).isEqualTo(MessageSent.class);
	}

	@Test
	void notifiesTheNgoWhenAVolunteerRegistersOrCancels() {
		AccountId ngoAccount = directories.account(Kind.NGO);
		NgoId ngoId = directories.ngoOf(ngoAccount);
		EventId eventId = EventId.generate();
		VolunteerId volunteer = VolunteerId.generate();

		new VolunteerRegisteredHandler(directories, notifier).handle(new VolunteerRegistered(eventId, ngoId, volunteer));
		new RegistrationCancelledHandler(directories, notifier)
				.handle(new RegistrationCancelled(eventId, ngoId, volunteer));

		assertThat(notifications.all()).extracting(Notification::type)
				.containsExactly(NotificationType.VOLUNTEER_REGISTERED, NotificationType.REGISTRATION_CANCELLED);
		assertThat(notifications.all()).allSatisfy(n -> {
			assertThat(n.recipientId()).isEqualTo(ngoAccount);
			assertThat(n.referenceId()).contains(eventId.value());
		});
	}

	@Test
	void ignoresEventsWhoseRecipientCannotBeResolved() {
		new VolunteerRegisteredHandler(directories, notifier)
				.handle(new VolunteerRegistered(EventId.generate(), NgoId.generate(), VolunteerId.generate()));
		new EventCancelledHandler(directories, notifier)
				.handle(new EventCancelled(EventId.generate(), List.of(VolunteerId.generate())));

		assertThat(notifications.all()).isEmpty();
	}

	@Test
	void notifiesEveryAffectedVolunteerWhenEventIsCancelledOrRescheduled() {
		AccountId first = directories.account(Kind.VOLUNTEER);
		AccountId second = directories.account(Kind.VOLUNTEER);
		List<VolunteerId> volunteers = List.of(directories.volunteerOf(first), directories.volunteerOf(second));
		EventId eventId = EventId.generate();

		new EventCancelledHandler(directories, notifier).handle(new EventCancelled(eventId, volunteers));
		new EventRescheduledHandler(directories, notifier).handle(new EventRescheduled(eventId,
				Period.of(NOW.plusSeconds(3600), NOW.plusSeconds(7200)), volunteers));

		assertThat(notifications.all()).hasSize(4);
		assertThat(notifications.all()).extracting(Notification::type).containsExactlyInAnyOrder(
				NotificationType.EVENT_CANCELLED, NotificationType.EVENT_CANCELLED,
				NotificationType.EVENT_RESCHEDULED, NotificationType.EVENT_RESCHEDULED);
		assertThat(notifications.countUnreadFor(first)).isEqualTo(2);
		assertThat(notifications.countUnreadFor(second)).isEqualTo(2);
	}

	@Test
	void notifiesTheNgoAccountAboutVerificationDecision() {
		AccountId ngoAccount = AccountId.generate();
		NgoId ngoId = NgoId.generate();

		new NgoVerifiedHandler(notifier).handle(new NgoVerified(ngoId, ngoAccount));
		new NgoRejectedHandler(notifier).handle(new NgoRejected(ngoId, ngoAccount));

		assertThat(notifications.all()).extracting(Notification::type)
				.containsExactly(NotificationType.NGO_VERIFIED, NotificationType.NGO_REJECTED);
		assertThat(notifications.all()).allSatisfy(n -> assertThat(n.recipientId()).isEqualTo(ngoAccount));
	}

	@Test
	void notifiesTheFollowedAccount() {
		AccountId follower = AccountId.generate();
		AccountId followee = AccountId.generate();

		new AccountFollowedHandler(notifier).handle(new AccountFollowed(follower, followee));

		Notification notification = onlyNotification();
		assertThat(notification.recipientId()).isEqualTo(followee);
		assertThat(notification.type()).isEqualTo(NotificationType.NEW_FOLLOWER);
		assertThat(notification.referenceId()).contains(follower.value());
	}

	@Test
	void notifiesThePostAuthorOfCommentsButNotOfTheirOwn() {
		AccountId author = AccountId.generate();
		AccountId commenter = AccountId.generate();
		PostId postId = directories.postBy(author);
		CommentPostedHandler handler = new CommentPostedHandler(directories, notifier);

		handler.handle(new CommentPosted(CommentId.generate(), postId, author));
		assertThat(notifications.all()).isEmpty();

		handler.handle(new CommentPosted(CommentId.generate(), postId, commenter));
		Notification notification = onlyNotification();
		assertThat(notification.recipientId()).isEqualTo(author);
		assertThat(notification.type()).isEqualTo(NotificationType.NEW_COMMENT);
	}

	@Test
	void notifiesTheContentAuthorOfReactionsButNotSelfReactions() {
		AccountId postAuthor = AccountId.generate();
		AccountId commentAuthor = AccountId.generate();
		AccountId fan = AccountId.generate();
		PostId postId = directories.postBy(postAuthor);
		CommentId commentId = directories.commentBy(commentAuthor);
		ReactionAddedHandler handler = new ReactionAddedHandler(directories, notifier);

		handler.handle(new ReactionAdded(ReactionId.generate(), postAuthor, ReactionTarget.post(postId)));
		assertThat(notifications.all()).isEmpty();

		handler.handle(new ReactionAdded(ReactionId.generate(), fan, ReactionTarget.post(postId)));
		handler.handle(new ReactionAdded(ReactionId.generate(), fan, ReactionTarget.comment(commentId)));
		handler.handle(new ReactionAdded(ReactionId.generate(), fan, ReactionTarget.post(PostId.generate())));

		assertThat(notifications.all()).extracting(Notification::recipientId).containsExactly(postAuthor, commentAuthor);
		assertThat(notifications.all()).allSatisfy(n -> assertThat(n.type()).isEqualTo(NotificationType.NEW_REACTION));
	}

	@Test
	void notifiesTheRecipientOfANewMessage() {
		AccountId sender = AccountId.generate();
		AccountId recipient = AccountId.generate();
		ConversationId conversationId = ConversationId.generate();

		new MessageSentHandler(notifier).handle(new MessageSent(MessageId.generate(), conversationId, sender, recipient));

		Notification notification = onlyNotification();
		assertThat(notification.recipientId()).isEqualTo(recipient);
		assertThat(notification.type()).isEqualTo(NotificationType.NEW_MESSAGE);
		assertThat(notification.referenceId()).contains(conversationId.value());
	}
}
