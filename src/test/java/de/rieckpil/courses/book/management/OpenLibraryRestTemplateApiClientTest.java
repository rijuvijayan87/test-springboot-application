package de.rieckpil.courses.book.management;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.HttpServerErrorException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@RestClientTest(OpenLibraryRestTemplateApiClient.class)
class OpenLibraryRestTemplateApiClientTest {

  @Autowired
  private OpenLibraryRestTemplateApiClient cut;

  @Autowired
  private MockRestServiceServer mockRestServiceServer;

  private static final String ISBN = "9780596004651";

  @Test
  void shouldInjectBeans() {
    assertNotNull(cut);
    assertNotNull(mockRestServiceServer);

  }

  @Test
  void shouldReturnBookWhenResultIsSuccess() {
    this.mockRestServiceServer
      .expect(requestTo(String.format("/api/books?jscmd=data&format=json&bibkeys=ISBN:%s",ISBN)))
      .andRespond(withSuccess(new ClassPathResource(String.format("/stubs/openlibrary/success-%s.json", ISBN))
        , MediaType.APPLICATION_JSON));

    Book book = cut.fetchMetadataForBook(ISBN);
    assertNotNull(book);
    assertNull(book.getId());
  }

  @Test
  void shouldReturnBookWhenResultIsSuccessButLackingAllInformation() {

  }

  @Test
  void shouldPropagateExceptionWhenRemoteSystemIsDown() {
    this.mockRestServiceServer
      .expect(requestTo(String.format("/api/books?jscmd=data&format=json&bibkeys=ISBN:%s",ISBN)))
      .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

    assertThrows(HttpServerErrorException.class, () -> cut.fetchMetadataForBook(ISBN));
  }

  @Test
  void shouldContainCorrectHeadersWhenRemoteSystemIsInvoked() {
    this.mockRestServiceServer
      .expect(requestTo(String.format("/api/books?jscmd=data&format=json&bibkeys=ISBN:%s", ISBN)))
      .andExpect(header("X-Custom-Auth", "Duke42"))
      .andExpect(header("X-Customer-Id", "42"))
      .andRespond(withSuccess(new ClassPathResource(String.format("/stubs/openlibrary/success-%s.json", ISBN))
        , MediaType.APPLICATION_JSON));

    Book book = cut.fetchMetadataForBook(ISBN);
    assertNotNull(book);
  }
}
