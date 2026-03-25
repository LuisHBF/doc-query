package br.com.docquery.gateway.auth.domain;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class User {

    UUID id;
    String email;
    String passwordHash;

}