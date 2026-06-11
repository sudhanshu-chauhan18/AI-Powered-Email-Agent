package com.example.AutomateEmailSenderAgent.service;

import com.example.AutomateEmailSenderAgent.dto.GenerateReplyResponse;
import com.example.AutomateEmailSenderAgent.model.Conversation;
import com.example.AutomateEmailSenderAgent.repository.ConversationRepository;
import jakarta.mail.Flags;
import jakarta.mail.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailProcessorService {

    private final EmailListenerService listenerService;
    private final ConversationRepository repository;
    private final ConversationService conversationService;
    private final AiEmailWriterService aiService;

    @Scheduled(fixedDelay = 60000)
    public void processReplies() {

        System.out.println("Checking Inbox...");

        List<Message> emails = listenerService.fetchUnreadEmails();

        for(Message email : emails) {
            try {
                processEmail(email);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void processEmail(Message email) throws Exception {

        String[] inReplyTo = email.getHeader("In-Reply-To");

        if(inReplyTo == null) {
            return;
        }

        Conversation original = repository.findByMessageId(inReplyTo[0])
                        .orElse(null);

        if(original == null) {
            System.out.println("No matching thread found");
            return;
        }

        String body = listenerService.extractBody(email);
//        System.out.println("BODY = " + body);

        //Save user response into database
        Conversation incoming = Conversation.builder()
                        .threadId(original.getThreadId())
                        .customerEmail(original.getCustomerEmail())
                        .subject(email.getSubject())
                        .message(body)
                        .role("user")
                        .status("received")
                        .build();

        repository.save(incoming);

        String context =
                conversationService.buildConversationContext(original.getThreadId());

        GenerateReplyResponse reply = aiService.generateReply(
                        original.getThreadId(),
                        context
                );

        String messageId = "<" +  UUID.randomUUID() + "@emailagent.com>";

        Conversation draft = Conversation.builder()
                        .threadId(original.getThreadId())
                        .customerEmail(original.getCustomerEmail())
                        .subject("Re: " + original.getSubject())
                        .message(reply.getReply())
                        .role("agent")
                        .status("draft")
                        .messageId(messageId)
                        .build();

        repository.save(draft);
        email.setFlag(Flags.Flag.SEEN, true);

        System.out.println("AI Draft Generated");
    }
}