package org.example;

import org.example.config.DataInitializer;
import org.example.repository.ProjectMemberRepository;
import org.example.repository.ProjectRepository;
import org.example.repository.TaskRepository;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DataInitializerTest {
    @Autowired UserRepository users;
    @Autowired ProjectRepository projects;
    @Autowired ProjectMemberRepository members;
    @Autowired TaskRepository tasks;
    @Autowired PasswordEncoder encoder;

    private DataInitializer initializer(String password, String username) {
        return new DataInitializer(users, projects, members, tasks, encoder, password, username);
    }

    @Test
    void resetsOnlySelectedPasswordWithoutChangingRelatedData() {
        var anna = users.findByUsername("anna").orElseThrow();
        anna.setEnabled(false);
        var role = anna.getRole();
        String adminPassword = users.findByUsername("admin").orElseThrow().getPassword();
        String janPassword = users.findByUsername("jan").orElseThrow().getPassword();
        long userCount = users.count();
        long projectCount = projects.count();
        long memberCount = members.count();
        long taskCount = tasks.count();

        initializer("NewPassword123!", " ANNA ").run();

        assertThat(encoder.matches("NewPassword123!", anna.getPassword())).isTrue();
        assertThat(anna.isEnabled()).isFalse();
        assertThat(anna.getRole()).isEqualTo(role);
        assertThat(users.findByUsername("admin").orElseThrow().getPassword()).isEqualTo(adminPassword);
        assertThat(users.findByUsername("jan").orElseThrow().getPassword()).isEqualTo(janPassword);
        assertThat(users.count()).isEqualTo(userCount);
        assertThat(projects.count()).isEqualTo(projectCount);
        assertThat(members.count()).isEqualTo(memberCount);
        assertThat(tasks.count()).isEqualTo(taskCount);
    }

    @Test
    void defaultStartupDoesNotReplaceExistingPassword() {
        String previous = users.findByUsername("admin").orElseThrow().getPassword();
        initializer("DifferentPassword123!", "").run();
        assertThat(users.findByUsername("admin").orElseThrow().getPassword()).isEqualTo(previous);
    }

    @Test
    void rejectsResetForMissingUserWithoutCreatingAccount() {
        long previousCount = users.count();
        assertThatThrownBy(() -> initializer("NewPassword123!", "missing").run())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Nie istnieje użytkownik");
        assertThat(users.count()).isEqualTo(previousCount);
    }

    @Test
    void rejectsShortPasswordWithoutChangingStoredPassword() {
        String previous = users.findByUsername("admin").orElseThrow().getPassword();
        assertThatThrownBy(() -> initializer("short", "admin").run())
                .isInstanceOf(IllegalStateException.class);
        assertThat(users.findByUsername("admin").orElseThrow().getPassword()).isEqualTo(previous);
    }

    @Test
    void rejectsPasswordExceedingBcryptByteLimit() {
        String previous = users.findByUsername("admin").orElseThrow().getPassword();
        assertThatThrownBy(() -> initializer("ą".repeat(37), "admin").run())
                .isInstanceOf(IllegalStateException.class);
        assertThat(users.findByUsername("admin").orElseThrow().getPassword()).isEqualTo(previous);
    }
}
