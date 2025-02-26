package com.example.mfa.service;

import org.jboss.logging.Logger;
import com.example.mfa.config.MFAConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Adapter Pattern: Implementation for Telegram service
 * Singleton Pattern: Only one instance per config
 */
public class TelegramServiceAdapter implements ExternalServiceAdapter {
    private static final Logger logger = Logger.getLogger(TelegramServiceAdapter.class);
    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot%s/sendMessage";
    
    private final String botToken;
    private final HttpClient httpClient;
    private static TelegramServiceAdapter instance;
    
    private TelegramServiceAdapter(MFAConfig config) {
        this.botToken = config.getTelegramBotToken();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized TelegramServiceAdapter getInstance(MFAConfig config) {
        if (instance == null) {
            instance = new TelegramServiceAdapter(config);
        }
        return instance;
    }
    
    @Override
    public boolean isConfigured() {
        return botToken != null && !botToken.isEmpty();
    }
    
    @Override
    public void sendVerificationCode(String chatId, String code) throws Exception {
        if (!isConfigured()) {
            logger.info("Development Mode - Telegram OTP for " + chatId + ": " + code);
            return;
        }
        
        try {
            String message = String.format("Your verification code is: %s", code);
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
    
    @Override
    public boolean verifyCode(String chatId, String code) {
        // Telegram doesn't have a built-in verification API, so we rely on manually comparing codes
        // This is handled by the AbstractMFAProvider's verifyCode method
        return true;
    }
}