package de.rieckpil.courses.book.management;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenLibraryApiClientTest {

  private MockWebServer mockWebServer;
  private OpenLibraryApiClient cut;

  private static final String ISBN = "9780596004651";

  private static String VALID_RESPONSE;

  static {
    try {
      VALID_RESPONSE = new String(Objects.requireNonNull(OpenLibraryApiClient.class
          .getClassLoader()
          .getResourceAsStream(String.format("stubs/openlibrary/success-%s.json", ISBN)))
        .readAllBytes());
    }catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @BeforeEach
  public void setup() throws IOException {
    this.mockWebServer = new MockWebServer();
    this.mockWebServer.start();

    HttpClient httpClient = HttpClient.create()
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2_000)
      .doOnConnected(connection ->
        connection.addHandlerLast(new ReadTimeoutHandler(2))
          .addHandlerLast(new WriteTimeoutHandler(2)));


    WebClient openLibraryWebClient = WebClient.builder()
      .clientConnector(new ReactorClientHttpConnector(httpClient))
      .baseUrl(this.mockWebServer.url("/").toString())
      .build();

    this.cut = new OpenLibraryApiClient(openLibraryWebClient);
  }

  @Test
  void notNull() {
    assertNotNull(mockWebServer);
    assertNotNull(cut);
  }

  @Test
  void shouldReturnBookWhenResultIsSuccess() throws InterruptedException {
    MockResponse mockResponse = new MockResponse()
      .addHeader("Content-Type", "application/json; charset=utf-8")
      .setBody(VALID_RESPONSE);

    this.mockWebServer.enqueue(mockResponse);

    Book result = cut.fetchMetadataForBook(ISBN);

    assertEquals("9780596004651", result.getIsbn());
    assertEquals(619, result.getPages());

    assertNull(result.getId());
    RecordedRequest recordedRequest = this.mockWebServer.takeRequest();
    assertEquals("/api/books?jscmd=data&format=json&bibkeys=ISBN:" + ISBN, recordedRequest.getPath());
  }

  @Test
  void shouldReturnBookWhenResultIsSuccessButLackingAllInformation() {
  }

  @Test
  void shouldPropagateExceptionWhenRemoteSystemIsDown() {
    this.mockWebServer.enqueue(new MockResponse()
      .setResponseCode(500)
      .setBody("Sorry, system is down"));

    assertThrows(RuntimeException.class, () -> this.cut.fetchMetadataForBook(ISBN));
  }

  @Test
  void shouldRetryWhenRemoteSystemIsSlowOrFailing() {
    this.mockWebServer.enqueue(new MockResponse()
      .setResponseCode(500)
      .setBody("Sorry, system is down"));


    this.mockWebServer.enqueue(new MockResponse()
      .addHeader("Content-Type", "application/json; charset=utf-8")
      .setResponseCode(200)
      .setBody(VALID_RESPONSE)
      .setBodyDelay(2, TimeUnit.SECONDS));


    this.mockWebServer.enqueue(new MockResponse()
      .addHeader("Content-Type", "application/json; charset=utf-8")
      .setResponseCode(200)
      .setBody(VALID_RESPONSE));

    Book result = cut.fetchMetadataForBook(ISBN);

    assertEquals("9780596004651", result.getIsbn());
    assertEquals(619, result.getPages());

    assertNull(result.getId());
  }
}
