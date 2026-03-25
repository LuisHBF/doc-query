package br.com.docquery.gateway.auth.domain;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    UUID save(User user);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

}