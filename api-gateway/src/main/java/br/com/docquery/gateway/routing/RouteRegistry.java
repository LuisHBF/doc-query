package br.com.docquery.gateway.routing;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Optional;

@Component
public class RouteRegistry {

    private static final String DOCUMENT_SERVICE = "http://localhost:8081";
    private static final String QUERY_SERVICE    = "http://localhost:8083";

    private final List<RouteDefinition> routes;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RouteRegistry() {
        routes = List.of(
                new RouteDefinition("/documents",             List.of(HttpMethod.POST),             DOCUMENT_SERVICE),
                new RouteDefinition("/documents",             List.of(HttpMethod.GET),              DOCUMENT_SERVICE),
                new RouteDefinition("/documents/{id}",        List.of(HttpMethod.GET),              DOCUMENT_SERVICE),
                new RouteDefinition("/documents/{id}",        List.of(HttpMethod.DELETE),           DOCUMENT_SERVICE),
                new RouteDefinition("/documents/{id}/chat",   List.of(HttpMethod.POST),             QUERY_SERVICE),
                new RouteDefinition("/documents/{id}/history",List.of(HttpMethod.GET),              QUERY_SERVICE),
                new RouteDefinition("/documents/{id}/history",List.of(HttpMethod.DELETE),           QUERY_SERVICE)
        );
    }

    public Optional<RouteDefinition> resolve(String path, HttpMethod method) {
        return routes.stream()
                .filter(route -> pathMatcher.match(route.pathPattern(), path))
                .filter(route -> route.methods().contains(method))
                .findFirst();
    }
}
