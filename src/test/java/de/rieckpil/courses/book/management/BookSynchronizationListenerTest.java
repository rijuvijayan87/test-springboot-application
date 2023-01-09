package de.rieckpil.courses.book.management;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookSynchronizationListenerTest {

  @Mock
  private BookRepository mockedBookRepository;
  @Mock
  private OpenLibraryApiClient mockedOpenLibraryApiClient;

  @InjectMocks
  private BookSynchronizationListener mockedBookSynchronizationListener;
  @Test
  void shouldRejectBookWhenIsbnIsMalformed() {
    System.out.println(mockedBookSynchronizationListener.toString());
  }

  @Test
  void shouldNotOverrideWhenBookAlreadyExists() {
  }

  @Test
  void shouldThrowExceptionWhenProcessingFails() {
  }

  @Test
  void shouldStoreBookWhenNewAndCorrectIsbn() {
  }

}
