package com.anygroup.splitfair.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/")
    public String home() {
        return "SplitFair is running 🚀";
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
