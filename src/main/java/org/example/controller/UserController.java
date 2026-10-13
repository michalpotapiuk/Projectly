package org.example.controller;

import jakarta.validation.Valid;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.example.model.dto.UserForm;
import org.example.model.enums.Role;
import org.example.exception.BusinessException;
import org.example.service.UserService;

@Controller
@RequestMapping("/users")
public class UserController {
    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", users.getAllUsers());
        return "users/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new UserForm());
        return form(model, null);
    }

    @PostMapping
    public String create(
            @org.springframework.validation.annotation.Validated(UserForm.Create.class)
                    @ModelAttribute("form")
                    UserForm form,
            BindingResult errors,
            Model model) {
        if (errors.hasErrors()) {
            form.setPassword(null);
            return form(model, null);
        }
        try {
            users.createUser(form);
            return "redirect:/users";
        } catch (BusinessException exception) {
            FormErrors.reject(errors, exception);
        } catch (DataIntegrityViolationException exception) {
            errors.reject("duplicate", "Login lub email jest już zajęty.");
        }
        form.setPassword(null);
        return form(model, null);
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        var user = users.getUserById(id);
        var form = new UserForm();
        form.setUsername(user.getUsername());
        form.setEmail(user.getEmail());
        form.setFirstName(user.getFirstName());
        form.setLastName(user.getLastName());
        form.setRole(user.getRole());
        form.setEnabled(user.isEnabled());
        model.addAttribute("form", form);
        return form(model, id);
    }

    @PostMapping("/{id}/edit")
    public String edit(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") UserForm form,
            BindingResult errors,
            Model model) {
        if (errors.hasErrors()) return form(model, id);
        try {
            users.updateUser(id, form);
            return "redirect:/users";
        } catch (BusinessException exception) {
            FormErrors.reject(errors, exception);
        } catch (DataIntegrityViolationException exception) {
            errors.reject("duplicate", "Login lub email jest już zajęty.");
        }
        return form(model, id);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            users.deleteUser(id);
            redirect.addFlashAttribute(
                    "success", "Usunięto konto lub zablokowano je, jeżeli ma powiązania.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/users";
    }

    private String form(Model model, Long id) {
        model.addAttribute("userId", id);
        model.addAttribute("roles", Role.values());
        return "users/form";
    }
}
