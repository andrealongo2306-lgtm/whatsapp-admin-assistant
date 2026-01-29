package com.yourcompany.assistant.controller;

import com.yourcompany.assistant.dto.CommessaAnagraficaDto;
import com.yourcompany.assistant.model.CommessaAnagrafica;
import com.yourcompany.assistant.repository.CommessaAnagraficaRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/commesse")
@RequiredArgsConstructor
public class CommessaAnagraficaController {

    private final CommessaAnagraficaRepository commessaRepository;

    @GetMapping
    public ResponseEntity<List<CommessaAnagrafica>> getAll() {
        return ResponseEntity.ok(commessaRepository.findAll());
    }

    @GetMapping("/active")
    public ResponseEntity<List<CommessaAnagrafica>> getActive() {
        return ResponseEntity.ok(commessaRepository.findByActiveTrue());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        Optional<CommessaAnagrafica> commessa = commessaRepository.findById(id);

        if (commessa.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Commessa non trovata"));
        }

        return ResponseEntity.ok(commessa.get());
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CommessaAnagraficaDto dto) {
        CommessaAnagrafica existing = commessaRepository.findByNameIgnoreCase(dto.getName());
        if (existing != null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Esiste già una commessa con questo nome"));
        }

        CommessaAnagrafica commessa = CommessaAnagrafica.builder()
                .name(dto.getName())
                .tariffa(dto.getTariffa())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();

        CommessaAnagrafica saved = commessaRepository.save(commessa);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @Valid @RequestBody CommessaAnagraficaDto dto) {
        Optional<CommessaAnagrafica> existingOpt = commessaRepository.findById(id);

        if (existingOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Commessa non trovata"));
        }

        // Verifica nome duplicato (escluso se stesso)
        CommessaAnagrafica byName = commessaRepository.findByNameIgnoreCase(dto.getName());
        if (byName != null && !byName.getId().equals(id)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Esiste già una commessa con questo nome"));
        }

        CommessaAnagrafica existing = existingOpt.get();
        existing.setName(dto.getName());
        existing.setTariffa(dto.getTariffa());
        existing.setActive(dto.getActive() != null ? dto.getActive() : existing.getActive());

        CommessaAnagrafica saved = commessaRepository.save(existing);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        if (!commessaRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Commessa non trovata"));
        }

        commessaRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Commessa eliminata"));
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<?> toggleActive(@PathVariable String id) {
        Optional<CommessaAnagrafica> existingOpt = commessaRepository.findById(id);

        if (existingOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Commessa non trovata"));
        }

        CommessaAnagrafica commessa = existingOpt.get();
        commessa.setActive(!commessa.getActive());

        CommessaAnagrafica saved = commessaRepository.save(commessa);
        return ResponseEntity.ok(saved);
    }
}
