package com.example.mfa.service;

import org.jboss.logging.Logger;
import com.example.mfa.config.MFAConfig;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailService {
    private static final Logger logger = Logger.getLogger(EmailService.class);
    private final MFAConfig config;
    
    public EmailService(MFAConfig config) {
        this.config = config;
    }
    
    public void sendOTP(String email, String otp) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", config.getSmtpHost());
        props.put("mail.smtp.port", config.getSmtpPort());

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.getSmtpUsername(), config.getSmtpPassword());
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(config.getSmtpFromEmail()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
        message.setSubject("Your Verification Code");
        
        // Using both plain text and HTML for better compatibility
        String plainText = "Your verification code is: " + otp;
        String htmlContent = "<html><body><p>Your verification code is: <b>" + otp + "</b></p></body></html>";
        
        // Create multipart message
        Multipart multipart = new MimeMultipart("alternative");
        
        // Plain text part
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(plainText, "utf-8");
        
        // HTML part
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(htmlContent, "text/html; charset=utf-8");
        
        // Add parts to multipart
        multipart.addBodyPart(textPart);
        multipart.addBodyPart(htmlPart);
        
        // Set content
        message.setContent(multipart);

        Transport.send(message);
        logger.info("Email OTP sent successfully to: " + email);
    }
}