package com.busgallery.busgallery.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI busGalleryOpenAPI() {
        return new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title("Bus Gallery API")
                        .version("1.0.0")
                        .description("公交车图库管理接口文档"));
    }
}