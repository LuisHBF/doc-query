package br.com.docquery.gateway.auth;

import br.com.docquery.gateway.BaseIntegrationTest;
import br.com.docquery.gateway.auth.usecase.RegisterUserUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegisterUserUseCaseTest extends BaseIntegrationTest {

    @Autowired
    private RegisterUserUseCase registerUserUseCase;

    @Test
    @DisplayName("returns a non-null UUID when a new user is registered with a unique email")
    void returnsNonNullUuidWhenNewUserRegisteredWithUniqueEmail() {
        RegisterUserUseCase.Command command = RegisterUserUseCase.Command.builder()
                .email("newuser@example.com")
                .password("password123")
                .build();

        UUID userId = registerUserUseCase.handle(command);

        assertThat(userId).isNotNull();
    }

    @Test
    @DisplayName("two different registrations with different emails produce different UUIDs")
    void twoDifferentRegistrationsProduceDifferentUuids() {
        RegisterUserUseCase.Command first = RegisterUserUseCase.Command.builder()
                .email("user.a@example.com")
                .password("password123")
                .build();
        RegisterUserUseCase.Command second = RegisterUserUseCase.Command.builder()
                .email("user.b@example.com")
                .password("password456")
                .build();

        UUID firstId = registerUserUseCase.handle(first);
        UUID secondId = registerUserUseCase.handle(second);

        assertThat(firstId).isNotEqualTo(secondId);
    }

    @Test
    @DisplayName("throws IllegalArgumentException when registering with an email that is already in use")
    void throwsWhenEmailAlreadyInUse() {
        RegisterUserUseCase.Command command = RegisterUserUseCase.Command.builder()
                .email("duplicate@example.com")
                .password("password123")
                .build();
        registerUserUseCase.handle(command);

        RegisterUserUseCase.Command duplicate = RegisterUserUseCase.Command.builder()
                .email("duplicate@example.com")
                .password("different-password")
                .build();

        assertThatThrownBy(() -> registerUserUseCase.handle(duplicate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email already in use");
    }
}
