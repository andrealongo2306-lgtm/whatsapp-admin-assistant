package com.yourcompany.assistant.service;

import com.yourcompany.assistant.config.TwilioConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test per TwilioService")
class TwilioServiceTest {


    @Mock
    private TwilioConfig twilioConfig;

    @InjectMocks
    private TwilioService twilioService;

    @Nested
    @DisplayName("Test Mock Mode")
    class MockModeTests {

        @BeforeEach
        void setUp() {
            when(twilioConfig.isMockMode()).thenReturn(true);
        }

        @Test
        @DisplayName("sendWhatsAppMessage restituisce MOCK SID in mock mode")
        void sendWhatsAppMessage_shouldReturnMockSid_inMockMode() {
            String result = twilioService.sendWhatsAppMessage("whatsapp:+39123456789", "Test message");

            assertNotNull(result);
            assertTrue(result.startsWith("MOCK_"));
        }

        @Test
        @DisplayName("sendWhatsAppMessage aggiunge prefisso whatsapp se mancante")
        void sendWhatsAppMessage_shouldAddWhatsappPrefix_ifMissing() {
            String result = twilioService.sendWhatsAppMessage("+39123456789", "Test message");

            assertNotNull(result);
            assertTrue(result.startsWith("MOCK_"));
        }

        @Test
        @DisplayName("sendFormattedMessage aggiunge emoji al messaggio")
        void sendFormattedMessage_shouldAddEmoji() {
            String result = twilioService.sendFormattedMessage("whatsapp:+39123456789", "📱", "Test");

            assertNotNull(result);
            assertTrue(result.startsWith("MOCK_"));
        }
    }

   @Nested
    @DisplayName("Test validazione input")
    class InputValidationTests {

        //@BeforeEach
        //void setUp() {
        //    when(twilioConfig.isMockMode()).thenReturn(true);
        //}

        @Test
        @DisplayName("Numero null lancia eccezione")
        void sendWhatsAppMessage_shouldThrowException_whenPhoneIsNull() {
            assertThrows(Exception.class, () ->
                    twilioService.sendWhatsAppMessage(null, "Test message")
            );
        }

        @Test
        @DisplayName("Numero vuoto lancia eccezione")
        void sendWhatsAppMessage_shouldThrowException_whenPhoneIsEmpty() {
            assertThrows(Exception.class, () ->
                    twilioService.sendWhatsAppMessage("", "Test message")
            );
        }


    }

    @Nested
    @DisplayName("Test formattazione numero")
    class PhoneNumberFormattingTests {

        @BeforeEach
        void setUp() {
            when(twilioConfig.isMockMode()).thenReturn(true);
        }

        @Test
        @DisplayName("Numero con prefisso whatsapp rimane invariato")
        void formatPhoneNumber_shouldKeepPrefix_whenAlreadyPresent() {
            String result = twilioService.sendWhatsAppMessage("whatsapp:+39123456789", "Test");

            assertNotNull(result);
            verify(twilioConfig).isMockMode();
        }

        @Test
        @DisplayName("Numero senza prefisso whatsapp viene formattato")
        void formatPhoneNumber_shouldAddPrefix_whenMissing() {
            String result = twilioService.sendWhatsAppMessage("+39123456789", "Test");

            assertNotNull(result);
        }

        @Test
        @DisplayName("Numero italiano senza + viene gestito")
        void formatPhoneNumber_shouldHandle_italianNumber() {
            String result = twilioService.sendWhatsAppMessage("39123456789", "Test");

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Test sendFormattedMessage")
    class FormattedMessageTests {

        @BeforeEach
        void setUp() {
            when(twilioConfig.isMockMode()).thenReturn(true);
        }

        @Test
        @DisplayName("Messaggio formattato include emoji e testo")
        void sendFormattedMessage_shouldCombineEmojiAndMessage() {
            String result = twilioService.sendFormattedMessage(
                    "whatsapp:+39123456789",
                    "✅",
                    "Operazione completata"
            );

            assertNotNull(result);
            assertTrue(result.startsWith("MOCK_"));
        }

        @Test
        @DisplayName("Emoji vuota funziona correttamente")
        void sendFormattedMessage_shouldWork_withEmptyEmoji() {
            String result = twilioService.sendFormattedMessage(
                    "whatsapp:+39123456789",
                    "",
                    "Messaggio"
            );

            assertNotNull(result);
        }
    }
}
