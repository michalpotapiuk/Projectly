package org.example;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import org.example.model.dto.*;
import org.example.model.entity.*;
import org.example.model.enums.*;
import org.example.exception.*;
import org.example.repository.*;
import org.example.service.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "admin", roles = "ADMIN")
class UserServiceTest {
    @Autowired UserService service;
    @Autowired UserRepository users;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;

    private UserForm form() {
        var form = new UserForm();
        form.setUsername("Student");
        form.setEmail("Student@Example.org");
        form.setPassword("Secret123!");
        form.setFirstName("Test");
        form.setLastName("Student");
        return form;
    }

    @Test
    void createsUserWithHashedPasswordAndNormalizedIdentifiers() {
        User user = service.createUser(form());
        assertThat(user.getUsername()).isEqualTo("student");
        assertThat(user.getEmail()).isEqualTo("student@example.org");
        assertThat(user.getPassword()).isNotEqualTo("Secret123!");
        assertThat(encoder.matches("Secret123!", user.getPassword())).isTrue();
    }

    @Test
    void rejectsDuplicateUsername() {
        var form = form();
        form.setUsername("ADMIN");
        assertThat(users.findByUsername("admin")).isPresent();
        assertThatThrownBy(() -> service.createUser(form)).isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsDuplicateEmail() {
        var form = form();
        form.setEmail("ANNA@PROJECTLY.EXAMPLE");
        assertThat(users.findByEmail("anna@projectly.example")).isPresent();
        assertThatThrownBy(() -> service.createUser(form)).isInstanceOf(BusinessException.class);
    }

    @Test
    void editingDoesNotReplacePassword() {
        User user = service.createUser(form());
        String encoded = user.getPassword();
        var edited = form();
        edited.setUsername("renamed");
        edited.setPassword(null);
        service.updateUser(user.getId(), edited);
        assertThat(users.findById(user.getId()).orElseThrow().getPassword()).isEqualTo(encoded);
    }

    @Test
    void disablesUserWithRelations() {
        User anna = users.findByUsername("anna").orElseThrow();
        service.deleteUser(anna.getId());
        assertThat(users.findById(anna.getId()).orElseThrow().isEnabled()).isFalse();
    }

    @Test
    void deletesUserWithoutRelations() {
        User user = service.createUser(form());
        service.deleteUser(user.getId());
        assertThat(users.findById(user.getId())).isEmpty();
    }

    @Test
    void protectsLastActiveAdmin() {
        Long adminId = users.findByUsername("admin").orElseThrow().getId();
        assertThatThrownBy(() -> service.deleteUser(adminId)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.changeRole(adminId, Role.USER))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @WithMockUser(username = "anna")
    void regularUserCannotManageUsers() {
        assertThatThrownBy(() -> service.createUser(form()))
                .isInstanceOf(AccessDeniedException.class);
    }
}
