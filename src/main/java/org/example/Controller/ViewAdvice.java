package org.example.Controller;

import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.example.Model.enums.Role;
import org.example.repository.UserRepository;

@ControllerAdvice
public class ViewAdvice {
    private final UserRepository users;

    public ViewAdvice(UserRepository users) {
        this.users = users;
    }

    @ModelAttribute
    public void commonAttributes(Model model, Authentication authentication) {
        boolean admin =
                authentication != null
                        && users.findByUsername(authentication.getName())
                                .filter(user -> user.isEnabled() && user.getRole() == Role.ADMIN)
                                .isPresent();
        model.addAttribute("isAdmin", admin);
        model.addAttribute(
                "loggedUsername", authentication == null ? "" : authentication.getName());
    }
}
