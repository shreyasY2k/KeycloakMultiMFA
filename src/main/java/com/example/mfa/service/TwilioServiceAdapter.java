package com.example.mfa.service;

import org.jboss.logging.Logger;
import com.example.mfa.config.MFAConfig;
import com.twilio.Twilio;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;

/**
 * Adapter Pattern: Implementation for Twilio service
 * Singleton Pattern: Only one instance per config
 */
public class TwilioServiceAdapter implements ExternalServiceAdapter {
    private static final Logger logger = Logger.getLogger(TwilioServiceAdapter.class);
    
    private final String verifyServiceSid;
    private final String accountSid;
    private final String authToken;
    private static TwilioServiceAdapter instance;
    
    private TwilioServiceAdapter(MFAConfig config) {
        this.accountSid = config.getTwilioAccountSid();
        this.authToken = config.getTwilioAuthToken();
        this.verifyServiceSid = config.getTwilioVerifyServiceSid();
        
        if (isConfigured()) {
            Twilio.init(accountSid, authToken);
        }
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized TwilioServiceAdapter getInstance(MFAConfig config) {
        if (instance == null) {
            instance = new TwilioServiceAdapter(config);
        }
        return instance;
    }
    
    @Override
    public boolean isConfigured() {
        return accountSid != null && !accountSid.isEmpty() 
            && authToken != null && !authToken.isEmpty()
            && verifyServiceSid != null && !verifyServiceSid.isEmpty();
    }
    
    @Override
    public void sendVerificationCode(String phoneNumber, String code) throws Exception {
        if (!isConfigured()) {
            logger.info("Development Mode - Would send verification to: " + phoneNumber);
            return;
        }
        
        try {
            Verification verification = Verification.creator(
                verifyServiceSid,
                phoneNumber,
                "sms"
            ).create();
            
            logger.info("Sent verification to " + phoneNumber + ": " + verification.getStatus());
        } catch (Exception e) {
            logger.error("Error sending verification", e);
            throw new RuntimeException("Failed to send verification code", e);
        }
    }
    
    @Override
    public boolean verifyCode(String phoneNumber, String code) {
        if (!isConfigured()) {
            logger.info("Development Mode - Would verify code: " + code + " for " + phoneNumber);
            return true; // Always succeed in development mode
        }
        
        try {
            VerificationCheck verificationCheck = VerificationCheck.creator(
                verifyServiceSid
            ).setTo(phoneNumber)
             .setCode(code)
             .create();
            
            return "approved".equals(verificationCheck.getStatus());
        } catch (Exception e) {
            logger.error("Error checking verification", e);
            return false;
        }
    }
}