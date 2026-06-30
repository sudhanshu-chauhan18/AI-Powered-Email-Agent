package com.example.AutomateEmailSenderAgent.controller;

import com.example.AutomateEmailSenderAgent.dto.*;
import com.example.AutomateEmailSenderAgent.model.Conversation;
import com.example.AutomateEmailSenderAgent.service.AiEmailWriterService;
import com.example.AutomateEmailSenderAgent.service.ConversationService;
import com.example.AutomateEmailSenderAgent.service.EmailSenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.SpringVersion;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class UserController {

    private final EmailSenderService emailSenderService;
    private final ConversationService conversationService;
    private final AiEmailWriterService aiEmailWriterService;

    @PostMapping("/send")
    public ResponseEntity<String> sendEmail(@RequestBody SendEmailRequest request) {
        conversationService.sendAndSaveEmail(
                request.getTo(),
                request.getSubject(),
                request.getBody(),
                request.getThreadId());
        return ResponseEntity.ok(
                "Email Sent Successfully");
    }

    @GetMapping("/conversations/{email}")
    public List<ConversationResponse> getConversation(@PathVariable String email) {
        return conversationService.getConversationByEmail(email);
    }

    @GetMapping("/thread/{threadId}")
    public List<ConversationResponse> getThread(@PathVariable String threadId) {
        return conversationService.findThread(threadId);
    }

    @PostMapping("/generate")
    public GenerateEmailResponse generateEmail(@RequestBody GenerateEmailRequest request) {
        return aiEmailWriterService.generateEmail(request.getPrompt());
    }

    // generate the mail
    @PostMapping("/generate-save")
    public GenerateEmailResponse generateAndSave(@RequestBody GenerateAndSaveRequest request) {

        GenerateEmailResponse draft = aiEmailWriterService.generateEmail(request.getPrompt());
        conversationService.saveDraft(
                request.getEmail(),
                draft.getSubject(),
                draft.getBody(),
                request.getThreadId());
        return draft;
    }

    // Save a custom email draft | custom changes
    @PostMapping("/draft/save-custom")
    public ResponseEntity<String> saveCustomDraft(@RequestBody SendEmailRequest request) {
        conversationService.saveDraft(
                request.getTo(),
                request.getSubject(),
                request.getBody(),
                request.getThreadId());
        return ResponseEntity.ok("Draft Saved Successfully");
    }

    // Get All the draft of email
    @GetMapping("/drafts")
    public List<ConversationResponse> getDrafts() {
        return conversationService.getDrafts();
    }

    // send the draft email.
    @PostMapping("/draft/send")
    public ConversationResponse sendDraft(@RequestBody SendDraftRequest request) {
        return conversationService.sendDraft(request.getConversationId());
    }

    // delete the draft email.
    @DeleteMapping("/draft/{id}")
    public ResponseEntity<String> deleteDraft(@PathVariable Integer id) {
        conversationService.deleteDraft(id);
        return ResponseEntity.ok("Draft deleted successfully");
    }

    @GetMapping("/sent")
    public List<ConversationResponse> getSentEmails() {
        return conversationService.getSentEmails();
    }

    // Generate the reply of email
    @PostMapping("/generate-reply")
    public GenerateReplyResponse generateReply(@RequestBody GenerateReplyRequest request) {
        String context = conversationService.buildConversationContext(
                request.getThreadId());

        return aiEmailWriterService.generateReply(
                request.getThreadId(),
                context);
    }

    // Refine the existing email draft.
    @PostMapping("/draft/refine")
    public ConversationResponse refineDraft(@RequestBody RefineDraftRequest request) {
        return conversationService.refineDraft(
                request.getDraftId(),
                request.getInstructions());
    }

}
