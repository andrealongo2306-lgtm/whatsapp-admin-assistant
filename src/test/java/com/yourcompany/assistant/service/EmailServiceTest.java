package com.yourcompany.assistant.service;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Draft;
import com.google.api.services.gmail.model.Message;
import com.yourcompany.assistant.config.GmailConfig;
import com.yourcompany.assistant.model.EmailDraft;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test per EmailService")
class EmailServiceTest {

    @Mock
    private GmailConfig gmailConfig;

    @Mock
    private Gmail gmailService;

    @Mock
    private Gmail.Users users;

    @Mock
    private Gmail.Users.Drafts drafts;

    @Mock
    private Gmail.Users.Drafts.Create draftsCreate;

    @Mock
    private Gmail.Users.Messages messages;

    @Mock
    private Gmail.Users.Messages.Send messagesSend;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(gmailConfig);
        ReflectionTestUtils.setField(emailService, "gmailService", gmailService);
    }

    @Nested
    @DisplayName("Test createDraft")
    class CreateDraftTests {

        @Test
        @DisplayName("createDraft crea bozza con successo")
        void createDraft_shouldCreateDraft_successfully() throws Exception {
            EmailDraft emailDraft = EmailDraft.builder()
                    .subject("Test Subject")
                    .body("<p>Test body</p>")
                    .recipientEmail("recipient@example.com")
                    .build();

            Draft mockDraft = new Draft();
            mockDraft.setId("draft-123");

            when(gmailConfig.getUserEmail()).thenReturn("sender@example.com");
            when(gmailService.users()).thenReturn(users);
            when(users.drafts()).thenReturn(drafts);
            when(drafts.create(eq("me"), any(Draft.class))).thenReturn(draftsCreate);
            when(draftsCreate.execute()).thenReturn(mockDraft);

            String draftId = emailService.createDraft(emailDraft);

            assertEquals("draft-123", draftId);
            verify(gmailService.users().drafts()).create(eq("me"), any(Draft.class));
        }

        @Test
        @DisplayName("createDraft lancia eccezione in caso di errore")
        void createDraft_shouldThrowException_onError() throws Exception {
            EmailDraft emailDraft = EmailDraft.builder()
                    .subject("Test Subject")
                    .body("<p>Test body</p>")
                    .recipientEmail("recipient@example.com")
                    .build();

            when(gmailConfig.getUserEmail()).thenReturn("sender@example.com");
            when(gmailService.users()).thenReturn(users);
            when(users.drafts()).thenReturn(drafts);
            when(drafts.create(eq("me"), any(Draft.class))).thenThrow(new RuntimeException("API Error"));

            assertThrows(RuntimeException.class, () -> emailService.createDraft(emailDraft));
        }
    }

    @Nested
    @DisplayName("Test sendEmail")
    class SendEmailTests {

        @Test
        @DisplayName("sendEmail invia email con successo")
        void sendEmail_shouldSendEmail_successfully() throws Exception {
            EmailDraft emailDraft = EmailDraft.builder()
                    .subject("Test Subject")
                    .body("<p>Test body</p>")
                    .recipientEmail("recipient@example.com")
                    .build();

            Message mockMessage = new Message();
            mockMessage.setId("message-456");

            when(gmailConfig.getUserEmail()).thenReturn("sender@example.com");
            when(gmailService.users()).thenReturn(users);
            when(users.messages()).thenReturn(messages);
            when(messages.send(eq("me"), any(Message.class))).thenReturn(messagesSend);
            when(messagesSend.execute()).thenReturn(mockMessage);

            String messageId = emailService.sendEmail(emailDraft);

            assertEquals("message-456", messageId);
            verify(gmailService.users().messages()).send(eq("me"), any(Message.class));
        }

        @Test
        @DisplayName("sendEmail lancia eccezione in caso di errore")
        void sendEmail_shouldThrowException_onError() throws Exception {
            EmailDraft emailDraft = EmailDraft.builder()
                    .subject("Test Subject")
                    .body("<p>Test body</p>")
                    .recipientEmail("recipient@example.com")
                    .build();

            when(gmailConfig.getUserEmail()).thenReturn("sender@example.com");
            when(gmailService.users()).thenReturn(users);
            when(users.messages()).thenReturn(messages);
            when(messages.send(eq("me"), any(Message.class))).thenThrow(new RuntimeException("API Error"));

            assertThrows(RuntimeException.class, () -> emailService.sendEmail(emailDraft));
        }
    }

    @Nested
    @DisplayName("Test EmailDraft model")
    class EmailDraftModelTests {

        @Test
        @DisplayName("EmailDraft builder crea oggetto correttamente")
        void emailDraft_builder_shouldCreateCorrectly() {
            EmailDraft draft = EmailDraft.builder()
                    .subject("Oggetto Test")
                    .body("Corpo Test")
                    .recipientEmail("test@example.com")
                    .build();

            assertEquals("Oggetto Test", draft.getSubject());
            assertEquals("Corpo Test", draft.getBody());
            assertEquals("test@example.com", draft.getRecipientEmail());
        }

        @Test
        @DisplayName("EmailDraft supporta HTML nel body")
        void emailDraft_shouldSupportHtmlBody() {
            String htmlBody = "<html><body><h1>Titolo</h1><p>Paragrafo</p></body></html>";

            EmailDraft draft = EmailDraft.builder()
                    .subject("Test HTML")
                    .body(htmlBody)
                    .recipientEmail("test@example.com")
                    .build();

            assertEquals(htmlBody, draft.getBody());
            assertTrue(draft.getBody().contains("<h1>"));
        }
    }
}
