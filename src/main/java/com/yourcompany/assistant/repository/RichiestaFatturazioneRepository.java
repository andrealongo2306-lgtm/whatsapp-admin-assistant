package com.yourcompany.assistant.repository;

import com.yourcompany.assistant.model.RichiestaFatturazione;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RichiestaFatturazioneRepository extends MongoRepository<RichiestaFatturazione, String> {

    List<RichiestaFatturazione> findByMeseAndAnno(String mese, String anno);

    List<RichiestaFatturazione> findByNomeCliente(String nomeCliente);
}
