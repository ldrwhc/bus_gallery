package com.busgallery.busgallery.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SwaggerConfig类用于封装SwaggerConfig相关的领域职责（所在包：com.busgallery.busgallery.config）。
 */
@Configuration
public class SwaggerConfig {

    /**
     * busGalleryOpenAPI方法用于处理busGalleryOpenAPI相关的业务逻辑。
     * @return 返回OpenAPI类型结果。
     */
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