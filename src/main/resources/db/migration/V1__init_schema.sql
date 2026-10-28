-- Unnia: schema inicial, um bloco por bounded context.

-- ---------------------------------------------------------------- identity
CREATE TABLE accounts (
    id            UUID         PRIMARY KEY,
    email         VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('NGO', 'VOLUNTEER', 'ADMIN')),
    status        VARCHAR(20)  NOT NULL CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);

CREATE TABLE ngo_profiles (
    id                  UUID          PRIMARY KEY,
    account_id          UUID          NOT NULL UNIQUE REFERENCES accounts (id),
    cnpj                VARCHAR(14)   NOT NULL UNIQUE,
    name                VARCHAR(120)  NOT NULL,
    description         VARCHAR(2000) NOT NULL,
    city                VARCHAR(80)   NOT NULL,
    verification_status VARCHAR(20)   NOT NULL CHECK (verification_status IN ('PENDING', 'VERIFIED', 'REJECTED'))
);

CREATE TABLE volunteer_profiles (
    id         UUID          PRIMARY KEY,
    account_id UUID          NOT NULL UNIQUE REFERENCES accounts (id),
    name       VARCHAR(120)  NOT NULL,
    bio        VARCHAR(2000) NOT NULL,
    city       VARCHAR(80)   NOT NULL
);

-- ------------------------------------------------------------------ events
CREATE TABLE events (
    id          UUID          PRIMARY KEY,
    owner_id    UUID          NOT NULL REFERENCES ngo_profiles (id),
    title       VARCHAR(150)  NOT NULL,
    description VARCHAR(2000) NOT NULL,
    address     VARCHAR(200)  NOT NULL,
    city        VARCHAR(80)   NOT NULL,
    starts_at   TIMESTAMPTZ   NOT NULL,
    ends_at     TIMESTAMPTZ   NOT NULL,
    capacity    INTEGER       NOT NULL CHECK (capacity >= 1),
    status      VARCHAR(20)   NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED', 'FINISHED')),
    version     BIGINT        NOT NULL DEFAULT 0,
    CHECK (ends_at > starts_at)
);
CREATE INDEX idx_events_owner ON events (owner_id);
CREATE INDEX idx_events_discovery ON events (status, starts_at);

CREATE TABLE event_registrations (
    id            UUID        PRIMARY KEY,
    event_id      UUID        NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    volunteer_id  UUID        NOT NULL REFERENCES volunteer_profiles (id),
    registered_at TIMESTAMPTZ NOT NULL,
    status        VARCHAR(20) NOT NULL CHECK (status IN ('CONFIRMED', 'CANCELLED', 'ATTENDED'))
);
-- um voluntário só pode ter uma inscrição ativa por evento
CREATE UNIQUE INDEX uq_active_registration
    ON event_registrations (event_id, volunteer_id) WHERE status <> 'CANCELLED';
CREATE INDEX idx_registrations_volunteer ON event_registrations (volunteer_id);

-- ------------------------------------------------------------------ social
CREATE TABLE posts (
    id              UUID          PRIMARY KEY,
    author_id       UUID          NOT NULL REFERENCES accounts (id),
    content         VARCHAR(3000) NOT NULL,
    linked_event_id UUID          REFERENCES events (id),
    created_at      TIMESTAMPTZ   NOT NULL,
    edited_at       TIMESTAMPTZ,
    status          VARCHAR(20)   NOT NULL CHECK (status IN ('ACTIVE', 'DELETED'))
);
CREATE INDEX idx_posts_feed ON posts (author_id, created_at DESC);

CREATE TABLE post_media (
    post_id  UUID         NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    position INTEGER      NOT NULL,
    url      VARCHAR(500) NOT NULL,
    type     VARCHAR(10)  NOT NULL CHECK (type IN ('IMAGE', 'VIDEO')),
    PRIMARY KEY (post_id, position)
);

CREATE TABLE comments (
    id         UUID          PRIMARY KEY,
    post_id    UUID          NOT NULL REFERENCES posts (id),
    parent_id  UUID          REFERENCES comments (id),
    author_id  UUID          NOT NULL REFERENCES accounts (id),
    content    VARCHAR(1000) NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL,
    status     VARCHAR(20)   NOT NULL CHECK (status IN ('ACTIVE', 'DELETED'))
);
CREATE INDEX idx_comments_post ON comments (post_id, created_at);

CREATE TABLE reactions (
    id          UUID        PRIMARY KEY,
    author_id   UUID        NOT NULL REFERENCES accounts (id),
    target_type VARCHAR(10) NOT NULL CHECK (target_type IN ('POST', 'COMMENT')),
    target_id   UUID        NOT NULL,
    type        VARCHAR(10) NOT NULL CHECK (type IN ('LIKE', 'LOVE', 'SUPPORT')),
    created_at  TIMESTAMPTZ NOT NULL,
    UNIQUE (author_id, target_type, target_id)
);
CREATE INDEX idx_reactions_target ON reactions (target_type, target_id);

CREATE TABLE follows (
    id          UUID        PRIMARY KEY,
    follower_id UUID        NOT NULL REFERENCES accounts (id),
    followee_id UUID        NOT NULL REFERENCES accounts (id),
    since       TIMESTAMPTZ NOT NULL,
    UNIQUE (follower_id, followee_id),
    CHECK (follower_id <> followee_id)
);
CREATE INDEX idx_follows_followee ON follows (followee_id);

-- --------------------------------------------------------------- messaging
-- participant_a < participant_b garante uma única conversa por par de contas.
CREATE TABLE conversations (
    id               UUID        PRIMARY KEY,
    participant_a    UUID        NOT NULL REFERENCES accounts (id),
    participant_b    UUID        NOT NULL REFERENCES accounts (id),
    started_at       TIMESTAMPTZ NOT NULL,
    last_activity_at TIMESTAMPTZ NOT NULL,
    UNIQUE (participant_a, participant_b),
    CHECK (participant_a < participant_b)
);
CREATE INDEX idx_conversations_b ON conversations (participant_b);

CREATE TABLE messages (
    id              UUID          PRIMARY KEY,
    conversation_id UUID          NOT NULL REFERENCES conversations (id),
    sender_id       UUID          NOT NULL REFERENCES accounts (id),
    recipient_id    UUID          NOT NULL REFERENCES accounts (id),
    content         VARCHAR(2000) NOT NULL,
    sent_at         TIMESTAMPTZ   NOT NULL,
    read_at         TIMESTAMPTZ
);
CREATE INDEX idx_messages_conversation ON messages (conversation_id, sent_at DESC);
CREATE INDEX idx_messages_unread ON messages (recipient_id) WHERE read_at IS NULL;

CREATE TABLE notifications (
    id           UUID         PRIMARY KEY,
    recipient_id UUID         NOT NULL REFERENCES accounts (id),
    type         VARCHAR(40)  NOT NULL,
    text         VARCHAR(300) NOT NULL,
    reference_id UUID,
    created_at   TIMESTAMPTZ  NOT NULL,
    read_at      TIMESTAMPTZ
);
CREATE INDEX idx_notifications_recipient ON notifications (recipient_id, created_at DESC);
CREATE INDEX idx_notifications_unread ON notifications (recipient_id) WHERE read_at IS NULL;

-- -------------------------------------------------------------- moderation
CREATE TABLE reports (
    id          UUID          PRIMARY KEY,
    reporter_id UUID          NOT NULL REFERENCES accounts (id),
    target_type VARCHAR(10)   NOT NULL CHECK (target_type IN ('POST', 'COMMENT', 'ACCOUNT')),
    target_id   UUID          NOT NULL,
    reason      VARCHAR(30)   NOT NULL,
    details     VARCHAR(1000) NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL,
    status      VARCHAR(10)   NOT NULL CHECK (status IN ('OPEN', 'RESOLVED', 'DISMISSED')),
    reviewed_by UUID          REFERENCES accounts (id),
    reviewed_at TIMESTAMPTZ
);
CREATE INDEX idx_reports_status ON reports (status, created_at);
-- uma denúncia aberta por denunciante e alvo
CREATE UNIQUE INDEX uq_open_report ON reports (reporter_id, target_type, target_id) WHERE status = 'OPEN';
