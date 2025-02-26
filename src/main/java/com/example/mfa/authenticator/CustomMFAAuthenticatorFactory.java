package com.example.mfa.authenticator;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import com.example.mfa.event.AuthEventManager;
import com.example.mfa.event.LoggingEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating CustomMFAAuthenticator instances
 * Registers authenticator with Keycloak and provides configuration properties
 */
public class CustomMFAAuthenticatorFactory implements AuthenticatorFactory, ConfigurableAuthenticatorFactory {
    
    public static final String PROVIDER_ID = "custom-mfa-authenticator";
    private static final CustomMFAAuthenticator SINGLETON = new CustomMFAAuthenticator();
    
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();
    
    static {
        // Register the logging event listener
        AuthEventManager.getInstance().addEventListener(new LoggingEventListener());
        
        // Twilio Configuration
        ProviderConfigProperty twilioAccountSid = new ProviderConfigProperty();
        twilioAccountSid.setName("twilioAccountSid");
        twilioAccountSid.setLabel("Twilio Account SID");
        twilioAccountSid.setType(ProviderConfigProperty.STRING_TYPE);
        twilioAccountSid.setHelpText("Your Twilio Account SID");
        configProperties.add(twilioAccountSid);

        ProviderConfigProperty twilioAuthToken = new ProviderConfigProperty();
        twilioAuthToken.setName("twilioAuthToken");
        twilioAuthToken.setLabel("Twilio Auth Token");
        twilioAuthToken.setType(ProviderConfigProperty.PASSWORD);
        twilioAuthToken.setHelpText("Your Twilio Auth Token");
        configProperties.add(twilioAuthToken);

        ProviderConfigProperty twilioVerifyServiceSid = new ProviderConfigProperty();
        twilioVerifyServiceSid.setName("twilioVerifyServiceSid");
        twilioVerifyServiceSid.setLabel("Twilio Verify Service SID");
        twilioVerifyServiceSid.setType(ProviderConfigProperty.STRING_TYPE);
        twilioVerifyServiceSid.setHelpText("Your Twilio Verify Service SID");
        configProperties.add(twilioVerifyServiceSid);

        // Telegram Configuration
        ProviderConfigProperty telegramBotToken = new ProviderConfigProperty();
        telegramBotToken.setName("telegramBotToken");
        telegramBotToken.setLabel("Telegram Bot Token");
        telegramBotToken.setType(ProviderConfigProperty.PASSWORD);
        telegramBotToken.setHelpText("Your Telegram Bot Token");
        configProperties.add(telegramBotToken);

        // Email configuration properties
        ProviderConfigProperty useKeycloakSmtp = new ProviderConfigProperty();
        useKeycloakSmtp.setName("useKeycloakSmtp");
        useKeycloakSmtp.setLabel("Use Keycloak SMTP Settings");
        useKeycloakSmtp.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        useKeycloakSmtp.setDefaultValue("true");
        useKeycloakSmtp.setHelpText("Use Keycloak's default SMTP settings for sending emails");
        configProperties.add(useKeycloakSmtp);

        ProviderConfigProperty emailVerificationRequired = new ProviderConfigProperty();
        emailVerificationRequired.setName("emailVerificationRequired");
        emailVerificationRequired.setLabel("Require Email Verification");
        emailVerificationRequired.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        emailVerificationRequired.setDefaultValue("true");
        emailVerificationRequired.setHelpText("Require email verification before allowing OTP via email");
        configProperties.add(emailVerificationRequired);

        ProviderConfigProperty otpEmailSubject = new ProviderConfigProperty();
        otpEmailSubject.setName("otpEmailSubject");
        otpEmailSubject.setLabel("OTP Email Subject");
        otpEmailSubject.setType(ProviderConfigProperty.STRING_TYPE);
        otpEmailSubject.setDefaultValue("Your authentication code");
        otpEmailSubject.setHelpText("Subject line for OTP emails");
        configProperties.add(otpEmailSubject);
        
        // OTP configuration
        ProviderConfigProperty otpExpiration = new ProviderConfigProperty();
        otpExpiration.setName("otpExpiration");
        otpExpiration.setLabel("OTP Expiration Time");
        otpExpiration.setType(ProviderConfigProperty.STRING_TYPE);
        otpExpiration.setDefaultValue("300"); // 5 minutes in seconds
        otpExpiration.setHelpText("Time in seconds before OTP expires");
        configProperties.add(otpExpiration);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Custom MFA Authentication";
    }

    @Override
    public String getReferenceCategory() {
        return "mfa";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[] {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.ALTERNATIVE,
            AuthenticationExecutionModel.Requirement.DISABLED
        };
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    @Override
    public void init(org.keycloak.Config.Scope config) {
        // Initialize any global settings here
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Perform any post-initialization logic
    }

    @Override
    public void close() {
        // Clean up resources
    }

    @Override
    public String getHelpText() {
        return "Provides MFA via TOTP, SMS, Telegram, or Email";
    }

    @Override
    public boolean isUserSetupAllowed() {
        return true;
    }
}