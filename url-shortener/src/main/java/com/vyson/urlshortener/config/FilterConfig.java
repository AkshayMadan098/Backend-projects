package com.vyson.urlshortener.config;

import com.vyson.urlshortener.filter.*;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * [Q2 & Q8] Central Filter Registration and Chaining Configuration.
 *
 * Demonstrates:
 * 1. [Q8] Ordering of Middlewares via setOrder():
 *    - Order 1: ResponseTimeFilter (Outermost wrapper)
 *    - Order 2: RequestLoggingFilter
 *    - Order 3: BlacklistFilter
 *    - Order 4: ApiKeyAuthenticationFilter
 *    - Order 5: TierAuthorizationFilter (Innermost before Controller)
 *
 * 2. [Q2] Route-level Mapping via addUrlPatterns():
 *    - Explicitly maps filters to specific routes or all routes ("/*").
 */
@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<ResponseTimeFilter> responseTimeFilterRegistration(ResponseTimeFilter filter) {
        FilterRegistrationBean<ResponseTimeFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(FilterOrderConstants.RESPONSE_TIME_ORDER);
        registration.addUrlPatterns("/*");
        registration.setName("ResponseTimeFilter");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RequestLoggingFilter> loggingFilterRegistration(RequestLoggingFilter filter) {
        FilterRegistrationBean<RequestLoggingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(FilterOrderConstants.REQUEST_LOGGING_ORDER);
        registration.addUrlPatterns("/*");
        registration.setName("RequestLoggingFilter");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<BlacklistFilter> blacklistFilterRegistration(BlacklistFilter filter) {
        FilterRegistrationBean<BlacklistFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(FilterOrderConstants.BLACKLIST_ORDER);
        registration.addUrlPatterns("/*");
        registration.setName("BlacklistFilter");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<ApiKeyAuthenticationFilter> apiKeyAuthFilterRegistration(ApiKeyAuthenticationFilter filter) {
        FilterRegistrationBean<ApiKeyAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(FilterOrderConstants.API_KEY_AUTH_ORDER);
        registration.addUrlPatterns("/*");
        registration.setName("ApiKeyAuthenticationFilter");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<TierAuthorizationFilter> tierAuthFilterRegistration(TierAuthorizationFilter filter) {
        FilterRegistrationBean<TierAuthorizationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(FilterOrderConstants.TIER_AUTHORIZATION_ORDER);
        registration.addUrlPatterns("/shorten/bulk", "/shorten/bulk/*");
        registration.setName("TierAuthorizationFilter");
        return registration;
    }
}
