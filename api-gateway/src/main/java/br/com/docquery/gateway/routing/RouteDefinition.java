package br.com.docquery.gateway.routing;

import org.springframework.http.HttpMethod;

import java.util.List;

public record RouteDefinition(
        String pathPattern,
        List<HttpMethod> methods,
        String targetBaseUrl
) {}
