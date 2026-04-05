package br.com.docquery.gateway.auth.api;

import br.com.docquery.gateway.auth.usecase.LoginUserUseCase;
import br.com.docquery.gateway.auth.usecase.RegisterUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUserUseCase loginUserUseCase;

    @PostMapping("/register")
    public ResponseEntity<UUID> register(@Valid @RequestBody RegisterUserUseCase.Command command) {
        UUID userId = registerUserUseCase.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(userId);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginUserUseCase.Response> login(@Valid @RequestBody LoginUserUseCase.Command command) {
        LoginUserUseCase.Response response = loginUserUseCase.handle(command);
        return ResponseEntity.ok(response);
    }

}