# whatsapp-admin-assistant — System Wiki

## Identità del sistema

Applicazione Spring Boot 3.2.1 (Java 17) per la gestione amministrativa delle richieste di fatturazione mensili tramite conversazione WhatsApp. L'admin invia le giornate lavorate per ogni commessa via WhatsApp; il sistema compila e invia una email riepilogativa. Include anche un chatbot AI (Claude) per interrogare i dati di fatturazione.

**Owner:** Andrea Longo  
**Repo:** andrealongo2306-lgtm/whatsapp-admin-assistant  
**Branch principale:** main  
**Build/Test:** `mvn test`  
**Avvio locale:** `mvn spring-boot:run`  
**Porta:** 8080

---

## Stack tecnico

| Componente | Tecnologia |
|---|---|
| Framework | Spring Boot 3.2.1 |
| Database | MongoDB Atlas (`whatsapp_assistant` db) |
| Cache | Redis (TTL 10 min) |
| Messaggistica | Twilio WhatsApp API |
| Email | Gmail API (OAuth2) |
| AI | Claude API (`claude-sonnet-4-20250514`) |
| Sicurezza | Spring Security + JWT (HMAC-SHA, 24h) |
| Build | Maven |

---

## Struttura package

```
com.yourcompany.assistant
  config/        SecurityConfig, RedisConfig, TwilioConfig, ClaudeConfig
  controller/    AuthController, WhatsAppController, ChatbotController,
                 CommessaAnagraficaController, RichiestaFatturazioneController
  service/       ConversationService, TwilioService, EmailService, JwtService,
                 CommessaAnagraficaService, RichiestaFatturazioneService,
                 ChatbotService, ClaudeApiService, MongoQueryService,
                 UserDetailsServiceImpl
  repository/    UserRepository, ConversationRepository,
                 CommessaAnagraficaRepository, RichiestaFatturazioneRepository
  model/         User, Conversation, CommessaAnagrafica, RichiestaFatturazione, Commessa
  dto/           AuthRequest, AuthResponse, RegisterRequest,
                 ChatRequest, ChatResponse, CommessaAnagraficaDto, EmailDraft
  enums/         ConversationState
  scheduler/     SessionCleanupScheduler
  security/      JwtAuthenticationFilter
```

---

## Pattern architetturali

- **Layer rigorosi**: Controller → Service → Repository. Mai saltare layer.
- **Logica transazionale**: solo nel layer Service.
- **DTO obbligatori**: i controller non espongono mai entità MongoDB direttamente.
- **Validazione input**: sui DTO con Jakarta Validation (`@NotBlank`, `@Size`, `@Positive`).
- **Eccezioni**: gestite tramite `@ControllerAdvice` globale — mai try/catch nei controller.
- **Cache Redis**: `CommessaAnagraficaService` e `RichiestaFatturazioneService` usano `@Cacheable`/`@CacheEvict`. Ogni operazione di scrittura invalida la cache.
- **Logging**: SLF4J via `@Slf4j` (Lombok). Mai `System.out.println`.

---

## Modelli dati (MongoDB)

### User (`users`)
```
id (String, @Id), username (unique, indexed), password (BCrypt),
fullName, role (default "USER"), enabled (default true), createdAt
```

### Conversation (`conversations`)
```
phoneNumber (String, @Id — chiave primaria), currentState (ConversationState),
month, year, currentCommessaIndex (int), commesseAttiveIds (List<String>),
commesse (List<Commessa> embedded), lastUpdated, createdAt
```
Metodi rilevanti: `addCommessa()`, `hasMoreCommesseToProcess()`, `reset()`

### CommessaAnagrafica (`commesse_anagrafica`)
```
id, name, tariffa (BigDecimal), active (boolean, default true)
```

### RichiestaFatturazione (`richieste_fatturazione`)
```
id, nomeCliente, giornate (BigDecimal), tariffa (BigDecimal),
mese, anno, createdAt
```

### Commessa (embedded in Conversation, non persistita autonomamente)
```
name, giornate (BigDecimal), tariffa (BigDecimal)
Metodo: calculateTotal() → giornate * tariffa
```

---

## Stato conversazione WhatsApp

```
INITIAL → WAITING_MONTH_YEAR → WAITING_GIORNATE → REVIEW_EMAIL → COMPLETED
                                                              ↘ CANCELLED
```

**Flusso dettagliato:**
1. `INITIAL`: qualsiasi messaggio → risponde chiedendo "mese-anno"
2. `WAITING_MONTH_YEAR`: parsing "Gennaio-2026", carica commesse attive da MongoDB
3. `WAITING_GIORNATE`: chiede giornate per ogni commessa in sequenza (itera `currentCommessaIndex`)
4. `REVIEW_EMAIL`: mostra riepilogo totale, chiede "1=Invia / 2=Annulla"
5. `COMPLETED`: `EmailService.sendEmail()` + salva N `RichiestaFatturazione` + risponde via WhatsApp
6. `CANCELLED`: reset conversazione

Reset con parola "start" da qualsiasi stato. Timeout sessione: 30 minuti (cleanup scheduler ogni ora).

---

## API REST

### Pubblici (no autenticazione)
```
POST /api/auth/login          AuthRequest → AuthResponse (token JWT)
POST /api/auth/register       RegisterRequest → AuthResponse
POST /api/whatsapp/webhook    Twilio callback (form-urlencoded: From, Body, MessageSid)
POST /api/whatsapp/test-message
GET  /api/whatsapp/health
```

### Protetti (Bearer JWT)
```
GET    /api/auth/me

GET    /api/commesse
GET    /api/commesse/active
GET    /api/commesse/{id}
POST   /api/commesse               CommessaAnagraficaDto → CommessaAnagrafica
PUT    /api/commesse/{id}
DELETE /api/commesse/{id}
PATCH  /api/commesse/{id}/toggle-active

GET    /api/richieste-fatturazione
GET    /api/richieste-fatturazione/by-periodo?mese=&anno=
GET    /api/richieste-fatturazione/by-cliente?nomeCliente=
DELETE /api/richieste-fatturazione/{id}

POST   /api/chatbot/message        ChatRequest → ChatResponse
DELETE /api/chatbot/conversation/{id}
```

---

## Sicurezza

- **JWT**: HMAC-SHA, secret base64, scadenza 24h. Filtro `JwtAuthenticationFilter` su ogni richiesta.
- **Password**: BCrypt.
- **CORS**: abilitato per `localhost:4200` e `${baseUrl}`.
- **CSRF**: disabilitato.
- **Session**: STATELESS.
- **Ruoli**: campo `role` su `User` (default "USER") — non ancora usato per autorizzazione granulare.

---

## Servizi chiave

### ConversationService
State machine principale. Dipendenze: `TwilioService`, `EmailService`, `CommessaAnagraficaService`, `RichiestaFatturazioneService`, `ConversationRepository`. Ogni transizione di stato persiste la conversazione su MongoDB.

### TwilioService
Invia messaggi WhatsApp. `mockMode=true` in sviluppo (solo log). In produzione chiama SDK Twilio. Numero mittente: `whatsapp:+14155238886`.

### EmailService
Gmail API con OAuth2. Supporta `createDraft()` e `sendEmail()`. Credenziali da file JSON (`credentials.json`) o variabile d'ambiente. Genera HTML per l'email di riepilogo fatturazione.

### CommessaAnagraficaService
Cache Redis su tutti i metodi di lettura (TTL 10 min, keys: "all", "active", id). Cache invalidata su ogni scrittura.

### ChatbotService + ClaudeApiService + MongoQueryService
Loop tool-calling (max 10 iterazioni):
1. Invia messaggi a Claude con tool definitions
2. Se `stop_reason == "tool_use"` → esegue tool su MongoDB via `MongoQueryService`
3. Aggiunge risultato come messaggio user e ripete
4. Esce quando Claude risponde senza tool_use

**Tool disponibili per Claude:**
- `get_commesse_attive()` — lista commesse attive
- `get_richieste_fatturazione(mese?, anno?, nomeCliente?)` — filtro flessibile
- `count_richieste(mese?, anno?)` — conteggio
- `calcola_totale_fatturazione(mese?, anno?)` — totale €, giornate, dettaglio per commessa
- `get_statistiche_commessa(nomeCommessa)` — anagrafica + storico richieste
- `confronta_periodi(mese1, anno1, mese2, anno2)` — diff e variazione %
- `query_libera(collection, filter?, limit?)` — query MongoDB custom (solo su `commesse_anagrafica`, `richieste_fatturazione`, `conversations`)

---

## Scheduler

| Trigger | Metodo | Azione |
|---|---|---|
| Avvio app (`@PostConstruct`) | `resetOnStartup()` | Cancella tutte le conversazioni |
| Ogni ora (`0 0 * * * *`) | `cleanupExpiredSessions()` | Elimina conversazioni con `lastUpdated` > 30 min |
| Ogni giorno 09:00 | `dailyStatistics()` | Log numero sessioni attive |
| Ogni giorno 09:53 (TEST) | `monthlyBillingReminder()` | Invia promemoria WhatsApp all'admin |

> `monthlyBillingReminder` è in modalità test (si attiva ogni giorno). Va completato con il check "ultimo giorno del mese".

---

## Anti-pattern noti

- Non gestire transazioni o logica di business nel Controller
- Non esporre entità MongoDB come response body (usare DTO)
- Non iniettare Repository direttamente nei Controller
- Non chiamare `CommessaAnagraficaRepository` direttamente fuori dal suo Service (bypasserebbe la cache Redis)
- Non resettare manualmente la cache — usare sempre `@CacheEvict` nei service
- Non aggiungere commesse alla conversazione senza passare per `addCommessa()` (tiene aggiornato l'indice)

---

## Regole di dominio

- Una commessa è fatturabile solo se `active = true`
- Le giornate accettano decimali (es. "20,5" o "20.5")
- Il numero di telefono dell'admin autorizzato: `+393476087010` (configurabile via `ADMIN_PHONE`)
- L'email di destinazione fatture: `andrealongo94@hotmail.it` (configurabile via `app.client.email`)
- Ogni ciclo di fatturazione genera N `RichiestaFatturazione` (uno per ogni commessa compilata)
- `SessionCleanupScheduler.resetOnStartup()` cancella tutte le conversazioni ad ogni riavvio — comportamento voluto per sviluppo, da disabilitare in produzione
