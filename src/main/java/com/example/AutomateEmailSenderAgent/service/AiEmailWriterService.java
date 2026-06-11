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
                        Return email in this format:

                        Subject: <subject>

                        Body:
                        <body>

                        %s
                        """.formatted(prompt);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put(
                "model",
                "llama-3.3-70b-versatile"
        );

        requestBody.put(
                "messages",
                List.of(
                        Map.of(
                                "role",
                                "user",
                                "content",
                                finalPrompt
                        )
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.setBearerAuth(apiKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                        apiUrl,
                        HttpMethod.POST,
                        entity,
                        Map.class
        );

        List<Map<String, Object>> choices = (List<Map<String, Object>>)
                response.getBody().get("choices");

        Map<String, Object> firstChoice = choices.get(0);

        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");

        String aiResponse = message.get("content").toString();

        String subject = "";
        String body = aiResponse;
        if(aiResponse.startsWith("Subject:")) {
            String[] parts =
                    aiResponse.split("\n", 2);

            subject = parts[0]
                    .replace("Subject:", "")
                    .trim();

            body = parts[1]
                    .replace("Body:", "")
                    .trim();
        }

        return new GenerateEmailResponse(
                subject,
                body
        );
    }

    public GenerateReplyResponse generateReply(String threadId, String context) {

        String prompt = """
            You are an email assistant.
            Below is the email conversation:

            %s

            Generate a professional reply
            to the latest customer message.
            """
                .formatted(context);

        GenerateEmailResponse response = generateEmail(prompt);

        return new GenerateReplyResponse(threadId, response.getBody()
        );
    }

    //Refine version of existing email
    public GenerateEmailResponse refineDraft(String subject, String message, String instructions) {

        String prompt = """
            Improve the following email.

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
                        message
                );

        return generateEmail(prompt);

    }
}