package com.busgallery.busgallery.config;

import com.busgallery.busgallery.auth.AuthTokenInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * WebMvcConfig类用于封装WebMvcConfig相关的领域职责（所在包：com.busgallery.busgallery.config）。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthTokenInterceptor authTokenInterceptor;

    /**
     * addFormatters方法用于处理addFormatters相关的业务逻辑。
     * @param registry registry参数，详见调用方上下文。
     * @return 无返回值。
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, LocalDate.class,
                source -> source == null || source.isEmpty()
                        ? null
                        : LocalDate.parse(source, DateTimeFormatter.ISO_DATE));
    }

    /**
     * addInterceptors方法用于处理addInterceptors相关的业务逻辑。
     * @param registry registry参数，详见调用方上下文。
     * @return 无返回值。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authTokenInterceptor)
                .addPathPatterns("/api/**");
    }
}
