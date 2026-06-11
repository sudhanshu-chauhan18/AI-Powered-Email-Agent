package com.example.AutomateEmailSenderAgent.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenerateEmailResponse {

    private String subject;
    private String body;

}
