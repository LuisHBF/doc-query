package br.com.docquery.gateway.auth.application;

import br.com.docquery.gateway.auth.domain.User;
import br.com.docquery.gateway.auth.domain.UserRepository;
import br.com.docquery.gateway.auth.usecase.RegisterUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegisterUserAppService implements RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UUID handle(Command command) {
        if (userRepository.existsByEmail(command.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        User user = User.builder()
                .id(UUID.randomUUID())
                .email(command.getEmail())
                .passwordHash(passwordEncoder.encode(command.getPassword()))
                .build();

        return userRepository.save(user);
    }

}