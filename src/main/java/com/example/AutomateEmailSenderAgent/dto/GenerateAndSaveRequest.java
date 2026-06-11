package com.example.AutomateEmailSenderAgent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenerateAndSaveRequest {

    private String email;
    private String prompt;
    private String threadId;
}
