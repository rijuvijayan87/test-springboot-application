package de.rieckpil.courses.book.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

  private final String API_REVIEWS_ENDPOINT = "/api/books/reviews";
  @MockBean
  private ReviewService reviewService;

  @Autowired
  private MockMvc mockMvc;

  private ObjectMapper objectMapper;

  @BeforeEach
  public void beforeEach() {
    objectMapper = new ObjectMapper();
  }

  @Test
  void shouldReturnTwentyReviewsWithoutAnyOrderWhenNoParametersAreSpecified() throws Exception {
    ArrayNode result = objectMapper.createArrayNode();
    ObjectNode statistic = objectMapper.createObjectNode();
    statistic.put("bookId", 1);
    statistic.put("isbn", 42);
    statistic.put("avg", 89);
    statistic.put("ratings", 2);

    result.add(statistic);

    when(reviewService.getAllReviews(20, "none")).thenReturn(result);

    this.mockMvc
      .perform(get(API_REVIEWS_ENDPOINT))
      .andDo(print())
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.size()", Matchers.is(1)));
  }

  @Test
  void shouldNotReturnReviewStatisticsWhenUserIsUnauthenticated() throws Exception {
    this.mockMvc
      .perform(get("/api/books/reviews/statistics"))
      .andExpect(status().isUnauthorized());

    verifyNoInteractions(reviewService);
  }

  @Test
  void shouldReturnReviewStatisticsWhenUserIsAuthenticated() throws Exception {
    this.mockMvc
      .perform(get("/api/books/reviews/statistics")
        .with(jwt()))
      .andDo(print())
      .andExpect(status().isOk());

    verify(reviewService).getReviewStatistics();
  }

  @Test
  void shouldCreateNewBookReviewForAuthenticatedUserWithValidPayload() throws Exception {
    String requestBody = """
      {
        "reviewTitle": "Great java book",
        "reviewContent": "I really like this book!",
        "rating": 4
      }
      """;
    when(this.reviewService.createBookReview(eq("42"), any(BookReviewRequest.class),
      eq("duke"),endsWith("spring.io"))).thenReturn(84L);

    this.mockMvc
      .perform(post("/api/books/{isbn}/reviews", 42)
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody)
        .with(jwt().jwt(build -> build
          .claim("preferred_username", "duke")
          .claim("email", "duke@spring.io"))))
      .andDo(print())
      .andExpect(status().isCreated())
      .andExpect(header().exists("Location"))
      .andExpect(header().string("Location", Matchers.containsString("/books/42/reviews/84")));

    verify(reviewService, times(1)).createBookReview(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  @Test
  void shouldRejectNewBookReviewForAuthenticatedUsersWithInvalidPayload() throws Exception {
    String requestBody = """
      {
        "reviewTitle": "Great java book",
        "reviewContent": "I really like this book!",
        "rating": -1
      }
      """;
    when(this.reviewService.createBookReview(eq("42"), any(BookReviewRequest.class),
      eq("duke"),endsWith("spring.io"))).thenReturn(84L);

    this.mockMvc
      .perform(post("/api/books/{isbn}/reviews", 42)
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody)
        .with(jwt().jwt(build -> build
          .claim("preferred_username", "duke")
          .claim("email", "duke@spring.io"))))
      .andDo(print())
      .andExpect(status().isBadRequest());
  }

  @Test
  void shouldNotAllowDeletingReviewsWhenUserIsAuthenticatedWithoutModeratorRole() throws Exception {
    this.mockMvc
      .perform(delete("/api/books/{isbn}/reviews/{reviewId}", 42, 74)
        .with(jwt()))
      .andExpect(status().isForbidden());

    verifyNoInteractions(reviewService);
  }

  @Test
  //@WithMockUser(roles = "moderator")
  void shouldAllowDeletingReviewsWhenUserIsAuthenticatedAndHasModeratorRole() throws Exception {
    this.mockMvc
      .perform(delete("/api/books/{isbn}/reviews/{reviewId}", 42, 74)
        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_moderator"))))
      .andExpect(status().isOk());

    verify(reviewService, times(1)).deleteReview("42", 74L);
  }
}
