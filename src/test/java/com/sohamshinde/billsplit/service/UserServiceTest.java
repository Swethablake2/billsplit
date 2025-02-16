package com.sohamshinde.billsplit.service;

import com.sohamshinde.billsplit.dto.UserRegistrationDto;
import com.sohamshinde.billsplit.dto.UserUpdateDto;
import com.sohamshinde.billsplit.entity.User;
import com.sohamshinde.billsplit.entity.VerificationToken;
import com.sohamshinde.billsplit.enums.Role;
import com.sohamshinde.billsplit.exceptions.InvalidTokenException;
import com.sohamshinde.billsplit.exceptions.ResourceNotFoundException;
import com.sohamshinde.billsplit.exceptions.UserAlreadyExistsException;
import com.sohamshinde.billsplit.exceptions.UserNotFoundException;
import com.sohamshinde.billsplit.repository.UserRepository;
import com.sohamshinde.billsplit.repository.VerificationTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @Mock
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        ReflectionTestUtils.setField(userService, "mailersendAPIKey", "test-api-key");

        when(passwordEncoder.encode(anyString())).thenReturn("mockedEncodedPassword");

        // Set up a test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("John Doe");
        testUser.setEmail("johndoe@example.com");
        testUser.setPhone("1234567890");
        testUser.setPassword(passwordEncoder.encode("StrongPassword123"));
        testUser.setGender(true);
        testUser.setEnabled(false);
        testUser.setRole(Role.USER);

        when(userRepository.findByEmail("johndoe@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    }

    /**
     * ✅ Test: Register a New User (Happy Path)
     */
    @Test
    void testRegisterUserSuccess() {
        UserRegistrationDto userDto = new UserRegistrationDto("Alice", "alice@example.com", "9876543210", "StrongPass123", true);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());

        userService.registerUser(userDto);

        verify(userRepository, times(1)).save(any(User.class));
        verify(verificationTokenRepository, times(1)).save(any(VerificationToken.class));
    }

    /**
     * ❌ Test: Register with Existing Email
     */
    @Test
    void testRegisterWithExistingEmail() {
        UserRegistrationDto userDto = new UserRegistrationDto("John Doe", "johndoe@example.com", "1234567890", "StrongPass123", true);

        assertThrows(UserAlreadyExistsException.class, () -> userService.registerUser(userDto));
    }

    @Test
    void testVerifyEmailWithValidToken() {
        // Arrange
        VerificationToken token = new VerificationToken("validToken", testUser, LocalDateTime.now().plusHours(24));
        when(verificationTokenRepository.findByToken("validToken")).thenReturn(token);

        // Act
        userService.verifyEmail("validToken");

        // Capture the saved user
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        // Assert
        assertTrue(userCaptor.getValue().getEnabled()); // Check if user was enabled
        verify(verificationTokenRepository, times(1)).delete(token);
    }


    /**
     * ❌ Test: Verify Email with Invalid Token
     */
    @Test
    void testVerifyEmailWithInvalidToken() {
        when(verificationTokenRepository.findByToken("invalidToken")).thenReturn(null);

        assertThrows(InvalidTokenException.class, () -> userService.verifyEmail("invalidToken"));
    }

    /**
     * ✅ Test: Fetch User by ID (Existing User)
     */
    @Test
    void testFetchUserById() {
        User fetchedUser = userService.fetchUserById(1L);
        assertNotNull(fetchedUser);
        assertEquals("John Doe", fetchedUser.getName());
    }

    /**
     * ✅ Test: Fetch User by Invalid ID (Expect Exception)
     */
    @Test
    void testFetchUserByInvalidId() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(UserNotFoundException.class, () -> userService.fetchUserById(99L));

        assertEquals("User with ID 99 not found", exception.getMessage());
    }

    /**
     * ✅ Test: Get Authenticated User Details
     */
    @Test
    void testGetAuthenticatedUser() {
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(new UsernamePasswordAuthenticationToken(testUser, null));
        when(userRepository.findByEmail("johndoe@example.com")).thenReturn(Optional.of(testUser));

        User authenticatedUser = userService.getAuthenticatedUser();
        assertNotNull(authenticatedUser);
        assertEquals("John Doe", authenticatedUser.getName());
    }

    /**
     * ❌ Test: Get Authenticated User Details (Not Logged In)
     */
    @Test
    void testGetAuthenticatedUserNotLoggedIn() {
        SecurityContextHolder.clearContext();

        assertThrows(ResourceNotFoundException.class, () -> userService.getAuthenticatedUser());
    }

    /**
     * ✅ Test: User Updates Their Own Profile
     */
    @Test
    void testUpdateOwnProfile() {
        // Create UserUpdateDto instead of using User directly
        UserUpdateDto userUpdateDto = new UserUpdateDto("John Updated", "9876543210", true);

        // Mock user retrieval from repository
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Call update method
        userService.updateUser(testUser.getId(), userUpdateDto);

        // Assert that the user object is updated correctly
        assertEquals("John Updated", testUser.getName());
        assertEquals("9876543210", testUser.getPhone());
        assertEquals(true, testUser.getGender());
    }

    /**
     * ✅ Test: User Deactivates Their Own Account
     */
    @Test
    void testDeactivateOwnAccount() {
        userService.deactivateUser(testUser.getId());

        assertFalse(testUser.isActive());
        verify(userRepository, times(1)).save(testUser);
    }

}
