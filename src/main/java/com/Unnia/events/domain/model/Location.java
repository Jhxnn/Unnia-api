package com.Unnia.events.domain.model;

import com.Unnia.shared.domain.Guard;
import com.Unnia.shared.domain.vo.Cep;
import com.Unnia.shared.domain.vo.City;
import com.Unnia.shared.domain.vo.StateCode;

/**
 * Onde o evento acontece. Estado e CEP são opcionais, porque eventos criados antes do preenchimento por CEP
 * não os têm; quando informados, são validados.
 */
public record Location(String address, City city, StateCode state, Cep cep) {

	public Location {
		address = Guard.maxLength(Guard.notBlank(address, "Endereço é obrigatório"), 200,
				"Endereço deve ter no máximo 200 caracteres");
		Guard.notNull(city, "Cidade é obrigatória");
	}

	public Location(String address, City city) {
		this(address, city, null, null);
	}

	public static Location of(String address, City city) {
		return new Location(address, city);
	}

	public static Location of(String address, City city, StateCode state, Cep cep) {
		return new Location(address, city, state, cep);
	}
}
