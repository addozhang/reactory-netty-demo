package com.atbug.demo.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class DemoServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoServerApplication.class, args);
    }

    @RestController
    public static class GreetingController {
        @GetMapping("/greeting")
        public String greeting() {
            return "Hello from demo-server!";
        }
    }
}

