package org.example.config;

import org.example.model.entity.Project;
import org.example.model.entity.ProjectMember;
import org.example.model.entity.Task;
import org.example.model.entity.User;
import org.example.model.enums.Priority;
import org.example.model.enums.Role;
import org.example.model.enums.TaskStatus;
import org.example.repository.ProjectMemberRepository;
import org.example.repository.ProjectRepository;
import org.example.repository.TaskRepository;
import org.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@ConditionalOnProperty(name = "projectly.seed.enabled", havingValue = "true")
public class DataInitializer implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    private final UserRepository users;
    private final ProjectRepository projects;
    private final ProjectMemberRepository members;
    private final TaskRepository tasks;
    private final PasswordEncoder encoder;
    private final String password;
    private final String resetUser;

    public DataInitializer(
            UserRepository users,
            ProjectRepository projects,
            ProjectMemberRepository members,
            TaskRepository tasks,
            PasswordEncoder encoder,
            @Value("${projectly.seed.password}") String password,
            @Value("${projectly.seed.reset-user:}") String resetUser) {
        this.users = users;
        this.projects = projects;
        this.members = members;
        this.tasks = tasks;
        this.encoder = encoder;
        this.password = password;
        this.resetUser = resetUser;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!resetUser.isBlank()) {
            resetPassword();
            return;
        }
        if (users.count() != 0
                || projects.count() != 0
                || members.count() != 0
                || tasks.count() != 0) return;
        validatePassword();
        User admin = user("admin", "Administrator", "Projectly", Role.ADMIN);
        User anna = user("anna", "Anna", "Kowalska", Role.USER);
        User jan = user("jan", "Jan", "Nowak", Role.USER);
        Project first =
                project(
                        "Strona koła naukowego",
                        "Przygotowanie prostej strony i treści dla studentów.",
                        anna);
        Project second =
                project(
                        "Organizacja warsztatów",
                        "Planowanie wydarzenia i podział obowiązków.",
                        admin);
        member(first, jan);
        member(second, anna);
        member(second, jan);
        task(first, "Przygotować makietę strony", anna, TaskStatus.TODO, Priority.HIGH);
        task(first, "Napisać opis koła", jan, TaskStatus.IN_PROGRESS, Priority.MEDIUM);
        task(second, "Zarezerwować salę", anna, TaskStatus.DONE, Priority.HIGH);
        task(second, "Przygotować materiały", jan, TaskStatus.TODO, Priority.LOW);
    }

    private void resetPassword() {
        validatePassword();
        String username = resetUser.trim().toLowerCase(Locale.ROOT);
        User user = users.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException(
                        "Nie istnieje użytkownik do resetu hasła: " + username));
        user.setPassword(encoder.encode(password));
        users.saveAndFlush(user);
        logger.warn("Zresetowano hasło konta '{}'. Usuń SEED_RESET_USER po wykonaniu resetu.", username);
    }

    private void validatePassword() {
        if (password.length() < 6
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new IllegalStateException(
                    "Ustaw SEED_PASSWORD: minimum 6 znaków, maksymalnie 72 bajty UTF-8.");
    }

    private User user(String username, String firstName, String lastName, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@projectly.example");
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        user.setEnabled(true);
        user.setPassword(encoder.encode(password));
        return users.save(user);
    }

    private Project project(String name, String description, User owner) {
        Project project = new Project();
        project.setName(name);
        project.setDescription(description);
        project.setOwner(owner);
        return projects.save(project);
    }

    private void member(Project project, User user) {
        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(user);
        members.save(member);
    }

    private void task(
            Project project, String title, User user, TaskStatus status, Priority priority) {
        Task task = new Task();
        task.setProject(project);
        task.setTitle(title);
        task.setAssignedUser(user);
        task.setStatus(status);
        task.setPriority(priority);
        task.setDeadline(LocalDate.now().plusDays(7));
        tasks.save(task);
    }
}
