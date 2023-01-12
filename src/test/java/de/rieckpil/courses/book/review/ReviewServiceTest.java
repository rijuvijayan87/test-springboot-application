package de.rieckpil.courses.book.review;

import de.rieckpil.courses.book.management.Book;
import de.rieckpil.courses.book.management.BookRepository;
import de.rieckpil.courses.book.management.User;
import de.rieckpil.courses.book.management.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

  @Mock
  private ReviewVerifier mockedReviewVerifier;

  @Mock
  private UserService mockedUserService;

  @Mock
  private BookRepository mockedBookRepository;

  @Mock
  private ReviewRepository mockedReviewRepository;

  @InjectMocks
  private ReviewService cut;

  @Captor
  private ArgumentCaptor<Review> argumentCaptor;

  private static final String EMAIL = "duke@spring.io";
  private static final String USERNAME = "duke";
  private static final String ISBN = "42";

  @Test
  void shouldNotBeNull() {
    assertNotNull(mockedReviewRepository);
    assertNotNull(mockedReviewVerifier);
    assertNotNull(mockedUserService);
    assertNotNull(mockedBookRepository);
    assertNotNull(cut);

  }

  @Test
  @DisplayName("should throw exception when review book isbn doesnot exist")
  void shouldThrowExceptionWhenReviewedBookIsNotExisting() {
    when(mockedBookRepository.findByIsbn(ISBN)).thenReturn(null);

    assertThrows(IllegalArgumentException.class, () -> cut.createBookReview(ISBN, any(BookReviewRequest.class), USERNAME, EMAIL));
  }

  @Test
  void shouldRejectReviewWhenReviewQualityIsBad() {
    BookReviewRequest bookReviewRequest = new BookReviewRequest(
      "review title", "Bad review", 2
    );

    when(mockedBookRepository.findByIsbn(ISBN)).thenReturn(new Book());
    when(mockedReviewVerifier.doesMeetQualityStandards(bookReviewRequest.getReviewContent())).thenReturn(false);

    assertThrows(BadReviewQualityException.class, () -> cut.createBookReview(ISBN, bookReviewRequest, USERNAME, EMAIL));

    verify(mockedReviewRepository, times(0)).save(ArgumentMatchers.any(Review.class));
  }

  @Test
  void shouldStoreReviewWhenReviewQualityIsGoodAndBookIsPresent() {
    BookReviewRequest bookReviewRequest = new BookReviewRequest(
      "review title", "good review", 2
    );

    when(mockedBookRepository.findByIsbn(ISBN)).thenReturn(new Book());
    when(mockedReviewVerifier.doesMeetQualityStandards(bookReviewRequest.getReviewContent())).thenReturn(true);
    when(mockedUserService.getOrCreateUser(USERNAME, EMAIL)).thenReturn(new User());
    when(mockedReviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
      Review review = invocation.getArgument(0);
      review.setId(101L);
      return review;
    });

    Long bookId = cut.createBookReview(ISBN, bookReviewRequest, USERNAME, EMAIL);

    verify(mockedReviewRepository).save(argumentCaptor.capture());
    assertEquals(101L, bookId);
    assertEquals(101, argumentCaptor.getValue().getId());
    assertEquals("review title", argumentCaptor.getValue().getTitle());
    assertEquals("good review", argumentCaptor.getValue().getContent());

  }
}
