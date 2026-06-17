package com.yourcompany.assistant.service;

import com.yourcompany.assistant.constants.BotMessages;
// ... (altri import invariati)

        @Test
        void processMessage_shouldSendWelcomeMessage_forNewUser() {

            verify(twilioService).sendWhatsAppMessage(eq(PHONE_NUMBER), messageCaptor.capture());
            String message = messageCaptor.getValue();
            assertEquals(BotMessages.WELCOME_MESSAGE, message);
        }

    @Nested
    @DisplayName("Test WELCOME_MESSAGE")
    class WelcomeMessageTests {

        @Test
        @DisplayName("La costante WELCOME_MESSAGE è definita e non vuota")
        void welcomeMessage_shouldBeDefined() {
            assertNotNull(BotMessages.WELCOME_MESSAGE);
            assertFalse(BotMessages.WELCOME_MESSAGE.isBlank());
        }

        @Test
        @DisplayName("La costante WELCOME_MESSAGE contiene le istruzioni sul formato")
        void welcomeMessage_shouldContainFormatInstructions() {
            assertTrue(
                BotMessages.WELCOME_MESSAGE.contains("2024") ||
                BotMessages.WELCOME_MESSAGE.contains("es:") ||
                BotMessages.WELCOME_MESSAGE.contains("Esempio"),
                "Il messaggio di benvenuto deve includere un esempio del formato atteso"
            );
        }
    }
}
