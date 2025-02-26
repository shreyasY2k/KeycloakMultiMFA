package com.example.mfa.config;

import org.keycloak.models.AuthenticatorConfigModel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for MFA with immutable properties
 */
public class MFAConfig {
    // Email configuration
    public static final String SMTP_HOST = "smtpHost";
    public static final String SMTP_PORT = "smtpPort";
    public static final String SMTP_USERNAME = "smtpUsername";
    public static final String SMTP_PASSWORD = "smtpPassword";
    public static final String SMTP_FROM_EMAIL = "smtpFromEmail";
    public static final String USE_KEYCLOAK_SMTP = "useKeycloakSmtp";
    public static final String EMAIL_VERIFICATION_REQUIRED = "emailVerificationRequired";
    public static final String OTP_EMAIL_SUBJECT = "otpEmailSubject";

    // Telegram configuration
    public static final String TELEGRAM_BOT_TOKEN = "telegramBotToken";
    
    // Twilio configuration
    public static final String TWILIO_ACCOUNT_SID = "twilioAccountSid";
    public static final String TWILIO_AUTH_TOKEN = "twilioAuthToken";
    public static final String TWILIO_VERIFY_SERVICE_SID = "twilioVerifyServiceSid";
    
    // General configuration
    public static final String OTP_EXPIRATION = "otpExpiration";
    public static final String DEFAULT_OTP_EXPIRATION = "300"; // 5 minutes
    
    private final Map<String, String> config;
    
    /**
     * Create a config from an authenticator config model
     */
    public MFAConfig(AuthenticatorConfigModel configModel) {
        Map<String, String> configMap = new HashMap<>();
        
        if (configModel != null && configModel.getConfig() != null) {
            configMap.putAll(configModel.getConfig());
        }
        
        this.config = Collections.unmodifiableMap(configMap);
    }
    
    /**
     * Create a config from a map
     */
    public MFAConfig(Map<String, String> config) {
        Map<String, String> configMap = new HashMap<>();
        
        if (config != null) {
            configMap.putAll(config);
        }
        
        this.config = Collections.unmodifiableMap(configMap);
    }
    
    /**
     * Get a configuration value
     */
    public String getConfig(String key) {
        return config.get(key);
    }
    
    /**
     * Get a configuration value with a default
     */
    public String getConfig(String key, String defaultValue) {
        return config.getOrDefault(key, defaultValue);
    }
    
    /**
     * Get the full configuration map
     */
    public Map<String, String> getAllConfig() {
        return config;
    }
    
    // Email getters
    public String getSmtpHost() {
        return getConfig(SMTP_HOST);
    }
    
    public String getSmtpPort() {
        return getConfig(SMTP_PORT);
    }
    
    public String getSmtpUsername() {
        return getConfig(SMTP_USERNAME);
    }
    
    public String getSmtpPassword() {
        return getConfig(SMTP_PASSWORD);
    }
    
    public String getSmtpFromEmail() {
        return getConfig(SMTP_FROM_EMAIL);
    }
    
    public boolean useKeycloakSmtp() {
        return Boolean.parseBoolean(getConfig(USE_KEYCLOAK_SMTP, "true"));
    }
    
    public boolean emailVerificationRequired() {
        return Boolean.parseBoolean(getConfig(EMAIL_VERIFICATION_REQUIRED, "true"));
    }
    
    public String getOtpEmailSubject() {
        return getConfig(OTP_EMAIL_SUBJECT, "Your authentication code");
    }
    
    // Twilio getters
    public String getTwilioAccountSid() {
        return getConfig(TWILIO_ACCOUNT_SID);
    }
    
    public String getTwilioAuthToken() {
        return getConfig(TWILIO_AUTH_TOKEN);
    }
    
    public String getTwilioVerifyServiceSid() {
        return getConfig(TWILIO_VERIFY_SERVICE_SID);
    }
    
    // Telegram getters
    public String getTelegramBotToken() {
        return getConfig(TELEGRAM_BOT_TOKEN);
    }
    
    // General getters
    public int getOtpExpiration() {
        String expiration = getConfig(OTP_EXPIRATION);
        return expiration != null ? Integer.parseInt(expiration) : Integer.parseInt(DEFAULT_OTP_EXPIRATION);
    }
    
    /**
     * Builder for creating custom configs for testing
     */
    public static class Builder {
        private final Map<String, String> config = new HashMap<>();
        
        public Builder setConfig(String key, String value) {
            config.put(key, value);
            return this;
        }
        
        public Builder setSmtpHost(String host) {
            return setConfig(SMTP_HOST, host);
        }
        
        public Builder setSmtpPort(String port) {
            return setConfig(SMTP_PORT, port);
        }
        
        public Builder setSmtpUsername(String username) {
            return setConfig(SMTP_USERNAME, username);
        }
        
        public Builder setSmtpPassword(String password) {
            return setConfig(SMTP_PASSWORD, password);
        }
        
        public Builder setSmtpFromEmail(String email) {
            return setConfig(SMTP_FROM_EMAIL, email);
        }
        
        public Builder setTwilioAccountSid(String accountSid) {
            return setConfig(TWILIO_ACCOUNT_SID, accountSid);
        }
        
        public Builder setTwilioAuthToken(String authToken) {
            return setConfig(TWILIO_AUTH_TOKEN, authToken);
        }
        
        public Builder setTwilioVerifyServiceSid(String serviceSid) {
            return setConfig(TWILIO_VERIFY_SERVICE_SID, serviceSid);
        }
        
        public Builder setTelegramBotToken(String botToken) {
            return setConfig(TELEGRAM_BOT_TOKEN, botToken);
        }
        
        public Builder setOtpExpiration(int seconds) {
            return setConfig(OTP_EXPIRATION, String.valueOf(seconds));
        }
        
        public MFAConfig build() {
            return new MFAConfig(config);
        }
    }
}