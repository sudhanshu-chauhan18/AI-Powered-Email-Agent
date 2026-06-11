package com.example.AutomateEmailSenderAgent.service;

import jakarta.mail.*;
import jakarta.mail.search.FlagTerm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class EmailListenerService {

    @Value("${imap.host}")
    private String host;

    @Value("${imap.port}")
    private String port;

    @Value("${imap.username}")
    private String username;

    @Value("${imap.password}")
    private String password;

    public List<Message> fetchUnreadEmails() {

        try {
            Properties props = new Properties();
            props.put(
                    "mail.store.protocol",
                    "imaps"
            );

            Session session = Session.getDefaultInstance(props);

            Store store = session.getStore("imaps");

            store.connect(
                    host,
                    username,
                    password
            );

            Folder inbox = store.getFolder("INBOX");

            inbox.open(Folder.READ_WRITE);

            Message[] messages = inbox.search(
                            new FlagTerm(new Flags(Flags.Flag.SEEN),
                                    false
                            )
                    );

            return Arrays.asList(messages);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String extractBody(Message message) throws Exception {

        if (message.isMimeType("text/plain")) {
            return message.getContent().toString();
        }

        if (message.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) message.getContent();

            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);

                if (bodyPart.isMimeType("text/plain")) {
                    return bodyPart.getContent().toString();
                }
            }
        }

        return "";
    }
}