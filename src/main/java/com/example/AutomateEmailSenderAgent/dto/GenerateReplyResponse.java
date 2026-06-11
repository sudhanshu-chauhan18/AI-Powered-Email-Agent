package com.example.AutomateEmailSenderAgent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GenerateReplyResponse {

    private String threadId;
    private String reply;
}
