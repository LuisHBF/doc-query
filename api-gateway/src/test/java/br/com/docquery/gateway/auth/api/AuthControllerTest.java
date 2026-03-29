package br.com.docquery.gateway.auth.api;

import br.com.docquery.gateway.auth.infrastructure.security.JwtService;
import br.com.docquery.gateway.auth.usecase.LoginUserUseCase;
import br.com.docquery.gateway.auth.usecase.RegisterUserUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@WithMockUser
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegisterUserUseCase registerUserUseCase;

    @MockBean
    private LoginUserUseCase loginUserUseCase;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("POST /auth/register returns 201 CREATED with the new user UUID when registration succeeds")
    void registerReturns201WithNewUserUuid() throws Exception {
        UUID userId = UUID.randomUUID();
        when(registerUserUseCase.handle(any())).thenReturn(userId);

        mockMvc.perform(post("/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").value(userId.toString()));
    }

    @Test
    @DisplayName("POST /auth/login returns 200 OK with token and expiresIn when credentials are valid")
    void loginReturns200WithTokenAndExpiresIn() throws Exception {
        LoginUserUseCase.Response response = new LoginUserUseCase.Response("jwt-token-value", 86400000L);
        when(loginUserUseCase.handle(any())).thenReturn(response);

        mockMvc.perform(post("/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-value"))
                .andExpect(jsonPath("$.expiresIn").value(86400000));
    }

    @Test
    @DisplayName("POST /auth/register delegates to RegisterUserUseCase with email and password from the request body")
    void registerDelegatesToUseCaseWithEmailAndPassword() throws Exception {
        when(registerUserUseCase.handle(any())).thenReturn(UUID.randomUUID());
        ArgumentCaptor<RegisterUserUseCase.Command> captor =
                ArgumentCaptor.forClass(RegisterUserUseCase.Command.class);

        mockMvc.perform(post("/auth/register").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"mypassword123\"}"));

        verify(registerUserUseCase).handle(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("test@example.com");
        assertThat(captor.getValue().getPassword()).isEqualTo("mypassword123");
    }
}
