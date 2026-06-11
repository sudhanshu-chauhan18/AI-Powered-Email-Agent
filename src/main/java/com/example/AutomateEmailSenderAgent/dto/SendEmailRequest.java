package com.example.AutomateEmailSenderAgent.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendEmailRequest {

    private String to;
    private String subject;
    private String body;
    private String threadId;

}
