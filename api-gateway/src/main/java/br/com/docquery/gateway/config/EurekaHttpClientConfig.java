package br.com.docquery.gateway.config;

import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.netflix.eureka.http.RestTemplateDiscoveryClientOptionalArgs;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

@Configuration
public class EurekaHttpClientConfig {

    @Bean
    @ConditionalOnMissingBean(AbstractDiscoveryClientOptionalArgs.class)
    public RestTemplateDiscoveryClientOptionalArgs eurekaDiscoveryClientOptionalArgs() {
        return new RestTemplateDiscoveryClientOptionalArgs(
                (sslContext, hostnameVerifier) -> new JdkClientHttpRequestFactory(),
                () -> null
        );
    }
}
