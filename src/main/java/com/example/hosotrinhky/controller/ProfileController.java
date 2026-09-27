package com.example.hosotrinhky.controller;

import com.example.hosotrinhky.model.User;
import com.example.hosotrinhky.service.CurrentUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProfileController {

    private final CurrentUserService currentUserService;

    public ProfileController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        User user = currentUserService.get();

        model.addAttribute("user", user);
        model.addAttribute("activeMenu", "profile");

        return "profile";
    }
}