package com.example.miniboard.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.miniboard.dto.SignupRequest;
import com.example.miniboard.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("signupRequest", new SignupRequest());
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute SignupRequest signupRequest,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "auth/signup"; // 400 성격: 폼 다시 보여줌
        }
        try {
            userService.signup(signupRequest);
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("username", "duplicate", e.getMessage());
            return "auth/signup";
        }
        return "redirect:/login"; // ★ PRG (3단계)
    }

    @GetMapping("/login")
    public String loginForm() {
        return "auth/login";
    }
    // POST /login, POST /logout 컨트롤러가 없다 → Security가 처리 (3단계 표 그대로)
}