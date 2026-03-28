package br.com.docquery.gateway.routing;

import br.com.docquery.gateway.auth.infrastructure.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserIdHeaderFilter extends OncePerRequestFilter {

    static final String USER_ID_HEADER = "X-Api-Gateway-User-Id";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            UUID userId = jwtService.extractUserIdFromToken(authHeader.substring(7));
            log.debug("Injecting {} header: {}", USER_ID_HEADER, userId);
            request = new UserIdRequestWrapper(request, userId.toString());
        }

        chain.doFilter(request, response);
    }

    private static class UserIdRequestWrapper extends HttpServletRequestWrapper {

        private final String userId;

        UserIdRequestWrapper(HttpServletRequest request, String userId) {
            super(request);
            this.userId = userId;
        }

        @Override
        public String getHeader(String name) {
            if (USER_ID_HEADER.equalsIgnoreCase(name)) return userId;
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (USER_ID_HEADER.equalsIgnoreCase(name)) {
                return Collections.enumeration(List.of(userId));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = Collections.list(super.getHeaderNames());
            names.add(USER_ID_HEADER);
            return Collections.enumeration(names);
        }
    }
}
