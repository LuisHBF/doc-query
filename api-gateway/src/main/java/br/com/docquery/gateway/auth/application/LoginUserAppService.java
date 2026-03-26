package br.com.docquery.gateway.auth.application;

import br.com.docquery.gateway.auth.domain.User;
import br.com.docquery.gateway.auth.domain.UserRepository;
import br.com.docquery.gateway.auth.infrastructure.security.JwtService;
import br.com.docquery.gateway.auth.usecase.LoginUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginUserAppService implements LoginUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public Response handle(Command command) {
        User user = userRepository.findByEmail(command.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(command.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getId());

        return new Response(token, jwtService.getExpiration());
    }

}