package org.example.nursingtrainingbackend.modules.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {
    
    @GetMapping("/generate-password/{password}")
    public String generatePassword(@PathVariable String password) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(password);
        return "Password: " + password + "\nHash: " + hash + "\nVerified: " + encoder.matches(password, hash);
    }
}
