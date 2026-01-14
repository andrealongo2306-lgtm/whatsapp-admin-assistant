# 🧪 Guida al Testing

Questa guida contiene tutti i metodi per testare il WhatsApp Admin Assistant.

---

## 1. 🏥 Health Check

Verifica che l'applicazione sia attiva:

```bash
curl http://localhost:8080/api/whatsapp/health
```

**Risposta attesa:**
```json
{
  "status": "UP",
  "service": "WhatsApp Admin Assistant"
}
```

---

## 2. 🧪 Test Locale (senza WhatsApp)

Testa il flusso conversazionale senza usare WhatsApp:

### Test Iniziale

```bash
curl -X POST http://localhost:8080/api/whatsapp/test-message \
  -d "phoneNumber=whatsapp:+39123456789" \
  -d "message=start"
```

### Test Completo Automatizzato

```bash
#!/bin/bash
# test-flow.sh

PHONE="whatsapp:+39123456789"
BASE_URL="http://localhost:8080/api/whatsapp/test-message"

echo "Test 1: Avvio conversazione"
curl -X POST $BASE_URL -d "phoneNumber=$PHONE" -d "message=start"
sleep 2

echo "\nTest 2: Inserimento mese"
curl -X POST $BASE_URL -d "phoneNumber=$PHONE" -d "message=Gennaio"
sleep 2

echo "\nTest 3: Inserimento anno"
curl -X POST $BASE_URL -d "phoneNumber=$PHONE" -d "message=2024"
sleep 2

echo "\nTest 4: Numero commesse"
curl -X POST $BASE_URL -d "phoneNumber=$PHONE" -d "message=1"
sleep 2

echo "\nTest 5: Nome commessa"
curl -X POST $BASE_URL -d "phoneNumber=$PHONE" -d "message=Progetto Test"
sleep 2

echo "\nTest 6: Giornate"
curl -X POST $BASE_URL -d "phoneNumber=$PHONE" -d "message=10"
sleep 2

echo "\nTest 7: Tariffa"
curl -X POST $BASE_URL -d "phoneNumber=$PHONE" -d "message=500"
sleep 2

echo "\nTest 8: Salva bozza"
curl -X POST $BASE_URL -d "phoneNumber=$PHONE" -d "message=2"
```

Rendi eseguibile:
```bash
chmod +x test-flow.sh
./test-flow.sh
```

---

## 3. 📱 Test WhatsApp Reale

### Test Base

1. Apri WhatsApp
2. Vai alla chat con il numero Twilio
3. Invia: `start`

### Test Validazione Input

**Test mese invalido:**
```
You: start
Bot: ...Per quale mese?
You: Dicembree   # typo intenzionale
Bot: ❌ Mese non valido...
You: Dicembre
Bot: ✅ Perfetto!
```

**Test anno invalido:**
```
You: 1999
Bot: ❌ Anno non valido. Inserisci un anno tra 2020 e 2030
You: 2024
Bot: ✅ Ottimo!
```

**Test numero commesse invalido:**
```
You: 0
Bot: ❌ Numero non valido. Inserisci un numero tra 1 e 20
You: 2
Bot: ✅ Perfetto!
```

**Test tariffa invalida:**
```
You: -100
Bot: ❌ La tariffa deve essere maggiore di zero
You: 500
Bot: ✅ Commessa salvata!
```

### Test Modifica Dati

```
You: start
[... completa il flusso fino all'anteprima ...]
Bot: Cosa vuoi fare?
     1️⃣ Invia email
     2️⃣ Salva come bozza
     3️⃣ Modifica dati
     4️⃣ Annulla

You: 3
Bot: 🔄 Ok, ricominciamo dall'inizio...
```

### Test Annullamento

```
You: start
[... completa il flusso fino all'anteprima ...]
You: 4
Bot: ❌ Operazione annullata...
You: start
Bot: 👋 Ciao! Sono il tuo assistente...
```

---

## 4. 🔄 Test Multi-Utente

Verifica che utenti diversi abbiano sessioni indipendenti.

**Terminale 1 (Utente A):**
```bash
curl -X POST http://localhost:8080/api/whatsapp/test-message \
  -d "phoneNumber=whatsapp:+39111111111" \
  -d "message=start"

curl -X POST http://localhost:8080/api/whatsapp/test-message \
  -d "phoneNumber=whatsapp:+39111111111" \
  -d "message=Gennaio"
```

**Terminale 2 (Utente B):**
```bash
curl -X POST http://localhost:8080/api/whatsapp/test-message \
  -d "phoneNumber=whatsapp:+39222222222" \
  -d "message=start"

curl -X POST http://localhost:8080/api/whatsapp/test-message \
  -d "phoneNumber=whatsapp:+39222222222" \
  -d "message=Febbraio"
```

Verifica che ogni utente riceva risposte basate sul proprio stato.

---

## 5. 🗄️ Test MongoDB

### Verifica Dati Salvati

```bash
# Connettiti a MongoDB
mongosh

# Usa il database
use whatsapp_assistant

# Visualizza tutte le conversazioni
db.conversations.find().pretty()

# Cerca conversazione specifica
db.conversations.findOne({phoneNumber: "whatsapp:+39123456789"})

# Conta conversazioni attive
db.conversations.countDocuments()

# Trova conversazioni per stato
db.conversations.find({currentState: "REVIEW_EMAIL"}).pretty()
```

### Pulizia Manuale

```bash
# Elimina una conversazione
db.conversations.deleteOne({phoneNumber: "whatsapp:+39123456789"})

# Elimina tutte le conversazioni
db.conversations.deleteMany({})

# Reset completo
db.conversations.drop()
```

---

## 6. 📧 Test Gmail API

### Test Creazione Bozza

```bash
# Completa un flusso e scegli opzione 2 (salva bozza)
# Poi verifica su Gmail:

# 1. Apri Gmail
# 2. Vai su "Bozze"
# 3. Dovresti vedere l'email generata
```

### Test Invio Email

```bash
# Completa un flusso e scegli opzione 1 (invia email)
# Poi verifica:

# 1. Apri Gmail
# 2. Vai su "Inviati"
# 3. Dovresti vedere l'email inviata
```

---

## 7. 📊 Test Performance

### Test Carico Simultaneo

```bash
# test-concurrent.sh
#!/bin/bash

for i in {1..10}; do
  curl -X POST http://localhost:8080/api/whatsapp/test-message \
    -d "phoneNumber=whatsapp:+3912345678$i" \
    -d "message=start" &
done

wait
echo "Test completato!"
```

### Monitoraggio Risorse

```bash
# Memoria heap
jcmd $(pgrep -f "WhatsAppAssistant") VM.native_memory summary

# CPU e Memoria
top -pid $(pgrep -f "WhatsAppAssistant")
```

---

## 8. 🔐 Test Sicurezza

### Test Injection

**SQL Injection (non applicabile con MongoDB, ma test comunque):**
```bash
curl -X POST http://localhost:8080/api/whatsapp/test-message \
  -d "phoneNumber=whatsapp:+39123456789" \
  -d "message='; DROP TABLE conversations; --"
```

Verifica che il messaggio venga gestito come testo normale.

### Test Input Malformato

```bash
# HTML/Script injection
curl -X POST http://localhost:8080/api/whatsapp/test-message \
  -d "phoneNumber=whatsapp:+39123456789" \
  -d "message=<script>alert('xss')</script>"

# Caratteri speciali
curl -X POST http://localhost:8080/api/whatsapp/test-message \
  -d "phoneNumber=whatsapp:+39123456789" \
  -d "message=\\n\\t\\r"
```

---

## 9. 🕐 Test Timeout Sessioni

### Test Manuale

1. Avvia conversazione
2. Aspetta 31 minuti (timeout + 1)
3. Lo scheduler dovrebbe pulire la sessione
4. Verifica con:
```bash
mongosh
use whatsapp_assistant
db.conversations.find({phoneNumber: "whatsapp:+39123456789"})
```

### Forzare Pulizia

```bash
# Modifica application.yml
app:
  session-timeout-minutes: 1  # 1 minuto per test

# Riavvia app
# Avvia conversazione
# Aspetta 2 minuti
# Verifica pulizia
```

---

## 10. 📝 Test Log

### Visualizza Log in Tempo Reale

```bash
tail -f logs/application.log
```

### Cerca Errori

```bash
grep "ERROR" logs/application.log
grep "Exception" logs/application.log
```

### Analizza Traffico

```bash
# Conta richieste per utente
grep "Processando messaggio" logs/application.log | \
  awk '{print $8}' | sort | uniq -c | sort -rn
```

---

## 11. 🐳 Test Docker

### Build e Test Locale

```bash
# Build immagine
docker build -t whatsapp-assistant:latest .

# Run container
docker run -p 8080:8080 \
  -e TWILIO_ACCOUNT_SID=your_sid \
  -e TWILIO_AUTH_TOKEN=your_token \
  whatsapp-assistant:latest

# Health check
curl http://localhost:8080/api/whatsapp/health
```

### Test Docker Compose

```bash
# Avvia tutti i servizi
docker-compose up -d

# Verifica stato
docker-compose ps

# Log applicazione
docker-compose logs -f app

# Log MongoDB
docker-compose logs -f mongodb

# Ferma tutto
docker-compose down
```

---

## 12. ✅ Checklist Test Pre-Produzione

Prima di andare in produzione, verifica:

- [ ] Health check risponde correttamente
- [ ] MongoDB connessione funziona
- [ ] Twilio webhook riceve messaggi
- [ ] Gmail API crea bozze correttamente
- [ ] Validazione input funziona per tutti i campi
- [ ] Multi-utente gestisce sessioni separate
- [ ] Timeout sessioni pulisce dati vecchi
- [ ] Log non contengono dati sensibili
- [ ] Variabili d'ambiente configurate
- [ ] HTTPS abilitato (ngrok o certificato SSL)
- [ ] Backup MongoDB configurato
- [ ] Monitoraggio errori attivo
- [ ] Rate limiting configurato

---

## 🐛 Debugging

### Problema: Bot non risponde

1. Verifica log: `tail -f logs/application.log`
2. Controlla webhook Twilio: Console → Debugger
3. Verifica ngrok: `curl https://your-ngrok-url.ngrok.io/api/whatsapp/health`

### Problema: MongoDB connection error

```bash
# Verifica MongoDB
mongosh --eval "db.adminCommand('ping')"

# Restart MongoDB
brew services restart mongodb-community  # MacOS
sudo systemctl restart mongod  # Linux
```

### Problema: Gmail API error

1. Verifica `credentials.json` presente
2. Elimina `tokens/` e riautorizza
3. Verifica quota API su Google Cloud Console

---

## 📈 Metriche da Monitorare

- Numero richieste al minuto
- Tempo medio risposta
- Tasso di completamento conversazioni
- Errori Gmail API
- Timeout MongoDB
- Utilizzo memoria heap
- Dimensione database

---

**Buon Testing! 🎯**
