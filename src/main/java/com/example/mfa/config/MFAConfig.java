package com.example.mfa.config;

import org.keycloak.models.AuthenticatorConfigModel;

public class MFAConfig {
    // Email configuration
    public static final String SMTP_HOST = "smtpHost";
    public static final String SMTP_PORT = "smtpPort";
    public static final String SMTP_USERNAME = "smtpUsername";
    public static final String SMTP_PASSWORD = "smtpPassword";
    public static final String SMTP_FROM_EMAIL = "smtpFromEmail";

    public static final String TELEGRAM_BOT_TOKEN = "telegramBotToken";

    
    // Twilio configuration
    public static final String TWILIO_ACCOUNT_SID = "twilioAccountSid";
    public static final String TWILIO_AUTH_TOKEN = "twilioAuthToken";
    public static final String TWILIO_VERIFY_SERVICE_SID = "twilioVerifyServiceSid";
    public static final String OTP_EXPIRATION = "otpExpiration";
    public static final String DEFAULT_OTP_EXPIRATION = "300"; // 5 minutes

    
    private final AuthenticatorConfigModel configModel;
    
    public MFAConfig(AuthenticatorConfigModel configModel) {
        this.configModel = configModel;
    }
    
    // Email getters
    public String getSmtpHost() {
        return configModel.getConfig().get(SMTP_HOST);
    }
    
    public String getSmtpPort() {
        return configModel.getConfig().get(SMTP_PORT);
    }
    
    public String getSmtpUsername() {
        return configModel.getConfig().get(SMTP_USERNAME);
    }
    
    public String getSmtpPassword() {
        return configModel.getConfig().get(SMTP_PASSWORD);
    }
    
    public String getSmtpFromEmail() {
        return configModel.getConfig().get(SMTP_FROM_EMAIL);
    }
    
    public String getTwilioAccountSid() {
        return configModel.getConfig().get(TWILIO_ACCOUNT_SID);
    }
    
    public String getTwilioAuthToken() {
        return configModel.getConfig().get(TWILIO_AUTH_TOKEN);
    }
    
    public String getTwilioVerifyServiceSid() {
        return configModel.getConfig().get(TWILIO_VERIFY_SERVICE_SID);
    }

    public String getTelegramBotToken() {
        return configModel.getConfig().get(TELEGRAM_BOT_TOKEN);
    }
    
    public int getOtpExpiration() {
        String expiration = configModel.getConfig().get(OTP_EXPIRATION);
        return expiration != null ? Integer.parseInt(expiration) : Integer.parseInt(DEFAULT_OTP_EXPIRATION);
    }
}