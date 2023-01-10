package de.rieckpil.courses.book.management;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookSynchronizationListenerTest {

  private static final String VALID_ISBN = "1234567891234";
  @Mock
  private BookRepository bookRepository;
  @Mock
  private OpenLibraryApiClient openLibraryApiClient;
  @Captor
  private ArgumentCaptor<Book> argumentCaptor;
  @InjectMocks
  private BookSynchronizationListener bookSynchronizationListener;

  @Test
  void shouldRejectBookWhenIsbnIsMalformed() {
    BookSynchronization bookSynchronization = new BookSynchronization("43");

    bookSynchronizationListener.consumeBookUpdates(bookSynchronization);

    verifyNoInteractions(openLibraryApiClient, bookRepository);
  }

  @Test
  void shouldNotOverrideWhenBookAlreadyExists() {
    BookSynchronization bookSynchronization = new BookSynchronization(VALID_ISBN);
    when(bookRepository.findByIsbn(VALID_ISBN)).thenReturn(new Book());

    bookSynchronizationListener.consumeBookUpdates(bookSynchronization);

    verifyNoInteractions(openLibraryApiClient);
    verify(bookRepository, times(0)).save(ArgumentMatchers.any());
  }

  @Test
  void shouldThrowExceptionWhenProcessingFails() {
    BookSynchronization bookSynchronization = new BookSynchronization(VALID_ISBN);
    when(bookRepository.findByIsbn(VALID_ISBN)).thenReturn(null);
    when(openLibraryApiClient.fetchMetadataForBook(VALID_ISBN)).thenThrow(new RuntimeException("Network timeout"));

    assertThrows(RuntimeException.class, () -> bookSynchronizationListener.consumeBookUpdates(bookSynchronization));

  }

  @Test
  void shouldStoreBookWhenNewAndCorrectIsbn() {
    // Arrange
    BookSynchronization bookSynchronization = new BookSynchronization(VALID_ISBN);
    when(bookRepository.findByIsbn(VALID_ISBN)).thenReturn(null);
    Book book = new Book();
    book.setPages(500L);
    book.setTitle("A Promised Land");
    book.setAuthor("Barack Obama");
    when(openLibraryApiClient.fetchMetadataForBook(VALID_ISBN)).thenReturn(book);
    when(bookRepository.save(book)).then(invocation -> {
      Book mockBook = invocation.getArgument(0);
      mockBook.setId(1L);
      return mockBook;
    });

    // Act
    bookSynchronizationListener.consumeBookUpdates(bookSynchronization);

    // Assert
    verify(bookRepository).save(argumentCaptor.capture());
    assertEquals("Barack Obama", argumentCaptor.getValue().getAuthor());
    assertEquals(1L, argumentCaptor.getValue().getId());
  }
}
