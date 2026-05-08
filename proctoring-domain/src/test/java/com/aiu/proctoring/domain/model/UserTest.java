package com.aiu.proctoring.domain.model;

import com.aiu.proctoring.domain.value.UserId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void createUser_validData_shouldCreate() {
        User user = User.builder()
            .id(UserId.generate())
            .email("test@example.com")
            .username("testuser")
            .passwordHash("hashed")
            .firstName("John")
            .lastName("Doe")
            .role(User.Role.STUDENT)
            .build();

        assertNotNull(user.getId());
        assertEquals("test@example.com", user.getEmail());
        assertTrue(user.isActive());
    }

    @Test
    void activate_deactivate_shouldToggle() {
        User user = User.builder()
            .id(UserId.generate())
            .email("test@example.com")
            .username("testuser")
            .passwordHash("hashed")
            .firstName("Test")
            .lastName("User")
            .role(User.Role.PROCTOR)
            .active(false)
            .build();

        assertFalse(user.isActive());
        user.activate();
        assertTrue(user.isActive());
        user.deactivate();
        assertFalse(user.isActive());
    }

    @Test
    void anonymize_shouldRemovePersonalData() {
        User user = User.builder()
            .id(UserId.from("123e4567-e89b-12d3-a456-426614174000"))
            .email("real@example.com")
            .username("realuser")
            .firstName("Real")
            .lastName("Name")
            .phoneNumber("+77001234567")
            .role(User.Role.STUDENT)
            .build();

        user.anonymize();

        assertTrue(user.getEmail().contains("deleted.com"));
        assertTrue(user.getUsername().contains("deleted_user"));
        assertEquals("Deleted", user.getFirstName());
        assertNull(user.getPhoneNumber());
        assertFalse(user.isActive());
    }
}
