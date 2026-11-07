-- Endereço completo: estado e CEP, preenchidos pelo CEP (ViaCEP) no cadastro e na criação de eventos.
-- Opcionais, pois os registros anteriores não têm esses dados.
ALTER TABLE events            ADD COLUMN state VARCHAR(2), ADD COLUMN cep VARCHAR(8);
ALTER TABLE ngo_profiles      ADD COLUMN state VARCHAR(2), ADD COLUMN cep VARCHAR(8);
ALTER TABLE volunteer_profiles ADD COLUMN state VARCHAR(2), ADD COLUMN cep VARCHAR(8);
