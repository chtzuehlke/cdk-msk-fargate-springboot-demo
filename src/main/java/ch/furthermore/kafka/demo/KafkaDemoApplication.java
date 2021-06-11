package ch.furthermore.kafka.demo;

import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * See https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.messaging.kafka
 * See https://docs.spring.io/spring-kafka/reference/html/
 * See https://www.oreilly.com/library/view/kafka-the-definitive/9781491936153/ch04.html
 */
@SpringBootApplication
public class KafkaDemoApplication implements CommandLineRunner {
	private static final String TOPIC_NAME = "TopicI";
	public final static Logger log = LoggerFactory.getLogger(KafkaDemoApplication.class);
	
	public static void main(String[] args) {
		if (System.getenv("RUN_CDK") != null) {
			CdkApp.main(args);
		}
		else {
			SpringApplication.run(KafkaDemoApplication.class, args);
		}
	}
	
	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;

	@Bean
    public NewTopic topic() {
        return TopicBuilder.name(TOPIC_NAME)
            .partitions(10)
            .replicas(1) // local: only one broker available, cloud: 2 brokers are available
            .build();
    }
	
	@KafkaListener(topics = TOPIC_NAME, id = "#{T(java.util.UUID).randomUUID().toString()}", groupId = "GroupB" ) 
    public void processMessage(ConsumerRecord<String, String> record, String content) { 
        log.info("Received: partition={}, offset={}, content={}", String.format("%4s", record.partition()), String.format("%4s", record.offset()), content); 
    }
	
	@Override
	public void run(String... args) throws Exception {
		String prefix = "" + ((int) (Math.random() * 10000));
		
		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			String message = "Test-" + prefix + "-" + i;
			
			RecordMetadata recordMetadata = kafkaTemplate.send(TOPIC_NAME, message).get(3, TimeUnit.SECONDS).getRecordMetadata();
			
			log.info("Sent    : partition={}, offset={}, content={}", String.format("%4s", recordMetadata.partition()), String.format("%4s", recordMetadata.offset()), message);
			
			Thread.sleep(10000);
		}
		
		log.info("Done");
	}
}
