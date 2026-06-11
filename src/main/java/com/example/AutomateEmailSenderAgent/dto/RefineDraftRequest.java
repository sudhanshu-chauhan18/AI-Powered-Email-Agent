package com.example.AutomateEmailSenderAgent.dto;

import lombok.Data;

@Data
public class RefineDraftRequest {

    private Integer draftId;
    private String instructions;

}
