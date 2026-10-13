
package com.complaint.complaint_management.controller;

import com.complaint.complaint_management.entity.Student;
import com.complaint.complaint_management.repository.StudentRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final StudentRepository studentRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthController(
            StudentRepository studentRepository,
            BCryptPasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {

        String name = request.get("name");
        String email = request.get("email");
        String password = request.get("password");

        if (name == null || name.isBlank()
                || email == null || email.isBlank()
                || password == null || password.length() < 8) {
            return ResponseEntity.badRequest().body(
                    Map.of("message",
                            "Enter your name, email, and a password of at least 8 characters."));
        }

        email = email.trim().toLowerCase();

        if (studentRepository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("message", "An account with this email already exists."));
        }

        Student student = new Student();
        student.setName(name.trim());
        student.setEmail(email);
        student.setPassword(passwordEncoder.encode(password));
        student.setRole("STUDENT");

        Student saved = studentRepository.save(student);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of(
                        "message", "Registration successful.",
                        "studentId", saved.getId(),
                        "name", saved.getName(),
                        "email", saved.getEmail()
                ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {

        String email = request.get("email");
        String password = request.get("password");

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Email and password are required."));
        }

        Student student = studentRepository
                .findByEmail(email.trim().toLowerCase())
                .orElse(null);

        if (student == null
                || !passwordEncoder.matches(password, student.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("message", "Invalid email or password."));
        }

        return ResponseEntity.ok(
                Map.of(
                        "message", "Login successful.",
                        "studentId", student.getId(),
                        "name", student.getName(),
                        "email", student.getEmail(),
                        "role", student.getRole()
                ));
    }
}
