package com.example.AutomateEmailSenderAgent.repository;

import com.example.AutomateEmailSenderAgent.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Integer> {


    List<Conversation> findByCustomerEmailOrderByTimestampAsc(String customerEmail);

    List<Conversation> findByThreadIdOrderByTimestampAsc(String threadId);

    List<Conversation> findByStatusOrderByTimestampDesc(String status);
//    Optional<Conversation> findById(Integer id);

    Optional<Conversation> findByMessageId(String messageId);

}
