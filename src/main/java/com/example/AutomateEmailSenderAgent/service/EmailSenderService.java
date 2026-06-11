package com.example.AutomateEmailSenderAgent.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailSenderService {
    private final JavaMailSender mailSender;

    public void sendEmail(String to, String subject, String body, String messageId) {

//        SimpleMailMessage message = new SimpleMailMessage();
//        message.setTo(to);
//        message.setSubject(subject);
//        message.setText(body);
//        mailSender.send(message);

            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);

                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(body);

                mimeMessage.setHeader("Message-ID", messageId);
                mailSender.send(mimeMessage);

            } catch (Exception e) {
                throw new RuntimeException("Mail Not Sent : " +e );
            }
    }
}



