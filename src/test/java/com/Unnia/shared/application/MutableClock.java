package com.Unnia.shared.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Relógio de teste que pode avançar, para exercitar ordenação e janelas de tempo. */
public class MutableClock extends Clock {

	private Instant now;

	public MutableClock(Instant start) {
		this.now = start;
	}

	public void advance(Duration duration) {
		now = now.plus(duration);
	}

	@Override
	public ZoneId getZone() {
		return ZoneOffset.UTC;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		return this;
	}

	@Override
	public Instant instant() {
		return now;
	}
}
