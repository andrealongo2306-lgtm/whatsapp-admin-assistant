package com.yourcompany.assistant.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class GmailConfig {
    
    @Value("${gmail.credentials-file}")
    private String credentialsFile;
    
    @Value("${gmail.tokens-directory}")
    private String tokensDirectory;
    
    @Value("${gmail.application-name}")
    private String applicationName;
    
    @Value("${gmail.user-email}")
    private String userEmail;

    @Value("${refresh-token:}")
    private String refreshToken;
}
