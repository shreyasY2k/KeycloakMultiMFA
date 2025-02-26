package com.example.mfa.service;

import com.twilio.Twilio;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import org.jboss.logging.Logger;
import com.example.mfa.config.MFAConfig;

public class TwilioService {
    private static final Logger logger = Logger.getLogger(TwilioService.class);
    private final String verifyServiceSid;
    private final String accountSid;
    private final String authToken;
    
    public TwilioService(MFAConfig config) {
        this.accountSid = config.getTwilioAccountSid();
        this.authToken = config.getTwilioAuthToken();
        this.verifyServiceSid = config.getTwilioVerifyServiceSid();
        
        if (isConfigured()) {
            Twilio.init(accountSid, authToken);
        }
    }
    
    public boolean isConfigured() {
        return accountSid != null && !accountSid.isEmpty() 
            && authToken != null && !authToken.isEmpty()
            && verifyServiceSid != null && !verifyServiceSid.isEmpty();
    }
    
    public void sendVerificationCode(String phoneNumber) {
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