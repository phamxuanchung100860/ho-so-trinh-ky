package com.example.hosotrinhky.service;

import com.example.hosotrinhky.model.User;
import com.example.hosotrinhky.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

private final UserRepository repo;

public CurrentUserService(UserRepository repo) {
    this.repo = repo;
}

public User get() {

    String username =
            SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getName();

    User user = repo.findByUsername(username)
            .orElseThrow();

    System.out.println(
            "========== CURRENT USER =========="
    );

    System.out.println(
            "Username: " + username
    );

    System.out.println(
            "User ID: " + user.getId()
    );

    System.out.println(
            "Full name: " + user.getFullName()
    );

    System.out.println(
            "Role: " + user.getRole()
    );

    System.out.println(
            "=================================="
    );

    return user;
}

}
