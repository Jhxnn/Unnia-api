package com.Unnia.shared.domain;

import java.util.UUID;

/** Identidade tipada de um agregado ou entidade; todos os ids do domínio a implementam. */
public interface Identifier {

	UUID value();
}
