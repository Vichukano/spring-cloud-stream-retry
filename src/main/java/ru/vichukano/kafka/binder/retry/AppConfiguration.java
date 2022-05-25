package ru.vichukano.kafka.binder.retry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.config.ListenerContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.SeekToCurrentErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@Slf4j(topic = "ru.vichukano.kafka.binder.retry")
public class AppConfiguration {

    @Bean
    public ListenerContainerCustomizer<AbstractMessageListenerContainer<?, ?>> customizer(
        SeekToCurrentErrorHandler customErrorHandler
    ) {
        return (((container, destinationName, group) -> {
            container.setErrorHandler(customErrorHandler);
        }));
    }

    @Bean
    public SeekToCurrentErrorHandler customErrorHandler() {
        var errorHandler = new SeekToCurrentErrorHandler(
            (consumerRecord, e) -> log.error("Got exception skip record record: {}", consumerRecord, e),
            new FixedBackOff(1000L, 10)
        );
        errorHandler.addNotRetryableException(App.MyCustomException.class);
        return errorHandler;
    }

}
