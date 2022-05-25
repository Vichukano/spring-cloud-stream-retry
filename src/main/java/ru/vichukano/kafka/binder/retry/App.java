package ru.vichukano.kafka.binder.retry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Slf4j
@SpringBootApplication
public class App {
    final AtomicInteger counter = new AtomicInteger();

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean
    public Function<String, String> processor() {
        return in -> {
            log.info("Message in: {}", in);
            counter.incrementAndGet();
            final String out = in.toUpperCase();
            if (1 == 1) {
                throw new MyCustomException("BOOM");
            }
            log.info("Message out: {}", out);
            return out;
        };
    }

    static class MyCustomException extends RuntimeException {

        public MyCustomException(String message) {
            super(message);
        }

    }

}
