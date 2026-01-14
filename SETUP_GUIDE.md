# 🚀 Guida Setup Passo-Passo

Questa guida ti accompagnerà nell'installazione completa del WhatsApp Admin Assistant.

## ⏱️ Tempo Stimato: 30-45 minuti

---

## FASE 1: Prerequisiti (5 minuti)

### ✅ Verifica Java

```bash
java -version
```

Deve essere **Java 17 o superiore**. Se non installato:

- **MacOS**: `brew install openjdk@17`
- **Ubuntu**: `sudo apt install openjdk-17-jdk`
- **Windows**: Scarica da [adoptium.net](https://adoptium.net/)

### ✅ Verifica Maven

```bash
mvn -version
```

Se non installato:

- **MacOS**: `brew install maven`
- **Ubuntu**: `sudo apt install maven`
- **Windows**: Scarica da [maven.apache.org](https://maven.apache.org/)

---

## FASE 2: Setup MongoDB (10 minuti)

### Opzione A: MongoDB Locale

#### MacOS
```bash
brew tap mongodb/brew
brew install mongodb-community@7.0
brew services start mongodb-community@7.0

# Verifica
mongosh
> show dbs
> exit
```

#### Ubuntu/Debian
```bash
# Import GPG key
curl -fsSL https://pgp.mongodb.com/server-7.0.asc | \
   sudo gpg -o /usr/share/keyrings/mongodb-server-7.0.gpg --dearmor

# Add repository
echo "deb [ signed-by=/usr/share/keyrings/mongodb-server-7.0.gpg ] https://repo.mongodb.org/apt/ubuntu jammy/mongodb-org/7.0 multiverse" | \
   sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list

# Install
sudo apt-get update
sudo apt-get install -y mongodb-org

# Start
sudo systemctl start mongod
sudo systemctl enable mongod

# Verifica
mongosh
```

#### Windows
1. Scarica da [mongodb.com/try/download/community](https://www.mongodb.com/try/download/community)
2. Installa con wizard
3. Avvia MongoDB come servizio

### Opzione B: MongoDB Atlas (Cloud - CONSIGLIATO) ☁️

1. Vai su [mongodb.com/cloud/atlas/register](https://www.mongodb.com/cloud/atlas/register)
2. Crea account (gratuito)
3. Crea nuovo progetto: "WhatsApp Assistant"
4. **Deploy Database** → M0 FREE
5. Scegli region (es: Frankfurt per EU)
6. Cluster Name: "Cluster0"
7. **Create Cluster**
8. **Database Access**:
   - Add New Database User
   - Username: `admin`
   - Password: genera automatico (salvalo!)
   - Database User Privileges: Read and write to any database
9. **Network Access**:
   - Add IP Address → Allow Access from Anywhere (0.0.0.0/0)
   - ⚠️ In produzione, limita agli IP specifici
10. **Connect**:
    - Drivers → Java
    - Copia la connection string:
    ```
    mongodb+srv://admin:<password>@cluster0.xxxxx.mongodb.net/whatsapp_assistant
    ```

---

## FASE 3: Setup Twilio (10 minuti)

### 1. Crea Account Twilio

1. Vai su [twilio.com/try-twilio](https://www.twilio.com/try-twilio)
2. Registrati (gratuito, $15.50 di credito trial)
3. Verifica email e numero telefono

### 2. Configura WhatsApp Sandbox

1. Console Twilio → **Messaging** → **Try it out** → **Send a WhatsApp message**
2. Troverai:
   - Sandbox Number: es. `+1 415 523 8886`
   - Join Code: es. `join orange-tiger`
3. Sul tuo WhatsApp:
   - Aggiungi il Sandbox Number ai contatti
   - Invia: `join orange-tiger` (usa il tuo codice)
   - Riceverai conferma: "You are all set!"

### 3. Ottieni Credenziali

1. Dashboard Twilio → **Account Info**
2. Salva:
   - **Account SID**: `ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`
   - **Auth Token**: (clicca "show" per visualizzare)

---

## FASE 4: Setup Gmail API (10 minuti)

### 1. Crea Progetto Google Cloud

1. Vai su [console.cloud.google.com](https://console.cloud.google.com/)
2. Crea nuovo progetto: "WhatsApp Admin Assistant"
3. Seleziona il progetto

### 2. Abilita Gmail API

1. Menu ☰ → **APIs & Services** → **Library**
2. Cerca: "Gmail API"
3. Click su "Gmail API"
4. Click **Enable**

### 3. Crea Credenziali OAuth

1. **APIs & Services** → **Credentials**
2. **+ CREATE CREDENTIALS** → **OAuth client ID**
3. Se richiesto, configura OAuth consent screen:
   - User Type: **External**
   - App name: "WhatsApp Admin Assistant"
   - User support email: tua email
   - Developer contact: tua email
   - Click **Save and Continue** (salta tutto il resto)
4. Torna a **Credentials** → **+ CREATE CREDENTIALS** → **OAuth client ID**
5. Application type: **Desktop app**
6. Name: "WhatsApp Assistant Desktop"
7. **CREATE**
8. **DOWNLOAD JSON** → salva come `credentials.json`

---

## FASE 5: Clona e Configura Progetto (5 minuti)

### 1. Scarica Progetto

```bash
cd ~/progetti
# Se hai git:
git clone <url-repository>
# Oppure estrai lo zip

cd whatsapp-admin-assistant
```

### 2. Aggiungi credentials.json

```bash
# Copia il file scaricato da Google
cp ~/Downloads/credentials.json src/main/resources/
```

### 3. Configura application.yml

Apri `src/main/resources/application.yml` e modifica:

```yaml
spring:
  data:
    mongodb:
      # Se usi MongoDB Atlas:
      uri: mongodb+srv://admin:TUA_PASSWORD@cluster0.xxxxx.mongodb.net/whatsapp_assistant
      # Se usi MongoDB locale:
      # uri: mongodb://localhost:27017/whatsapp_assistant

twilio:
  account-sid: ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx  # Tuo Account SID
  auth-token: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx     # Tuo Auth Token
  whatsapp-number: whatsapp:+14155238886           # Numero sandbox Twilio

gmail:
  user-email: tua-email@gmail.com                  # La tua Gmail
```

### 4. Build Progetto

```bash
mvn clean install
```

Se tutto ok, vedrai `BUILD SUCCESS`

---

## FASE 6: Esposizione con ngrok (5 minuti)

### 1. Installa ngrok

- **MacOS**: `brew install ngrok`
- **Windows/Linux**: Scarica da [ngrok.com/download](https://ngrok.com/download)

### 2. Registrati su ngrok

1. Vai su [ngrok.com](https://ngrok.com)
2. Registrati (gratuito)
3. Copia il tuo authtoken
4. Configura:
```bash
ngrok config add-authtoken <your-token>
```

### 3. Avvia ngrok

```bash
ngrok http 8080
```

Vedrai qualcosa come:
```
Forwarding  https://abc123.ngrok.io -> http://localhost:8080
```

**🔴 NON CHIUDERE QUESTO TERMINALE!** Lascialo aperto.

Copia l'URL HTTPS (es: `https://abc123.ngrok.io`)

---

## FASE 7: Configura Webhook Twilio (3 minuti)

1. Torna su Console Twilio
2. **Messaging** → **Try it out** → **WhatsApp Sandbox Settings**
3. Nella sezione **Sandbox Configuration**:
   - "WHEN A MESSAGE COMES IN":
   - Incolla: `https://abc123.ngrok.io/api/whatsapp/webhook`
   - Method: **POST**
4. **Save**

---

## FASE 8: Avvio Applicazione (2 minuti)

In un **nuovo terminale**:

```bash
cd whatsapp-admin-assistant
mvn spring-boot:run
```

Attendi fino a vedere:
```
Started WhatsAppAssistantApplication in X.XX seconds
```

---

## FASE 9: Test! 🎉

### Test 1: Health Check

```bash
curl http://localhost:8080/api/whatsapp/health
```

Risposta attesa:
```json
{"status":"UP","service":"WhatsApp Admin Assistant"}
```

### Test 2: WhatsApp

1. Apri WhatsApp
2. Vai alla chat con il numero Twilio
3. Invia: **start**
4. Dovresti ricevere: "👋 Ciao! Sono il tuo assistente..."

### Test 3: Flusso Completo

Prova il flusso completo:
```
You: start
Bot: 👋 Ciao! ... Per quale mese?

You: Gennaio
Bot: 📆 Perfetto! ... Anno?

You: 2024
Bot: ✅ Ottimo! ... Quante commesse?

You: 1
Bot: 📝 Perfetto! ... Nome commessa?

You: Test Commessa
Bot: ✅ Commessa: "Test Commessa" ... Giornate?

You: 5
Bot: 💰 Tariffa giornaliera?

You: 500
Bot: ✅ [Mostra anteprima email]
     Cosa vuoi fare?
     1️⃣ Invia email
     2️⃣ Salva come bozza
     ...

You: 2
Bot: ✅ Bozza salvata con successo!
```

### Test 4: Verifica Gmail

1. Apri Gmail
2. Vai su **Bozze**
3. Dovresti vedere l'email generata!

---

## 🎊 COMPLETATO!

Il tuo WhatsApp Admin Assistant è ora operativo!

---

## 🐛 Problemi Comuni

### "Port 8080 already in use"

```bash
# Trova processo
lsof -i :8080
# Termina processo
kill -9 <PID>
```

### "MongoDB connection refused"

```bash
# Verifica stato
brew services list  # MacOS
sudo systemctl status mongod  # Linux

# Riavvia se necessario
brew services restart mongodb-community  # MacOS
sudo systemctl restart mongod  # Linux
```

### "Gmail authorization error"

Al primo avvio, si aprirà un browser per autorizzare l'app:
1. Scegli il tuo account Gmail
2. Click "Advanced" (se vede warning)
3. Click "Go to WhatsApp Admin Assistant (unsafe)"
4. Click "Allow"

### "Twilio webhook not working"

1. Verifica che ngrok sia attivo
2. Controlla URL in Twilio (deve essere identico a ngrok)
3. Verifica logs: `tail -f logs/application.log`

---

## 📚 Prossimi Passi

1. ✅ Personalizza i messaggi in `ConversationService.java`
2. ✅ Aggiungi email destinatario in `buildEmailDraft()`
3. ✅ Configura timeout sessione in `application.yml`
4. ✅ Deploy in produzione (Heroku, AWS, Azure...)

---

## 🆘 Supporto

Se hai problemi:
1. Controlla i log: `tail -f logs/application.log`
2. Verifica configurazioni in `application.yml`
3. Controlla che tutti i servizi siano attivi (MongoDB, ngrok)

---

**Buon lavoro! 🚀**
