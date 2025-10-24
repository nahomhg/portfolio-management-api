package io.github.nahomgh.portfolio.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(title = "Crypto Portfolio API", version = "1.0",
                description = "Manage crypto assets, portfolios and transactions")
)
@Configuration
public class OpenApiConfig {}