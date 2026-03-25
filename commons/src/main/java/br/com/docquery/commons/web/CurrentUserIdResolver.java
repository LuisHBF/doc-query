package br.com.docquery.commons.web;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

public class CurrentUserIdResolver implements HandlerMethodArgumentResolver {

    private static final String HEADER_NAME = "X-Api-Gateway-User-Id";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class);
    }

    @Override
    public UUID resolveArgument(MethodParameter parameter,
                                ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest,
                                WebDataBinderFactory binderFactory) {

        String headerValue = webRequest.getHeader(HEADER_NAME);

        if (headerValue == null || headerValue.isBlank()) {
            throw new IllegalArgumentException(
                    "Required header '" + HEADER_NAME + "' is missing or empty"
            );
        }

        try {
            return UUID.fromString(headerValue);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Header '" + HEADER_NAME + "' is not a valid UUID: " + headerValue
            );
        }
    }

}
