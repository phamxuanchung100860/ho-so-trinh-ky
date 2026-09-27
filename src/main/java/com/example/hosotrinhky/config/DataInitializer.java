package com.example.hosotrinhky.config;

import com.example.hosotrinhky.model.Role;
import com.example.hosotrinhky.model.User;
import com.example.hosotrinhky.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner init(UserRepository repo, PasswordEncoder encoder) {
        return args -> {
            ensure(repo, encoder, "admin", "123456", "Quản trị viên", "admin@gmail.com", "Phòng Hành chính", "Quản trị hệ thống", Role.ADMIN);
            ensure(repo, encoder, "nguyenana", "123456", "Nguyễn Văn A", "nguyenvana@gmail.com", "Phòng Hành chính", "Người lập hồ sơ", Role.CREATOR);
            ensure(repo, encoder, "tranthib", "123456", "Trần Thị B", "tranthib@gmail.com", "Phòng Tài chính", "Trưởng phòng", Role.APPROVER);
            ensure(repo, encoder, "levanc", "123456", "Lê Văn C", "levanc@gmail.com", "Ban Giám đốc", "Phó giám đốc", Role.APPROVER);
            ensure(repo, encoder, "nguyenvand", "123456", "Nguyễn Văn D", "nguyenvand@gmail.com", "Ban Giám đốc", "Giám đốc", Role.APPROVER);
        };
    }

    private void ensure(UserRepository repo, PasswordEncoder encoder, String username, String rawPassword,
                        String fullName, String email, String department, String position, Role role) {
        User u = repo.findByUsername(username).orElseGet(() -> User.builder().username(username).createdAt(LocalDateTime.now()).build());
        u.setPasswordHash(isBCrypt(u.getPasswordHash()) ? u.getPasswordHash() : encoder.encode(rawPassword));
        u.setFullName(fullName); u.setEmail(email); u.setDepartment(department); u.setPosition(position);
        u.setRole(role); u.setActive(true); repo.save(u);
    }

    private boolean isBCrypt(String p) {
        return p != null && (p.startsWith("$2a$") || p.startsWith("$2b$") || p.startsWith("$2y$")) && p.length() == 60;
    }
}
