package com.yourcompany.assistant.controller;

import com.yourcompany.assistant.model.RichiestaFatturazione;
import com.yourcompany.assistant.repository.RichiestaFatturazioneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/richieste-fatturazione")
@RequiredArgsConstructor
public class RichiestaFatturazioneController {

    private final RichiestaFatturazioneRepository richiestaRepository;

    @GetMapping
    public ResponseEntity<List<RichiestaFatturazione>> getAll() {
        List<RichiestaFatturazione> richieste = richiestaRepository.findAll();
        return ResponseEntity.ok(richieste);
    }

    @GetMapping("/by-periodo")
    public ResponseEntity<List<RichiestaFatturazione>> getByPeriodo(
            @RequestParam String mese,
            @RequestParam String anno) {
        List<RichiestaFatturazione> richieste = richiestaRepository.findByMeseAndAnno(mese, anno);
        return ResponseEntity.ok(richieste);
    }

    @GetMapping("/by-cliente")
    public ResponseEntity<List<RichiestaFatturazione>> getByCliente(@RequestParam String nomeCliente) {
        List<RichiestaFatturazione> richieste = richiestaRepository.findByNomeCliente(nomeCliente);
        return ResponseEntity.ok(richieste);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        if (!richiestaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        richiestaRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
