package br.com.docquery.gateway.routing;

import br.com.docquery.gateway.auth.infrastructure.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RoutingController {

    private final RouteRegistry routeRegistry;
    private final RequestForwarder requestForwarder;
    private final JwtService jwtService;

    @RequestMapping("/**")
    public ResponseEntity<byte[]> route(HttpServletRequest request) throws IOException {
        UUID userId = jwtService.extractUserIdFromSecurityContext();
        String path = request.getRequestURI();
        String queryString = request.getQueryString();
        HttpMethod method = HttpMethod.valueOf(request.getMethod());

        Optional<RouteDefinition> routeOpt = routeRegistry.resolve(path, method);

        if (routeOpt.isEmpty()) {
            log.warn("No route found for {} {}", method, path);
            return ResponseEntity.notFound().build();
        }

        RouteDefinition route = routeOpt.get();
        String targetUrl = route.targetBaseUrl() + path + (queryString != null ? "?" + queryString : "");

        log.info("Routing {} {} → {}", method, path, targetUrl);

        if (request instanceof MultipartHttpServletRequest multipartRequest) {
            return requestForwarder.forwardMultipart(multipartRequest, targetUrl, userId);
        }

        return requestForwarder.forward(request, targetUrl, userId);
    }
}
