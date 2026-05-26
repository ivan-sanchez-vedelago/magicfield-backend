package com.magicfield.backend.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {

    @Bean
    public Counter checkoutCompletedCounter(MeterRegistry registry) {
        return Counter.builder("business.checkout.completed")
                .description("Number of successful checkouts")
                .register(registry);
    }

    @Bean
    public Counter checkoutFailedCounter(MeterRegistry registry) {
        return Counter.builder("business.checkout.failed")
                .description("Number of failed checkouts")
                .register(registry);
    }

    @Bean
    public Counter authFailedCounter(MeterRegistry registry) {
        return Counter.builder("business.auth.failed")
                .description("Number of failed authentication attempts")
                .register(registry);
    }

    @Bean
    public Timer dollarServiceTimer(MeterRegistry registry) {
        return Timer.builder("business.external.dollar_service")
                .description("Time spent calling dollar rate API")
                .register(registry);
    }

    @Bean
    public Timer scryfallServiceTimer(MeterRegistry registry) {
        return Timer.builder("business.external.scryfall_service")
                .description("Time spent calling Scryfall API")
                .register(registry);
    }
}
