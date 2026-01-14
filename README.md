# WhatsApp Admin Assistant 📱💼

Chatbot WhatsApp intelligente per la gestione delle richieste di autorizzazione alla fatturazione, sviluppato con Java Spring Boot.

## 🎯 Funzionalità

- ✅ Raccolta dati interattiva tramite WhatsApp
- ✅ Supporto multi-utente con sessioni persistenti
- ✅ Anteprima email prima dell'invio
- ✅ Salvataggio bozze in Gmail
- ✅ Invio email diretto
- ✅ Calcolo automatico dei totali
- ✅ Validazione input in tempo reale

## 🏗️ Architettura

```
┌─────────────┐
│  WhatsApp   │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   Twilio    │  (Webhook)
└──────┬──────┘
       │
       ▼
┌──────────────────────┐
│  Spring Boot App     │
│  ┌────────────────┐  │
│  │ Controller     │  │
│  ├────────────────┤  │
│  │ Service Layer  │  │
│  ├────────────────┤  │
│  │ MongoDB        │  │
│  └────────────────┘  │
└──────────────────────┘
       │
       ▼
┌─────────────┐
│ Gmail API   │
└─────────────┘
```

## 📋 Prerequisiti

- ☕ Java 17+
- 📦 Maven 3.6+
- 🗄️ MongoDB 4.4+
- 🔐 Account Twilio (per WhatsApp)
- 📧 Account Google (per Gmail API)

## 🚀 Setup Completo

### 1️⃣ Setup Twilio

1. Registrati su [Twilio](https://www.twilio.com/try-twilio)
2. Vai su **Console > Messaging > Try it out > Send a WhatsApp message**
3. Segui il wizard per configurare il **Twilio Sandbox for WhatsApp**
4. Salva le seguenti credenziali:
   - `Account SID`
   - `Auth Token`
   - `WhatsApp Number` (es: `whatsapp:+14155238886`)

5. Connetti il tuo numero WhatsApp:
   - Invia un messaggio al numero Twilio con il codice join (es: `join <code>`)

### 2️⃣ Setup Gmail API

1. Vai su [Google Cloud Console](https://console.cloud.google.com)
2. Crea un nuovo progetto o seleziona uno esistente
3. Abilita la **Gmail API**:
   - Menu → APIs & Services → Library
   - Cerca "Gmail API" → Enable
4. Crea credenziali OAuth 2.0:
   - APIs & Services → Credentials
   - Create Credentials → OAuth client ID
   - Application type: Desktop app
   - Scarica il file JSON
5. Rinomina il file scaricato in `credentials.json`
6. Posiziona `credentials.json` in `src/main/resources/`

### 3️⃣ Setup MongoDB

**Opzione A: MongoDB Locale**
```bash
# MacOS
brew tap mongodb/brew
brew install mongodb-community
brew services start mongodb-community

# Ubuntu
sudo apt-get install mongodb
sudo systemctl start mongodb

# Windows
# Scarica e installa da mongodb.com
```

**Opzione B: MongoDB Atlas (Cloud - CONSIGLIATO)**
1. Registrati su [MongoDB Atlas](https://www.mongodb.com/cloud/atlas)
2. Crea un cluster gratuito (M0)
3. Crea un database user
4. Aggiungi il tuo IP alla whitelist
5. Ottieni la connection string

### 4️⃣ Configurazione Applicazione

Modifica `src/main/resources/application.yml`:

```yaml
spring:
  data:
    mongodb:
      # Per MongoDB locale:
      uri: mongodb://localhost:27017/whatsapp_assistant
      # Per MongoDB Atlas:
      # uri: mongodb+srv://username:password@cluster.mongodb.net/whatsapp_assistant

twilio:
  account-sid: YOUR_TWILIO_ACCOUNT_SID
  auth-token: YOUR_TWILIO_AUTH_TOKEN
  whatsapp-number: whatsapp:+14155238886
  webhook-url: https://your-domain.ngrok.io/api/whatsapp/webhook

gmail:
  user-email: your-email@gmail.com
```

### 5️⃣ Installazione Dipendenze

```bash
cd whatsapp-admin-assistant
mvn clean install
```

### 6️⃣ Avvio Applicazione

```bash
mvn spring-boot:run
```

L'applicazione sarà disponibile su `http://localhost:8080`

### 7️⃣ Esposizione Webhook (ngrok)

Twilio ha bisogno di un URL pubblico per inviare i webhook. Usa **ngrok**:

```bash
# Installa ngrok
# MacOS: brew install ngrok
# Windows/Linux: scarica da ngrok.com

# Avvia ngrok
ngrok http 8080
```

Copia l'URL HTTPS generato (es: `https://abc123.ngrok.io`)

### 8️⃣ Configurazione Webhook Twilio

1. Vai su Twilio Console
2. Messaging → Try it out → WhatsApp Sandbox Settings
3. Nel campo "WHEN A MESSAGE COMES IN":
   - Incolla: `https://your-ngrok-url.ngrok.io/api/whatsapp/webhook`
   - Method: POST
4. Salva

## 🧪 Testing

### Test Locale (senza WhatsApp)

```bash
curl -X POST http://localhost:8080/api/whatsapp/test-message \
  -d "phoneNumber=whatsapp:+39123456789" \
  -d "message=start"
```

### Test Completo

1. Apri WhatsApp
2. Invia un messaggio al numero Twilio
3. Scrivi: `start` o `inizia`
4. Segui il flusso conversazionale

## 💬 Flusso Conversazionale

```
User: start
Bot:  👋 Ciao! Sono il tuo assistente...
      📅 Per quale mese devi richiedere l'autorizzazione?

User: Gennaio
Bot:  📆 Perfetto! Ora dimmi l'anno...

User: 2024
Bot:  ✅ Ottimo! Stiamo lavorando su: Gennaio 2024
      💼 Quante commesse hai attive...

User: 2
Bot:  📝 Perfetto! Hai 2 commesse.
      🏢 Qual è il nome della commessa n.1?

User: Progetto Alpha
Bot:  ✅ Commessa: "Progetto Alpha"
      📊 Quante giornate hai lavorato...

User: 10
Bot:  💰 Qual è la tariffa giornaliera...

User: 500
Bot:  ✅ Commessa salvata!
      🏢 Qual è il nome della commessa n.2?

[...continua per tutte le commesse...]

Bot:  ✅ Perfetto! Ecco il riepilogo...
      
      📧 ANTEPRIMA EMAIL
      ━━━━━━━━━━━━━━━━━━━━━
      
      📌 OGGETTO:
      Autorizzazione fatturazione Gennaio 2024
      
      📝 CORPO:
      Ciao,
      con la presente si chiede l'autorizzazione...
      
      🔹 Commessa 1: Progetto Alpha
         • Giornate: 10
         • Tariffa: €500.00
         • Totale: €5.000.00
      
      💰 TOTALE COMPLESSIVO: €5.000.00
      
      Cosa vuoi fare?
      1️⃣ Invia email
      2️⃣ Salva come bozza in Gmail
      3️⃣ Modifica dati
      4️⃣ Annulla

User: 2
Bot:  ✅ Bozza salvata con successo in Gmail!
```

## 📁 Struttura Progetto

```
whatsapp-admin-assistant/
├── src/main/java/com/yourcompany/assistant/
│   ├── WhatsAppAssistantApplication.java    # Main class
│   ├── config/
│   │   ├── TwilioConfig.java                # Configurazione Twilio
│   │   └── GmailConfig.java                 # Configurazione Gmail
│   ├── controller/
│   │   └── WhatsAppController.java          # REST endpoints
│   ├── service/
│   │   ├── ConversationService.java         # Logica conversazione
│   │   ├── TwilioService.java               # Invio messaggi WhatsApp
│   │   └── EmailService.java                # Gestione Gmail
│   ├── model/
│   │   ├── Conversation.java                # Modello conversazione
│   │   ├── Commessa.java                    # Modello commessa
│   │   └── EmailDraft.java                  # Modello bozza email
│   ├── repository/
│   │   └── ConversationRepository.java      # Repository MongoDB
│   └── enums/
│       └── ConversationState.java           # Stati conversazione
├── src/main/resources/
│   ├── application.yml                      # Configurazione app
│   └── credentials.json                     # Credenziali Gmail (da aggiungere)
└── pom.xml                                  # Dipendenze Maven
```

## 🔧 Configurazioni Avanzate

### Email Destinatario

Per configurare l'email del destinatario, modifica il metodo `buildEmailDraft` in `ConversationService.java`:

```java
.recipientEmail("cliente@example.com") // Cambia qui
```

O meglio, aggiungi in `application.yml`:

```yaml
app:
  email:
    recipient: cliente@example.com
```

### Timeout Sessione

Modifica in `application.yml`:

```yaml
app:
  session-timeout-minutes: 30  # Default: 30 minuti
```

### Personalizzazione Messaggi

Tutti i messaggi del bot sono in `ConversationService.java`, facilmente personalizzabili.

## 🐛 Troubleshooting

### Problema: "Twilio authentication failed"
**Soluzione:** Verifica che Account SID e Auth Token siano corretti in `application.yml`

### Problema: "MongoDB connection refused"
**Soluzione:** 
- Verifica che MongoDB sia in esecuzione: `brew services list` (Mac) o `sudo systemctl status mongodb` (Linux)
- Controlla la connection string in `application.yml`

### Problema: "Gmail API credentials not found"
**Soluzione:** Assicurati che `credentials.json` sia in `src/main/resources/`

### Problema: "Webhook not receiving messages"
**Soluzione:**
- Verifica che ngrok sia attivo
- Controlla che l'URL webhook in Twilio sia corretto
- Verifica i log: `tail -f logs/application.log`

### Problema: "Authorization code flow error"
**Soluzione:** Al primo avvio, Gmail API aprirà un browser per l'autorizzazione. Segui le istruzioni.

## 📊 Monitoring

### Health Check

```bash
curl http://localhost:8080/api/whatsapp/health
```

### Log Application

```bash
# Visualizza i log in tempo reale
tail -f logs/application.log
```

## 🔒 Sicurezza

- ✅ Credenziali in variabili d'ambiente
- ✅ OAuth2 per Gmail API
- ✅ Validazione input utente
- ✅ Rate limiting (Twilio)
- ✅ HTTPS per webhook (ngrok)

## 📝 TODO / Miglioramenti Futuri

- [ ] Aggiungere autenticazione utenti
- [ ] Supporto multi-lingua
- [ ] Dashboard web per statistiche
- [ ] Export dati in Excel
- [ ] Notifiche email admin
- [ ] Scheduler per pulizia sessioni scadute
- [ ] Unit tests con JUnit e Mockito
- [ ] Docker containerization
- [ ] CI/CD pipeline

## 🤝 Contributi

Contributi, issues e feature requests sono benvenuti!

## 📄 Licenza

MIT License

## 👨‍💻 Autore

Andrea - Java Developer

## 📞 Supporto

Per domande o supporto, contatta: your-email@example.com

---

**Buon lavoro! 🚀**
