package com.yourcompany.assistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableMongoRepositories(basePackages = "com.yourcompany.assistant.repository")
@EnableScheduling
public class WhatsAppAssistantApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(WhatsAppAssistantApplication.class, args);
    }
}
