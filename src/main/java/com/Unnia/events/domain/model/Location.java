package com.Unnia.events.domain.model;

import com.Unnia.shared.domain.Guard;
import com.Unnia.shared.domain.vo.City;

public record Location(String address, City city) {

	public Location {
		address = Guard.maxLength(Guard.notBlank(address, "Endereço é obrigatório"), 200,
				"Endereço deve ter no máximo 200 caracteres");
		Guard.notNull(city, "Cidade é obrigatória");
	}

	public static Location of(String address, City city) {
		return new Location(address, city);
	}
}
