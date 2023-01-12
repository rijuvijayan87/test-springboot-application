package de.rieckpil.courses.book.management;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  private final String EMAIL = "email.spring.io";
  private String USERNAME = "user_name";
  @Mock
  private UserRepository mockedUserRepository;

  @InjectMocks
  private UserService cut;

  @Test
  public void shouldReturnUserIfAlreadyExists() {
    when(mockedUserRepository.findByNameAndEmail(USERNAME, EMAIL))
      .thenReturn(new User());

    cut.getOrCreateUser(USERNAME, EMAIL);

    verify(mockedUserRepository, times(0)).save(ArgumentMatchers.any(User.class));
  }

  @Test
  public void shouldCreateUserIfNotExists() {
    when(mockedUserRepository.findByNameAndEmail(USERNAME, EMAIL))
      .thenReturn(null);

    cut.getOrCreateUser(USERNAME, EMAIL);

    verify(mockedUserRepository, times(1)).save(ArgumentMatchers.any(User.class));
  }

}
