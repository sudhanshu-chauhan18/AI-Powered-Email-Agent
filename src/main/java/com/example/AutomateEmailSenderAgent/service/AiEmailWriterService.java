package com.example.AutomateEmailSenderAgent.service;

import com.example.AutomateEmailSenderAgent.dto.GenerateEmailResponse;
import com.example.AutomateEmailSenderAgent.dto.GenerateReplyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiEmailWriterService {

        private final RestTemplate restTemplate;

        @Value("${groq.api.key}")
        private String apiKey;

        @Value("${groq.api.url}")
        private String apiUrl;

        public GenerateEmailResponse generateEmail(String prompt) {

                String finalPrompt = """
                                You are a Professional Email Assistant.

                                Rules:
                                1. You can ONLY generate, improve, rewrite, refine, summarize, or reply to emails.
                                2. If the user request is NOT related to email writing or email communication,
                                return exactly:

                                Subject: Not Applicable

                                Body:
                                Sorry, I can only help with email generation, email refinement, and email replies.

                                3. Always return response in this exact format:

                                Subject: <subject>

                                Body:
                                <body>

                                User Request:
                                %s
                                """.formatted(prompt);

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put(
                                "model",
                                "llama-3.3-70b-versatile");

                requestBody.put(
                                "messages",
                                List.of(
                                                Map.of(
                                                                "role",
                                                                "user",
                                                                "content",
                                                                finalPrompt)));

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                headers.setBearerAuth(apiKey);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                ResponseEntity<Map> response = restTemplate.exchange(
                                apiUrl,
                                HttpMethod.POST,
                                entity,
                                Map.class);

                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");

                Map<String, Object> firstChoice = choices.get(0);

                Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");

                String aiResponse = message.get("content").toString();

                String subject = "";
                String body = aiResponse;
                if (aiResponse.startsWith("Subject:")) {
                        String[] parts = aiResponse.split("\n", 2);

                        subject = parts[0]
                                        .replace("Subject:", "")
                                        .trim();

                        body = parts[1]
                                        .replace("Body:", "")
                                        .trim();
                }

                return new GenerateEmailResponse(
                                subject,
                                body);
        }

        public GenerateReplyResponse generateReply(String threadId, String context) {

                String prompt = """
                                You are a Professional Email Assistant.

                                Your job is ONLY to generate professional email replies.

                                Based on the conversation history and current email content, generate a reply to the customer email.

                                If the conversation does not contain a valid email discussion,
                                return:

                                Subject: Not Applicable

                                Body:
                                Sorry, I can only generate replies for email conversations.

                                Conversation:
                                %s
                                """
                                .formatted(context);

                GenerateEmailResponse response = generateEmail(prompt);

                return new GenerateReplyResponse(threadId, response.getBody());
        }

        // Refine version of existing email
        public GenerateEmailResponse refineDraft(String subject, String message, String instructions) {

                String prompt = """
                                You are a Professional Email Assistant.

                                You can ONLY improve email content.

                                If the instruction is unrelated to email writing,
                                return:

                                Subject: Not Applicable

                                Body:
                                Sorry, I can only assist with email-related tasks.

                                Instruction:
                                %s

                                Current Subject:
                                %s

                                Current Email:
                                %s

                                Return email in this format:

                                Subject: <subject>

                                Body:
                                <body>
                                """
                                .formatted(
                                                instructions,
                                                subject,
                                                message);

                return generateEmail(prompt);

        }
}