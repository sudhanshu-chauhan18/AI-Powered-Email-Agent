package com.example.AutomateEmailSenderAgent.service;


import com.example.AutomateEmailSenderAgent.dto.ConversationResponse;
import com.example.AutomateEmailSenderAgent.dto.GenerateEmailResponse;
import com.example.AutomateEmailSenderAgent.model.Conversation;
import com.example.AutomateEmailSenderAgent.repository.ConversationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository repository;
    private final EmailSenderService emailSenderService;
    private final AiEmailWriterService aiEmailWriterService;

//    public void saveOutgoingEmail(String email, String subject, String body, String threadId){

        // this is the type of object creation : Setter based Object Creation

//        Conversation conversation = new Conversation();
//        conversation.setCustomerEmail(email);
//        conversation.setSubject(subject);
//        conversation.setMessage(body);
//        conversation.setRole("agent");
//        if(threadId == null || threadId.isBlank()) {
//            conversation.setThreadId(
//                    UUID.randomUUID().toString()
//            );
//        } else {
//            conversation.setThreadId(threadId);
//        }

        // Now using Builder class pattern to creating a object. Eske liye Entity class pe @Builder annotation lgana pdta h.

//        String actualThreadId = (threadId == null || threadId.isBlank())
//                        ? UUID.randomUUID().toString()
//                        : threadId;
//
//        String messageId = "<" + actualThreadId + "@emailagent.com>";
//
//
//        Conversation conversation = Conversation.builder()
//                .customerEmail(email)
//                .subject(subject)
//                .message(body)
//                .role("agent")
//                .status("sent")
//                .threadId(actualThreadId)
//                .messageId(messageId)
//                .build();
//
//        Conversation userSaved = repository.save(conversation);
//        return mapToResponse(userSaved);
//}

    public void sendAndSaveEmail(String email, String subject, String body, String threadId) {

        String actualThreadId = (threadId == null || threadId.isBlank())
                        ? UUID.randomUUID().toString()
                        : threadId;

        String messageId = "<" +  UUID.randomUUID() + "@emailagent.com>";

        emailSenderService.sendEmail(
                email,
                subject,
                body,
                messageId
        );

        Conversation conversation = Conversation.builder()
                        .customerEmail(email)
                        .subject(subject)
                        .message(body)
                        .role("agent")
                        .status("sent")
                        .threadId(actualThreadId)
                        .messageId(messageId)
                        .build();

        repository.save(conversation);
//        Conversation userSaved = repository.save(conversation);
//        return mapToResponse(userSaved);
    }

    private ConversationResponse mapToResponse(Conversation userSaved) {
        ConversationResponse response = new ConversationResponse();
        response.setId(userSaved.getId());
        response.setEmail(userSaved.getCustomerEmail());
        response.setSubject(userSaved.getSubject());
        response.setMessage(userSaved.getMessage());
        response.setRole(userSaved.getRole());
        response.setStatus(userSaved.getStatus());
        response.setThreadId(userSaved.getThreadId());
        response.setTimestamp(userSaved.getTimestamp());
        return response;
    }


    // Save the Draft into the database
    public Conversation saveDraft(String email, String subject, String body, String threadId) {

        String actualThreadId = threadId == null || threadId.isBlank()
                        ? UUID.randomUUID().toString()
                        : threadId;

        String messageId = "<" +  UUID.randomUUID() + "@emailagent.com>";

        Conversation conversation = Conversation.builder()
                        .customerEmail(email)
                        .subject(subject)
                        .message(body)
                        .role("agent")
                        .status("draft")
                        .threadId(actualThreadId)
                        .messageId(messageId)
                        .build();

        return repository.save(conversation);
    }


    // Send the draft.
    public ConversationResponse sendDraft(Integer conversationId) {
        Conversation draft = repository.findById(conversationId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Draft not found"
                                )
                        );
        String actualThreadId = draft.getThreadId() == null || draft.getThreadId().isBlank()
                                ? UUID.randomUUID().toString()
                                : draft.getThreadId();

//        String messageId = "<" +  UUID.randomUUID() + "@emailagent.com>";
        emailSenderService.sendEmail(
                draft.getCustomerEmail(),
                draft.getSubject(),
                draft.getMessage(),
                draft.getMessageId()
        );

        draft.setStatus("sent");
//        draft.setMessageId(messageId);
        Conversation saved = repository.save(draft);
        return mapToResponse(saved);
    }


    public List<ConversationResponse> getConversationByEmail(String email) {
        return repository.findByCustomerEmailOrderByTimestampAsc(email).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    public List<ConversationResponse> findThread(String threadId) {
        return repository.findByThreadIdOrderByTimestampAsc(threadId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    // get all the draft
    public List<ConversationResponse> getDrafts() {
        return repository
                .findByStatusOrderByTimestampDesc("draft")
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // It is for checking all sent emails.
    public List<ConversationResponse> getSentEmails() {
        return repository
                .findByStatusOrderByTimestampDesc("sent")
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    //Build the Context History
    public String buildConversationContext(String threadId) {

        List<Conversation> conversations =
                repository.findByThreadIdOrderByTimestampAsc(threadId);

        StringBuilder context = new StringBuilder();

        for (Conversation conversation : conversations) {
            context.append(conversation.getRole());
            context.append(": ");
            context.append(conversation.getMessage());
            context.append("\n");
        }

        return context.toString();
    }


    //for Refine the draft
    public ConversationResponse refineDraft(Integer draftId, String instructions) {
        Conversation draft = repository.findById(draftId)
                .orElseThrow(
                        ()->new RuntimeException("Draft Not Found.")
                );
        GenerateEmailResponse response =
                aiEmailWriterService.refineDraft(draft.getSubject(),draft.getMessage(),instructions);

        draft.setSubject(response.getSubject());
        draft.setMessage(response.getBody());
        draft.setStatus("draft");

        Conversation updatedDraft = repository.save(draft);  // update the database

        return mapToResponse(updatedDraft);


    }

    //Delete the draft
    public void deleteDraft(Integer id) {
        Conversation draft = repository.findById(id)
                .orElseThrow(()->new RuntimeException("Draft Not found."));

        repository.delete(draft);
    }
}
