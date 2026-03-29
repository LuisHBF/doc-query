package br.com.docquery.gateway.auth;

import br.com.docquery.gateway.BaseIntegrationTest;
import br.com.docquery.gateway.auth.usecase.LoginUserUseCase;
import br.com.docquery.gateway.auth.usecase.RegisterUserUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginUserUseCaseTest extends BaseIntegrationTest {

    @Autowired
    private RegisterUserUseCase registerUserUseCase;

    @Autowired
    private LoginUserUseCase loginUserUseCase;

    @Test
    @DisplayName("returns a non-empty JWT token and a positive expiration when credentials are valid")
    void returnsJwtTokenAndPositiveExpirationWhenCredentialsAreValid() {
        RegisterUserUseCase.Command register = RegisterUserUseCase.Command.builder()
                .email("login.valid@example.com")
                .password("correctpassword")
                .build();
        registerUserUseCase.handle(register);
        LoginUserUseCase.Command login = LoginUserUseCase.Command.builder()
                .email("login.valid@example.com")
                .password("correctpassword")
                .build();

        LoginUserUseCase.Response response = loginUserUseCase.handle(login);

        assertThat(response.token()).isNotBlank();
        assertThat(response.expiresIn()).isPositive();
    }

    @Test
    @DisplayName("throws IllegalArgumentException when the email is not registered")
    void throwsWhenEmailIsNotRegistered() {
        LoginUserUseCase.Command login = LoginUserUseCase.Command.builder()
                .email("nonexistent@example.com")
                .password("anypassword")
                .build();

        assertThatThrownBy(() -> loginUserUseCase.handle(login))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    @DisplayName("throws IllegalArgumentException when the password is wrong — same message as not-found to prevent user enumeration")
    void throwsWhenPasswordIsWrong() {
        RegisterUserUseCase.Command register = RegisterUserUseCase.Command.builder()
                .email("login.wrong@example.com")
                .password("correctpassword")
                .build();
        registerUserUseCase.handle(register);
        LoginUserUseCase.Command login = LoginUserUseCase.Command.builder()
                .email("login.wrong@example.com")
                .password("wrongpassword")
                .build();

        assertThatThrownBy(() -> loginUserUseCase.handle(login))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid credentials");
    }
}
