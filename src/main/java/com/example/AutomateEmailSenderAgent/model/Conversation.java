package com.example.AutomateEmailSenderAgent.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CurrentTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int Id;

    private String threadId;
    private String messageId;

    private String customerEmail;
    private String subject;
    private String role;
    private String status;

    @Column(columnDefinition = "text")
    private String message;

    @CurrentTimestamp
    private LocalDateTime timestamp;

}
