package com.santhan.banking_system.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException; // Import MailException
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            // IMPORTANT: The 'from' address must match your spring.mail.username in application.properties
            message.setFrom("kiranmayeekarnala@gmail.com"); // Ensure this matches your configured sender email
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            System.out.println("Email sent successfully to: " + to); // For debugging
        } catch (MailException e) { // Catch the specific MailException
            System.err.println("ERROR: Failed to send email to " + to + ": " + e.getMessage());
            e.printStackTrace(); // Print full stack trace for detailed error
            throw new RuntimeException("Failed to send email to " + to, e);
        }
    }
}
