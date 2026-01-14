package com.yourcompany.assistant.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Draft;
import com.google.api.services.gmail.model.Message;
import com.yourcompany.assistant.config.GmailConfig;
import com.yourcompany.assistant.model.EmailDraft;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final GmailConfig gmailConfig;
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_COMPOSE);
    
    private Gmail gmailService;
    
    @PostConstruct
    public void init() {
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            gmailService = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                    .setApplicationName(gmailConfig.getApplicationName())
                    .build();
            log.info("Gmail service inizializzato con successo");
        } catch (Exception e) {
            log.error("Errore nell'inizializzazione del Gmail service: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Ottiene le credenziali OAuth2 per Gmail API
     */
    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Carica le credenziali client
        InputStream in = getClass().getClassLoader().getResourceAsStream("credentials.json");
        if (in == null) {
            throw new FileNotFoundException("File credentials.json non trovato");
        }
        
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        
        // Build flow e trigger user authorization request
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(gmailConfig.getTokensDirectory())))
                .setAccessType("offline")
                .build();
        
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }
    
    /**
     * Crea una bozza email in Gmail
     * 
     * @param emailDraft Dati della bozza email
     * @return ID della bozza creata
     */
    public String createDraft(EmailDraft emailDraft) {
        try {
            MimeMessage email = createEmail(
                    emailDraft.getRecipientEmail(),
                    gmailConfig.getUserEmail(),
                    emailDraft.getSubject(),
                    emailDraft.getBody()
            );
            
            Message message = createMessageWithEmail(email);
            Draft draft = new Draft();
            draft.setMessage(message);
            
            draft = gmailService.users().drafts()
                    .create("me", draft)
                    .execute();
            
            log.info("Bozza creata con successo. ID: {}", draft.getId());
            return draft.getId();
            
        } catch (Exception e) {
            log.error("Errore nella creazione della bozza: {}", e.getMessage(), e);
            throw new RuntimeException("Impossibile creare la bozza email", e);
        }
    }
    
    /**
     * Invia un'email (non bozza)
     */
    public String sendEmail(EmailDraft emailDraft) {
        try {
            MimeMessage email = createEmail(
                    emailDraft.getRecipientEmail(),
                    gmailConfig.getUserEmail(),
                    emailDraft.getSubject(),
                    emailDraft.getBody()
            );
            
            Message message = createMessageWithEmail(email);
            message = gmailService.users().messages()
                    .send("me", message)
                    .execute();
            
            log.info("Email inviata con successo. ID: {}", message.getId());
            return message.getId();
            
        } catch (Exception e) {
            log.error("Errore nell'invio dell'email: {}", e.getMessage(), e);
            throw new RuntimeException("Impossibile inviare l'email", e);
        }
    }
    
    /**
     * Crea un oggetto MimeMessage
     */
    private MimeMessage createEmail(String to, String from, String subject, String bodyText) 
            throws Exception {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        
        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(from));
        email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        email.setText(bodyText, "UTF-8", "html");
        
        return email;
    }
    
    /**
     * Converte MimeMessage in Message di Gmail API
     */
    private Message createMessageWithEmail(MimeMessage emailContent) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = java.util.Base64.getUrlEncoder().encodeToString(bytes);
        
        Message message = new Message();
        message.setRaw(encodedEmail);
        
        return message;
    }
}
