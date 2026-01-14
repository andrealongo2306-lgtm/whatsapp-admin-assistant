package com.yourcompany.assistant.config;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class TwilioConfig {
    
    @Value("${twilio.account-sid}")
    private String accountSid;
    
    @Value("${twilio.auth-token}")
    private String authToken;
    
    @Value("${twilio.whatsapp-number}")
    private String whatsappNumber;
    
    @Value("${twilio.webhook-url}")
    private String webhookUrl;

    @Value("${twilio.mock-mode:false}")
    private boolean mockMode;

    @PostConstruct
    public void init() {
        if (!mockMode) {
            Twilio.init(accountSid, authToken);
        }
    }
}
