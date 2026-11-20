package ru.mifi.learning.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigInteger;
import java.util.Arrays;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Pageable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ru.mifi.learning.exception.InvalidPageRequestException;

@Configuration(proxyBeanMethods = false)
class PaginationConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                if (handler instanceof HandlerMethod method
                        && Arrays.asList(method.getMethod().getParameterTypes()).contains(Pageable.class)) {
                    validate(request, "page", 0);
                    validate(request, "size", 1);
                }
                return true;
            }
        });
    }

    // Spring Data заменяет некорректные значения значениями по умолчанию при создании Pageable.
    private static void validate(HttpServletRequest request, String name, int minimum) {
        String raw = request.getParameter(name);
        if (raw == null) return;
        BigInteger number;
        try {
            number = new BigInteger(raw);
        } catch (NumberFormatException ex) {
            throw InvalidPageRequestException.parameter(name, minimum);
        }
        if (number.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0 && name.equals("page")) {
            throw InvalidPageRequestException.offset();
        }
        if (number.compareTo(BigInteger.valueOf(minimum)) < 0
                || number.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
            throw InvalidPageRequestException.parameter(name, minimum);
        }
    }
}
