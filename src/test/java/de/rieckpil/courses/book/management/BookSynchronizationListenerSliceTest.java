package de.rieckpil.courses.book.management;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.aws.autoconfigure.messaging.MessagingAutoConfiguration;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@Import(BookSynchronizationListener.class)
@ImportAutoConfiguration(MessagingAutoConfiguration.class)
class BookSynchronizationListenerSliceTest {

  private static final Logger LOG = LoggerFactory.getLogger(BookSynchronizationListenerSliceTest.class);

  @Autowired
  private BookSynchronizationListener cut;

  @Autowired
  private QueueMessagingTemplate queueMessagingTemplate;

  @Autowired
  private SimpleMessageListenerContainer simpleMessageListenerContainer;

  @MockBean
  private BookRepository bookRepository;

  @MockBean
  private OpenLibraryApiClient openLibraryApiClient;

  private static final String QUEUE_NAME = UUID.randomUUID().toString();
  private static final String ISBN = "9780596004651";
  static LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:0.13.3"))
    .withServices(LocalStackContainer.Service.SQS)
    .withLogConsumer(new Slf4jLogConsumer(LOG));

  @BeforeAll
  static void beforeAll() throws IOException, InterruptedException {
    localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", QUEUE_NAME);
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry dynamicPropertyRegistry) {
    dynamicPropertyRegistry.add("sqs.book-synchronization-queue", () -> QUEUE_NAME);
  }

  static  {
    localStack.start();
  }

  @TestConfiguration
  static class TestConfig{

    @Bean
    public AmazonSQSAsync amazonSQS() {
      return AmazonSQSAsyncClientBuilder.standard()
        .withEndpointConfiguration(
          new AwsClientBuilder.EndpointConfiguration(
            localStack.getEndpointOverride(LocalStackContainer.Service.SQS).toString(),
            localStack.getRegion()
          )
        ).withCredentials(
          new AWSStaticCredentialsProvider(
            new BasicAWSCredentials(localStack.getAccessKey(), localStack.getSecretKey())
          )
        ).build();
    }

    @Bean
    public QueueMessagingTemplate queueMessagingTemplate(AmazonSQSAsync amazonSQS) {
      return new QueueMessagingTemplate(amazonSQS);
    }

  }

  @Test
  public void shouldStartSQS() {
    assertNotNull(cut);
    assertNotNull(queueMessagingTemplate);
    assertNotNull(simpleMessageListenerContainer);
  }

  @Test
  public void shouldConsumeMessageWhenPayloadIsCorrect() {
    queueMessagingTemplate.convertAndSend(QUEUE_NAME, new BookSynchronization(ISBN));

    when(bookRepository.findByIsbn(ISBN)).thenReturn(new Book());

    given()
      .await()
      .atMost(5, TimeUnit.SECONDS)
      .untilAsserted(() -> verify(bookRepository).findByIsbn(ISBN));
  }
}
