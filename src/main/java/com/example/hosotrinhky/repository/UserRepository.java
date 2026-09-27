package com.example.hosotrinhky.repository;
import com.example.hosotrinhky.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByUsername(String username);
    List<User> findByActiveTrueOrderByFullNameAsc();
}
