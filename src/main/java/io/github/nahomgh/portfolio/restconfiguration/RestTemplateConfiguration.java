package io.github.nahomgh.portfolio.restconfiguration;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfiguration {

    @Bean // Injected into the Spring container - i.e. gives us access to the restTemplate throughout the application.
    public RestTemplate restTemplate(){
        var requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setConnectionRequestTimeout(Duration.ofMillis(5000));
        requestFactory.setReadTimeout(Duration.ofMillis(5000));
        return new RestTemplate(requestFactory);
    }
    // Sets time out if responses like totalCost fetch, serving cache data takes too long. Should throw a 503 error if timeout is hit.
}
