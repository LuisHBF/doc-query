package br.com.docquery.gateway.auth.usecase;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;

public interface LoginUserUseCase {

    Response handle(Command command);

    @Value
    @Builder
    class Command {

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email;

        @NotBlank(message = "Password is required")
        String password;

    }

    record Response(String token, long expiresIn) {}

}