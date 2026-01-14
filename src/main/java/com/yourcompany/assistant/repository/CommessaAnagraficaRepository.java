package com.yourcompany.assistant.repository;

import com.yourcompany.assistant.model.CommessaAnagrafica;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommessaAnagraficaRepository extends MongoRepository<CommessaAnagrafica, String> {

    /**
     * Trova tutte le commesse attive
     */
    List<CommessaAnagrafica> findByActiveTrue();

    /**
     * Trova una commessa per nome (case insensitive)
     */
    CommessaAnagrafica findByNameIgnoreCase(String name);
}
