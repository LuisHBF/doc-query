package br.com.docquery.gateway.routing;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestForwarder {

    private static final String USER_ID_HEADER = "X-Api-Gateway-User-Id";

    private final RestClient restClient;

    public ResponseEntity<byte[]> forward(HttpServletRequest request,
                                          String targetUrl,
                                          UUID userId) throws IOException {
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        byte[] body = request.getInputStream().readAllBytes();

        return restClient.method(method)
                .uri(targetUrl)
                .header(USER_ID_HEADER, userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(byte[].class);
    }

    public ResponseEntity<byte[]> forwardMultipart(MultipartHttpServletRequest request,
                                                   String targetUrl,
                                                   UUID userId) throws IOException {
        MultipartFile file = request.getFile("file");

        if (file == null) {
            return ResponseEntity.badRequest().build();
        }

        byte[] bytes = file.getBytes();
        String originalFilename = file.getOriginalFilename();

        ByteArrayResource resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return originalFilename;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);

        return restClient.post()
                .uri(targetUrl)
                .header(USER_ID_HEADER, userId.toString())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .toEntity(byte[].class);
    }
}
