package com.example.mfa.authenticator;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.OTPCredentialProvider;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.jboss.logging.Logger;
import com.example.mfa.config.MFAConfig;
import com.example.mfa.service.TwilioService;
import com.example.mfa.service.TelegramService;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.commons.collections4.MultiValuedMap;
import java.security.SecureRandom;
import org.keycloak.credential.CredentialInput;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class CustomMFAAuthenticator implements Authenticator {
    private static final Logger logger = Logger.getLogger(CustomMFAAuthenticator.class);
    private static final String TEMPLATE_SELECT = "mfa-select.ftl";
    private static final String TEMPLATE_CONFIG = "mfa-config.ftl";
    private static final String TEMPLATE_CODE = "mfa-code.ftl";

    @Override
public void authenticate(AuthenticationFlowContext context) {
    UserModel user = context.getUser();
    if (user == null) {
        context.failure(AuthenticationFlowError.UNKNOWN_USER);
        return;
    }

    String state = context.getAuthenticationSession().getAuthNote("auth_state");
    if (state == null) {
        showMethodSelection(context, user);
        return;
    }

    switch (state) {
        case "METHOD_CONFIG":
            showMethodConfiguration(context, user);
            break;
        case "CODE_VALIDATION":
            String method = context.getAuthenticationSession().getAuthNote("chosen_method");
            if ("totp".equals(method)) {
                // Check TOTP configuration before proceeding
                OTPCredentialProvider otpProvider = (OTPCredentialProvider) context.getSession()
                    .getProvider(CredentialProvider.class, "keycloak-otp");
                
                if (!otpProvider.isConfiguredFor(context.getRealm(), user)) {
                    logger.warn("User attempted TOTP but it's not configured: " + user.getUsername());
                    context.form().setError("totpNotConfigured");
                    showMethodSelection(context, user);
                    return;
                }
            }
            sendOTPCode(context, user);
            break;
        default:
            showMethodSelection(context, user);
            break;
    }
}

    private void showMethodSelection(AuthenticationFlowContext context, UserModel user) {
    context.getAuthenticationSession().setAuthNote("auth_state", "METHOD_SELECT");
    
    // Get OTP credential provider from session
    OTPCredentialProvider otpProvider = (OTPCredentialProvider) context.getSession()
        .getProvider(CredentialProvider.class, "keycloak-otp");
    
    // Check if TOTP is configured using the provider
    boolean isTotpConfigured = otpProvider.isConfiguredFor(context.getRealm(), user);
    
    context.form()
        .setAttribute("sms_configured", user.getFirstAttribute("phoneNumber") != null)
        .setAttribute("telegram_configured", user.getFirstAttribute("telegramId") != null)
        .setAttribute("email_configured", user.getEmail() != null && !user.getEmail().isEmpty())
        .setAttribute("totp_configured", isTotpConfigured);
    
    context.challenge(context.form().createForm(TEMPLATE_SELECT));
}

    private void showMethodConfiguration(AuthenticationFlowContext context, UserModel user) {
        String method = context.getAuthenticationSession().getAuthNote("chosen_method");
        if (method == null) {
            showMethodSelection(context, user);
            return;
        }

        context.form().setAttribute("method", method);
        context.challenge(context.form().createForm(TEMPLATE_CONFIG));
    }

    // Replace the email sending code in sendOTPCode method (around line 147)
private void sendOTPCode(AuthenticationFlowContext context, UserModel user) {
    String method = context.getAuthenticationSession().getAuthNote("chosen_method");
    
    if ("totp".equals(method)) {
        context.challenge(context.form().createForm(TEMPLATE_CODE));
        logger.info("TOTP validation prepared for user: " + user.getUsername());
        return;
    }

    String otp = generateOTP();
    context.getAuthenticationSession().setAuthNote("otp_code", otp);

    try {
        switch (method) {
            case "sms":
                MFAConfig mfaConfig = new MFAConfig(context.getAuthenticatorConfig());
                TwilioService twilioService = new TwilioService(mfaConfig);
                String phoneNumber = user.getFirstAttribute("phoneNumber");
                twilioService.sendVerificationCode(phoneNumber);
                break;
            case "telegram":
                MFAConfig telegramConfig = new MFAConfig(context.getAuthenticatorConfig());
                TelegramService telegramService = new TelegramService(telegramConfig);
                String telegramId = user.getFirstAttribute("telegramId");
                telegramService.sendOTP(telegramId, otp);
                break;
            case "email":
                String email = user.getEmail();
                if (email == null || email.isEmpty()) {
                    throw new EmailException("Email not configured");
                }
                
                // Create email content
                Map<String, Object> attributes = new HashMap<>();
                attributes.put("code", otp);
                attributes.put("realmName", context.getRealm().getName());
                attributes.put("username", user.getUsername());
                
                List<Object> subjectParams = List.of(context.getAuthenticatorConfig().getConfig().getOrDefault("otpEmailSubject", "Your authentication code"));
                
                // Get email template provider from session
                EmailTemplateProvider emailProvider = context.getSession().getProvider(EmailTemplateProvider.class);
                if (emailProvider == null) {
                    logger.error("Email template provider not found");
                    throw new EmailException("Email provider not available");
                }
                
                // Use the correct send method signature
                emailProvider.setRealm(context.getRealm())
                             .setUser(user)
                             .send("Authentication Code", subjectParams, "mfa-otp.ftl", attributes);
                break;
        }
        context.challenge(context.form().createForm(TEMPLATE_CODE));
    } catch (Exception e) {
        logger.error("Failed to send verification code", e);
        context.form().setError("sendError", "Failed to send verification code");
        showMethodSelection(context, user);
    }
}

    @Override
    public void action(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        String state = context.getAuthenticationSession().getAuthNote("auth_state");
        
        if (state == null || user == null) {
            context.failure(AuthenticationFlowError.INTERNAL_ERROR);
            return;
        }

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        switch (state) {
            case "METHOD_SELECT":
                handleMethodSelection(context, user, formData);
                break;
            case "METHOD_CONFIG":
                handleMethodConfiguration(context, user, formData);
                break;
            case "CODE_VALIDATION":
                handleCodeValidation(context, user, formData);
                break;
            default:
                context.failure(AuthenticationFlowError.INTERNAL_ERROR);
        }
    }

    private void handleMethodSelection(AuthenticationFlowContext context, UserModel user, 
                                     MultivaluedMap<String, String> formData) {
        String method = formData.getFirst("mfa-method");
        if (method == null) {
            showMethodSelection(context, user);
            return;
        }

        if ("totp".equals(method)) {
            boolean isTotpConfigured = user.credentialManager().isConfiguredFor(OTPCredentialModel.TYPE);
            if (!isTotpConfigured) {
                user.addRequiredAction(UserModel.RequiredAction.CONFIGURE_TOTP);
                context.success();
                return;
            }
        }

        context.getAuthenticationSession().setAuthNote("chosen_method", method);
        context.form().setAttribute("method", method);
        boolean isConfigured = false;

        switch (method) {
            case "sms":
                isConfigured = user.getFirstAttribute("phoneNumber") != null;
                break;
            case "telegram":
                isConfigured = user.getFirstAttribute("telegramId") != null;
                break;
            case "email":
                isConfigured = user.getEmail() != null && !user.getEmail().isEmpty();
                break;
            case "totp":
                isConfigured = true;
                break;
        }

        if (isConfigured) {
            context.getAuthenticationSession().setAuthNote("auth_state", "CODE_VALIDATION");
            sendOTPCode(context, user);
        } else {
            context.getAuthenticationSession().setAuthNote("auth_state", "METHOD_CONFIG");
            showMethodConfiguration(context, user);
        }
    }

    private void handleMethodConfiguration(AuthenticationFlowContext context, UserModel user, 
                                         MultivaluedMap<String, String> formData) {
        String method = context.getAuthenticationSession().getAuthNote("chosen_method");
        boolean configSuccess = false;

        switch (method) {
            case "sms":
                String phoneNumber = formData.getFirst("phoneNumber");
                if (isValidPhoneNumber(phoneNumber)) {
                    user.setSingleAttribute("phoneNumber", phoneNumber);
                    configSuccess = true;
                }
                break;
            case "telegram":
                String telegramId = formData.getFirst("telegramId");
                if (isValidTelegramId(telegramId)) {
                    user.setSingleAttribute("telegramId", telegramId);
                    configSuccess = true;
                }
                break;
            case "email":
                String email = formData.getFirst("email");
                if (isValidEmail(email)) {
                    user.setEmail(email);
                    configSuccess = true;
                }
                break;
        }

        if (configSuccess) {
            context.getAuthenticationSession().setAuthNote("auth_state", "CODE_VALIDATION");
            sendOTPCode(context, user);
        } else {
            context.form().setError("configurationError");
            showMethodConfiguration(context, user);
        }
    }

    private boolean validateTOTP(AuthenticationFlowContext context, UserModel user, String enteredCode) {
    logger.info("Starting TOTP validation for user: " + user.getUsername());
    logger.info("Entered code: " + (enteredCode != null ? "Length=" + enteredCode.length() : "null"));
    
    try {
        // Get OTP credential provider from session like ValidateOTP does
        OTPCredentialProvider otpCredProvider = (OTPCredentialProvider) context.getSession()
            .getProvider(CredentialProvider.class, "keycloak-otp");
        
        // Check if user has OTP configured
        if (!otpCredProvider.isConfiguredFor(context.getRealm(), user)) {
            logger.warn("User does not have OTP configured: " + user.getUsername());
            return false;
        }
        
        // Get the default credential ID for this user
        String credentialId = otpCredProvider
            .getDefaultCredential(context.getSession(), context.getRealm(), user)
            .getId();
        
        // Validate the OTP using the credential provider directly
        boolean valid = otpCredProvider.isValid(
            context.getRealm(), 
            user, 
            new UserCredentialModel(credentialId, OTPCredentialModel.TYPE, enteredCode)
        );
        
        logger.info("TOTP validation result for " + user.getUsername() + ": " + valid);
        return valid;
    } catch (Exception e) {
        logger.error("TOTP validation error for " + user.getUsername(), e);
        e.printStackTrace();
        return false;
    }
}

    private void handleCodeValidation(AuthenticationFlowContext context, UserModel user, 
                                MultivaluedMap<String, String> formData) {
    String enteredCode = formData.getFirst("code");
    if (enteredCode == null || enteredCode.trim().isEmpty()) {
        context.form().setError("invalidCode");
        context.challenge(context.form().createForm(TEMPLATE_CODE));
        return;
    }

    String method = context.getAuthenticationSession().getAuthNote("chosen_method");
    boolean isValid = false;

    if ("totp".equals(method)) {
        logger.info("TOTP validation requested for user: " + user.getUsername());
        
        // Get OTP credential provider from session
        OTPCredentialProvider otpProvider = (OTPCredentialProvider) context.getSession()
            .getProvider(CredentialProvider.class, "keycloak-otp");
        
        // Check if user has OTP configured
        if (!otpProvider.isConfiguredFor(context.getRealm(), user)) {
            logger.warn("User does not have TOTP configured: " + user.getUsername());
            context.form().setError("totpNotConfigured", "TOTP not configured for this user");
            context.challenge(context.form().createForm(TEMPLATE_CODE));
            return;
        }
        
        try {
            // Get default credential ID 
            String credentialId = otpProvider
                .getDefaultCredential(context.getSession(), context.getRealm(), user)
                .getId();
            
            // Validate using the credential provider directly
            isValid = otpProvider.isValid(
                context.getRealm(), 
                user, 
                new UserCredentialModel(credentialId, OTPCredentialModel.TYPE, enteredCode)
            );
            
            logger.info("TOTP validation result for " + user.getUsername() + ": " + isValid);
        } catch (Exception e) {
            logger.error("Error during TOTP validation", e);
            isValid = false;
        }
    } else if ("sms".equals(method)) {
        // Existing SMS validation code
        MFAConfig mfaConfig = new MFAConfig(context.getAuthenticatorConfig());
        TwilioService twilioService = new TwilioService(mfaConfig);
        String phoneNumber = user.getFirstAttribute("phoneNumber");
        isValid = twilioService.verifyCode(phoneNumber, enteredCode);
    } else if ("telegram".equals(method)) {
        String storedCode = context.getAuthenticationSession().getAuthNote("otp_code");
        isValid = storedCode != null && storedCode.equals(enteredCode);
    } else if ("email".equals(method)) {
        String storedCode = context.getAuthenticationSession().getAuthNote("otp_code");
        isValid = storedCode != null && storedCode.equals(enteredCode);
    }

    if (isValid) {
        context.success();
    } else {
        context.form().setError("invalidCode");
        context.challenge(context.form().createForm(TEMPLATE_CODE));
    }
}

private void handleEmailConfiguration(AuthenticationFlowContext context, UserModel user, 
                                    MultivaluedMap<String, String> formData) {
    String email = formData.getFirst("email");
    if (!isValidEmail(email)) {
        context.form().setError("invalidEmail");
        showMethodConfiguration(context, user);
        return;
    }

    // Check if email verification is required
    boolean requireVerification = Boolean.parseBoolean(
        context.getAuthenticatorConfig().getConfig().getOrDefault("emailVerificationRequired", "true")
    );

    if (requireVerification) {
        // Store the email temporarily
        context.getAuthenticationSession().setAuthNote("pending_email", email);
        
        try {
            String verificationCode = generateOTP();
            context.getAuthenticationSession().setAuthNote("email_verification_code", verificationCode);
            
            // Create template attributes
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("code", verificationCode);
            attributes.put("realmName", context.getRealm().getName());
            attributes.put("username", user.getUsername());
            List<Object> subjectParams = List.of(context.getAuthenticatorConfig().getConfig().getOrDefault("otpEmailSubject", "Your authentication code"));
            // Send verification email
            EmailTemplateProvider emailProvider = context.getSession().getProvider(EmailTemplateProvider.class);
            emailProvider.setRealm(context.getRealm())
                        .setUser(user)
                        .setAttribute("code", verificationCode)
                        .send("Email Verification Required", subjectParams, "mfa-verify.ftl" ,attributes);

            // Show verification code input form
            context.getAuthenticationSession().setAuthNote("auth_state", "EMAIL_VERIFICATION");
            context.form()
                .setAttribute("email", email)
                .createForm("mfa-verify.ftl");
        } catch (EmailException e) {
            logger.error("Failed to send verification email", e);
            context.form().setError("emailSendError");
            showMethodConfiguration(context, user);
        }
    } else {
        // If no verification required, set email directly
        user.setEmail(email);
        user.setEmailVerified(false);
        proceedWithOTP(context, user);
    }
}

// Replace the sendEmailOTP method (around line 430)
private void sendEmailOTP(AuthenticationFlowContext context, UserModel user) {
    String otp = generateOTP();
    context.getAuthenticationSession().setAuthNote("otp_code", otp);

    try {
        String email = user.getEmail();
        if (email == null || email.isEmpty()) {
            throw new EmailException("Email not configured");
        }

        List<Object> subjectParams = List.of(context.getAuthenticatorConfig().getConfig().getOrDefault("otpEmailSubject", "Your authentication code"));
        // Create email content
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("code", otp);
        attributes.put("realmName", context.getRealm().getName());
        attributes.put("username", user.getUsername());
        
        // Get email template provider from session
        EmailTemplateProvider emailProvider = context.getSession().getProvider(EmailTemplateProvider.class);
        if (emailProvider == null) {
            logger.error("Email template provider not found");
            throw new EmailException("Email provider not available");
        }
        
        // Use the correct send method signature
        emailProvider.setRealm(context.getRealm())
                    .setUser(user)
                    .setAttribute("code", otp)
                    .send("Authentication Code", subjectParams, "mfa-otp.ftl", attributes);

        context.challenge(context.form().createForm("mfa-code.ftl"));
    } catch (EmailException e) {
        logger.error("Failed to send OTP email", e);
        context.form().setError("emailSendError");
        showMethodSelection(context, user);
    }
}

private void proceedWithOTP(AuthenticationFlowContext context, UserModel user) {
    context.getAuthenticationSession().setAuthNote("auth_state", "CODE_VALIDATION");
    sendEmailOTP(context, user);
}


    private String generateOTP() {
        return String.format("%06d", new SecureRandom().nextInt(1000000));
    }

    private boolean isValidTelegramId(String telegramId) {
        return telegramId != null && telegramId.matches("^-?\\d+$");
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && phoneNumber.matches("\\+[0-9]{10,15}");
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {
    }
}