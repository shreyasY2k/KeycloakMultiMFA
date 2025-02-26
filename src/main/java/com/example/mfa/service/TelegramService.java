package com.example.mfa.service;

import org.jboss.logging.Logger;
import com.example.mfa.config.MFAConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class TelegramService {
    private static final Logger logger = Logger.getLogger(TelegramService.class);
    private final String botToken;
    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot%s/sendMessage";
    private final HttpClient httpClient;
    
    public TelegramService(MFAConfig config) {
        this.botToken = config.getTelegramBotToken();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }
    
    public boolean isConfigured() {
        return botToken != null && !botToken.isEmpty();
    }
    
    public void sendOTP(String chatId, String otp) {
        if (!isConfigured()) {
            logger.info("Development Mode - Telegram OTP for " + chatId + ": " + otp);
            return;
        }
        
        try {
            String message = String.format("Your verification code is: %s", otp);
            String url = String.format(TELEGRAM_API_URL, botToken);
            
            String jsonBody = String.format(
                "{\"chat_id\": \"%s\", \"text\": \"%s\", \"parse_mode\": \"HTML\"}",
                chatId, message
            );
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                logger.error("Failed to send Telegram message. Status: " + response.statusCode());
                throw new RuntimeException("Failed to send Telegram message");
            }
            
            logger.info("Telegram OTP sent successfully to " + chatId);
        } catch (Exception e) {
            logger.error("Error sending Telegram message", e);
            throw new RuntimeException("Failed to send Telegram message", e);
        }
    }
}