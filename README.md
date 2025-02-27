# Keycloak MFA Plugin

This project is a Keycloak authentication plugin that provides multi-factor authentication (MFA) using various methods including SMS, Email, Telegram, and TOTP. The codebase follows modern design patterns, providing an extensible, maintainable, and testable architecture.

## Compatibility

- Supports Keycloak version 26.x and above
- Built with Java 11+
- Tested with PostgreSQL as the database backend

## Integration Guide

### Building the Plugin

1. Clone the repository
2. Build using Maven:
   ```bash
   mvn clean package
   ```
3. The build will produce a JAR file in the `target` directory

### Installation

1. Copy the JAR file to the Keycloak `providers` directory:
   ```bash
   cp target/keycloak-mfa-plugin-1.0-SNAPSHOT.jar /path/to/keycloak/providers/
   ```

2. Restart Keycloak or build a new image if using Docker:
   ```bash
   # For standalone Keycloak
   /path/to/keycloak/bin/kc.sh build
   /path/to/keycloak/bin/kc.sh start-dev
   
   # For Docker-based setup
   docker-compose up -d --build
   ```

### Configuration

1. Log in to the Keycloak Admin Console
2. Go to Authentication → Flows
3. Duplicate the "Browser" flow or create a new flow
4. Add the "Custom MFA Authentication" as an execution step
5. Click the gear icon on the new execution to configure it:
   - Twilio Account SID, Auth Token, and Verify Service SID for SMS
   - Telegram Bot Token for Telegram notifications
   - Email settings (uses Keycloak's email configuration by default)
   - OTP expiration time

6. Set the flow as "Required" or "Alternative" based on your needs
7. Bind the new flow to your realm's browser flow

### Docker Deployment

A Docker Compose file is included for easy deployment:

```bash
# Start the environment
docker-compose up -d

# View logs
docker-compose logs -f

# Stop the environment
docker-compose down
```

The Docker setup includes:
- Keycloak server with the MFA plugin pre-installed
- PostgreSQL database
- Proper configuration for all services

### Testing

You can test the MFA functionality by:
1. Creating a user in the realm
2. Enabling MFA for the user through the user account
3. Logging in with the user's credentials
4. Selecting an MFA method and configuring it if necessary
5. Verifying the authentication with the selected method

## Architecture Overview

```mermaid
graph TD
    User((User)) --> KC[Keycloak]
    KC --> Auth[CustomMFAAuthenticator]
    
    Auth --> Factory[MFAProviderFactory]
    Factory --> Provider{MFA Provider}
    
    Provider --> SMS[SMSProvider]
    Provider --> Email[EmailProvider]
    Provider --> Telegram[TelegramProvider]
    Provider --> TOTP[TOTPProvider]
    
    SMS --> Twilio[TwilioServiceAdapter]
    Email --> EmailService[EmailServiceAdapter]
    Telegram --> TelegramBot[TelegramServiceAdapter]
    
    Auth --> EventMgr[AuthEventManager]
    EventMgr --> Listeners[Event Listeners]
    
    subgraph Flow[Authentication Flow]
        Select[Method Selection] --> Config[Configuration]
        Config --> Verify[Code Verification]
        Verify --> Success[Authentication Success]
    end
```

## Design Patterns Overview

```mermaid
graph TD
    subgraph Strategy[Strategy Pattern]
        StrategyInterface[MFAProvider Interface]
        StrategyAbstract[AbstractMFAProvider]
        Strategy1[SMSProvider]
        Strategy2[EmailProvider]
        Strategy3[TelegramProvider]
        Strategy4[TOTPProvider]
        
        StrategyInterface --> StrategyAbstract
        StrategyAbstract --> Strategy1
        StrategyAbstract --> Strategy2
        StrategyAbstract --> Strategy3
        StrategyAbstract --> Strategy4
    end
    
    subgraph Factory[Factory Pattern]
        FactoryClass[MFAProviderFactory]
        Client[CustomMFAAuthenticator]
        Product[MFAProvider]
        
        Client --> FactoryClass
        FactoryClass --> Product
    end
    
    subgraph Adapter[Adapter Pattern]
        AdapterInterface[ExternalServiceAdapter]
        Adapter1[TwilioServiceAdapter]
        Adapter2[EmailServiceAdapter]
        Adapter3[TelegramServiceAdapter]
        
        AdapterInterface --> Adapter1
        AdapterInterface --> Adapter2
        AdapterInterface --> Adapter3
    end
    
    subgraph Observer[Observer Pattern]
        Subject[AuthEventManager]
        Observer1[LoggingEventListener]
        ObserverInterface[AuthEventListener]
        EventClass[AuthEvent]
        
        Subject --> ObserverInterface
        ObserverInterface --> Observer1
        Subject --> EventClass
    end
    
    subgraph Template[Template Method]
        TemplateClass[AbstractMFAProvider]
        ConcreteClass[Concrete Providers]
        
        TemplateClass -- "defines flow" --> ConcreteClass
        ConcreteClass -- "override specific steps" --> TemplateClass
    end
    
    subgraph Singleton[Singleton Pattern]
        Single1[OTPGenerator]
        Single2[MFAProviderFactory]
        Single3[AuthEventManager]
        Single4[Service Adapters]
    end
    
    subgraph Builder[Builder Pattern]
        Director[Client Code]
        Builder1[AuthEvent.Builder]
        Builder2[MFAConfig.Builder]
        Builder3[EmailContentBuilder]
        
        Director --> Builder1
        Director --> Builder2
        Director --> Builder3
    end
```

## Class Diagram

```mermaid
classDiagram
    MFAProvider <|.. AbstractMFAProvider
    AbstractMFAProvider <|-- SMSProvider
    AbstractMFAProvider <|-- EmailProvider
    AbstractMFAProvider <|-- TelegramProvider
    AbstractMFAProvider <|-- TOTPProvider
    
    ExternalServiceAdapter <|.. TwilioServiceAdapter
    ExternalServiceAdapter <|.. EmailServiceAdapter
    ExternalServiceAdapter <|.. TelegramServiceAdapter
    
    SMSProvider --> TwilioServiceAdapter
    EmailProvider --> EmailServiceAdapter
    TelegramProvider --> TelegramServiceAdapter
    
    CustomMFAAuthenticator --> MFAProviderFactory
    MFAProviderFactory --> MFAProvider
    CustomMFAAuthenticator --> AuthEventManager
    AuthEventManager --> AuthEventListener
    AuthEventListener <|.. LoggingEventListener
    
    class MFAProvider {
        <<interface>>
        +isConfiguredFor(user)
        +sendVerificationCode(context, user)
        +verifyCode(context, user, code)
        +configure(context, user, configValue)
        +getType()
        +getDisplayName()
    }
    
    class AbstractMFAProvider {
        <<abstract>>
        #config: MFAConfig
        #otpGenerator: OTPGenerator
        +sendVerificationCode(context, user)
        +verifyCode(context, user, code)
        #sendCode(context, user, code)
    }
    
    class ExternalServiceAdapter {
        <<interface>>
        +isConfigured()
        +sendVerificationCode(recipient, code)
        +verifyCode(recipient, code)
    }
    
    class MFAProviderFactory {
        -instance: MFAProviderFactory
        +getInstance()
        +createProvider(type, config)
    }
    
    class CustomMFAAuthenticator {
        -providerFactory: MFAProviderFactory
        -eventManager: AuthEventManager
        +authenticate(context)
        +action(context)
    }
```

## Design Patterns Implemented

### 1. Strategy Pattern

The Strategy Pattern defines a family of algorithms, encapsulates each one, and makes them interchangeable. In this project, it's used to handle different MFA methods:

- **Interface**: `MFAProvider`
- **Abstract Class**: `AbstractMFAProvider`
- **Concrete Implementations**:
  - `SMSProvider`
  - `EmailProvider`
  - `TelegramProvider`
  - `TOTPProvider`

This allows each provider to implement method-specific logic while sharing common functionality through the abstract class.

### 2. Factory Pattern

The Factory Pattern provides an interface for creating objects without specifying their concrete classes. 

- **Factory**: `MFAProviderFactory`

This factory creates the appropriate MFA provider based on the requested type, hiding the instantiation logic from the client code.

### 3. Template Method Pattern

The Template Method Pattern defines the skeleton of an algorithm, deferring some steps to subclasses.

- **Abstract Class**: `AbstractMFAProvider`
- **Template Methods**:
  - `sendVerificationCode()` - Defines the flow for sending codes
  - `verifyCode()` - Defines the flow for verifying codes

This allows consistent handling while letting subclasses implement method-specific steps.

### 4. Singleton Pattern

The Singleton Pattern ensures a class has only one instance and provides a global point to access it.

- **Singleton Classes**:
  - `MFAProviderFactory`
  - `AuthEventManager`
  - `OTPGenerator`
  - Service adapters (TwilioServiceAdapter, TelegramServiceAdapter, EmailServiceAdapter)

This prevents unnecessary creation of objects that should be shared.

### 5. Builder Pattern

The Builder Pattern separates the construction of a complex object from its representation.

- **Builders**:
  - `AuthEvent.Builder`
  - `MFAConfig.Builder`
  - `EmailServiceAdapter.EmailContentBuilder`

This makes object creation clearer and provides a fluent API.

### 6. Adapter Pattern

The Adapter Pattern converts the interface of a class into another interface clients expect.

- **Interface**: `ExternalServiceAdapter`
- **Implementations**:
  - `TwilioServiceAdapter`
  - `TelegramServiceAdapter`
  - `EmailServiceAdapter`

This provides a consistent interface for working with external services.

### 7. Observer Pattern

The Observer Pattern defines a one-to-many dependency between objects so that when one object changes state, all its dependents are notified.

- **Event Class**: `AuthEvent`
- **Event Manager**: `AuthEventManager`
- **Event Listener Interface**: `AuthEventListener`
- **Example Listener**: `LoggingEventListener`

This allows for logging, metrics, and other cross-cutting concerns without cluttering the core code.

## Key Benefits

1. **Extensibility**: Adding new MFA methods is easy - just implement a new provider.
2. **Testability**: The modular design makes unit testing much simpler.
3. **Separation of Concerns**: Each class has a single responsibility.
4. **Code Reuse**: Common logic is shared through abstract classes and utilities.
5. **Maintainability**: The clean architecture makes the code easier to understand and modify.
6. **Error Handling**: Consistent exception handling across components.
7. **Logging**: Centralized logging through events.

## Package Structure

```
com.example.mfa/
├── authenticator/
│   ├── CustomMFAAuthenticator.java       # Main authenticator
│   └── CustomMFAAuthenticatorFactory.java
├── config/
│   └── MFAConfig.java                    # Configuration
├── provider/
│   ├── MFAProvider.java                  # Interface for all providers
│   ├── AbstractMFAProvider.java          # Abstract base class
│   ├── MFAException.java                 # Custom exception
│   ├── SMSProvider.java                  # Implementation for SMS
│   ├── EmailProvider.java                # Implementation for Email
│   ├── TelegramProvider.java             # Implementation for Telegram
│   └── TOTPProvider.java                 # Implementation for TOTP
├── factory/
│   └── MFAProviderFactory.java           # Factory for creating providers
├── service/
│   ├── ExternalServiceAdapter.java       # Interface for external services
│   ├── TwilioServiceAdapter.java         # Implementation for Twilio
│   ├── TelegramServiceAdapter.java       # Implementation for Telegram
│   └── EmailServiceAdapter.java          # Implementation for Email
├── event/
│   ├── AuthEvent.java                    # Event class
│   ├── AuthEventListener.java            # Listener interface
│   ├── AuthEventManager.java             # Event manager
│   └── LoggingEventListener.java         # Example listener
└── util/
    ├── OTPGenerator.java                 # Utility for OTP generation
    └── ValidationUtil.java               # Validation utilities
```

## How to Use

To add a new MFA method:

1. Create a new implementation of `MFAProvider`
2. Add the new method type to the `MFAProviderFactory`
3. Update the UI templates if necessary

The main authenticator doesn't need to be modified when adding new methods, as it delegates to the appropriate provider through the factory.

## Requirements

### Twilio (for SMS authentication)
- Twilio account with Account SID and Auth Token
- Twilio Verify Service set up with SMS capability
- Twilio Verify Service SID

### Telegram (for Telegram authentication)
- Telegram Bot created through BotFather
- Telegram Bot Token
- Users will need to message the bot to get their Chat ID

### Email (for Email authentication)
- Working SMTP configuration in Keycloak
- Or custom SMTP configuration provided to the plugin

### TOTP (for Authenticator App)
- Uses Keycloak's built-in TOTP implementation
- Users will need an authenticator app like Google Authenticator, Microsoft Authenticator, or Authy

## Troubleshooting

### Configuration Issues
- If MFA methods stay in "development mode" despite configuration:
  - Check that the exact key names match between the factory and adapters
  - Verify authentication flow configuration in Keycloak
  - Enable DEBUG logging for detailed configuration tracing

### TOTP Configuration
- If users aren't redirected to TOTP setup:
  - Ensure the CONFIGURE_TOTP required action is enabled in your realm
  - Check that TOTP is properly set up in the authentication flow

### Email Delivery Issues
- Verify Keycloak's email configuration is working
- Test email sending through Keycloak's test feature
- Check spam folders for MFA verification emails

### Logging
- Enable DEBUG level logging for `com.example.mfa` package
- Examine `AuthEvent` logs for authentication flow issues
- Service adapters log connection and delivery attempts

### Common Errors
- "Configuration error occurred": Check service credentials
- "Verification session has expired": Increase OTP timeout
- "Failed to send verification code": Check service connectivity
- "Invalid verification code": Ensure clock synchronization for TOTP