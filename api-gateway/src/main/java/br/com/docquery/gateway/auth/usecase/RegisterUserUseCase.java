package br.com.docquery.gateway.auth.usecase;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

public interface RegisterUserUseCase {

    UUID handle(Command command);

    @Value
    @Builder
    class Command {

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password;

    }

}