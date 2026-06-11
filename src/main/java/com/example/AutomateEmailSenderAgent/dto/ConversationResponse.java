package com.example.AutomateEmailSenderAgent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversationResponse {

    private String email;
    private String subject;
    private String role;
    private String message;
    private String status;
    private String threadId;
    private LocalDateTime timestamp;

}
