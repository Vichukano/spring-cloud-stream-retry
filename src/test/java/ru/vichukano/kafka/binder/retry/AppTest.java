package ru.vichukano.kafka.binder.retry;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EmbeddedKafka(
    topics = {"topic-in", "topic-out"},
    brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"},
    partitions = 1
)
public class AppTest {
    private static final String TOPIC_IN = "topic-in";
    private static final String TOPIC_OUT = "topic-out";
    private final BlockingQueue<ConsumerRecord<String, String>> consumerRecords = new LinkedBlockingQueue<>();
    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;
    @Autowired
    private App app;

    @BeforeAll
    void setUp() {
        var props = new HashMap<String, Object>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-id");
        var consumerFactory = new DefaultKafkaConsumerFactory<String, String>(props);
        var containerProps = new ContainerProperties(TOPIC_OUT);
        var listenerContainer = new KafkaMessageListenerContainer<>(consumerFactory, containerProps);
        listenerContainer.setupMessageListener((MessageListener<String, String>) consumerRecords::add);
        listenerContainer.start();
        ContainerTestUtils.waitForAssignment(listenerContainer, embeddedKafkaBroker.getPartitionsPerTopic());
    }

    @Test
    @Disabled
    void positiveTest() throws ExecutionException, InterruptedException, TimeoutException {
        app.counter.set(0);
        final String messageIn = "hello world";
        try (var producer = producer()) {
            producer.send(new ProducerRecord<>(TOPIC_IN, messageIn)).get(5, TimeUnit.SECONDS);
        }
        ConsumerRecord<String, String> record = consumerRecords.poll(5, TimeUnit.SECONDS);
        Assertions.assertThat(record).isNotNull();
        Assertions.assertThat(record.value()).isEqualTo("HELLO WORLD");
        Assertions.assertThat(app.counter.get()).isEqualTo(1);
    }

    @Test
    void testWithException() throws ExecutionException, InterruptedException, TimeoutException {
        app.counter.set(0);
        final String messageIn = "hello world";
        try (var producer = producer()) {
            producer.send(new ProducerRecord<>(TOPIC_IN, messageIn)).get(5, TimeUnit.SECONDS);
        }
        ConsumerRecord<String, String> record = consumerRecords.poll(5, TimeUnit.SECONDS);
        Assertions.assertThat(record).isNull();
        Assertions.assertThat(app.counter.get()).isEqualTo(3);
    }

    private KafkaProducer<String, String> producer() {
        var props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 0);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new KafkaProducer<>(props);
    }
}
