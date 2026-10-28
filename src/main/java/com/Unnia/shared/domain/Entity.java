package com.Unnia.shared.domain;

import java.util.Objects;

public abstract class Entity<ID> {

	private final ID id;

	protected Entity(ID id) {
		this.id = Guard.notNull(id, "Identidade é obrigatória");
	}

	public ID id() {
		return id;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		return id.equals(((Entity<?>) other).id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(getClass(), id);
	}
}
