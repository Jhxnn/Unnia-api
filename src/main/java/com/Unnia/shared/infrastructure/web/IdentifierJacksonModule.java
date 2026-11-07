package com.Unnia.shared.infrastructure.web;

import com.Unnia.shared.domain.Identifier;
import org.springframework.stereotype.Component;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

/** Serializa qualquer id do domínio como uma string simples, em vez de {@code {"value": "..."}}. */
@Component
public class IdentifierJacksonModule extends SimpleModule {

	public IdentifierJacksonModule() {
		addSerializer(Identifier.class, new ValueSerializer<Identifier>() {
			@Override
			public void serialize(Identifier id, JsonGenerator generator, SerializationContext context) {
				generator.writeString(id.value().toString());
			}
		});
	}
}
