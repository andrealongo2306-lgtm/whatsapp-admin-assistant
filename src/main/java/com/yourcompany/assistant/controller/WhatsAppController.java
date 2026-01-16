package com.yourcompany.assistant.controller;

import com.yourcompany.assistant.service.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller per gestire i webhook di Twilio WhatsApp
 */
@RestController
@RequestMapping("/api/whatsapp")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppController {
    
    private final ConversationService conversationService;
    
    /**
     * Endpoint webhook per ricevere messaggi WhatsApp da Twilio
     * 
     * Twilio invia i dati come application/x-www-form-urlencoded
     */
    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> handleIncomingMessage(@RequestParam Map<String, String> payload) {
        try {
            log.info("Ricevuto webhook da Twilio: {}", payload);
            
            // Estrai i parametri dal payload
            String from = payload.get("From"); // es: whatsapp:+39123456789
            String body = payload.get("Body"); // Testo del messaggio
            String messageSid = payload.get("MessageSid");
            
            // Validazione
            if (from == null || body == null) {
                log.warn("Payload incompleto ricevuto: {}", payload);
                return ResponseEntity.badRequest().build();
            }
            
            log.info("Messaggio ricevuto da {}: {}", from, body);
            
            // Processa il messaggio in modo asincrono per rispondere velocemente a Twilio
            conversationService.processMessage(from, body);
            
            // Twilio si aspetta una risposta 200 OK entro 15 secondi
            // Risposta vuota per evitare che Twilio invii un messaggio automatico
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("Errore nel processamento del webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Endpoint per verificare che il server sia attivo
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "WhatsApp Admin Assistant"
        ));
    }
    
    /**
     * Endpoint di test per simulare l'invio di un messaggio
     * (utile per testing locale senza Twilio)
     */
    @PostMapping("/test-message")
    public ResponseEntity<String> testMessage(
            @RequestParam String phoneNumber,
            @RequestParam String message) {
        try {
            log.info("Test message: from={}, body={}", phoneNumber, message);
            conversationService.processMessage(phoneNumber, message);
            return ResponseEntity.ok("Test message processed");
        } catch (Exception e) {
            log.error("Error in test message: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
}
