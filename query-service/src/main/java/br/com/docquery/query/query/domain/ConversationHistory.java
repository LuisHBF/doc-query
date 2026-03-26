package br.com.docquery.query.query.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class ConversationHistory {

    UUID id;
    UUID documentId;
    UUID userId;
    String question;
    String answer;
    LocalDateTime askedAt;

}