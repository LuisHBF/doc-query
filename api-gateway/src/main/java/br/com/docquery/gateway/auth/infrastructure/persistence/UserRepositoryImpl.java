package br.com.docquery.gateway.auth.infrastructure.persistence;

import br.com.docquery.gateway.auth.domain.User;
import br.com.docquery.gateway.auth.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public UUID save(User user) {
        UserEntity entity = UserEntity.builder()
                .id(user.getId())
                .email(user.getEmail())
                .passwordHash(user.getPasswordHash())
                .createdAt(LocalDateTime.now())
                .build();

        UserEntity saved = userJpaRepository.save(entity);
        return saved.getId();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email)
                .map(entity -> User.builder()
                        .id(entity.getId())
                        .email(entity.getEmail())
                        .passwordHash(entity.getPasswordHash())
                        .build());
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

}