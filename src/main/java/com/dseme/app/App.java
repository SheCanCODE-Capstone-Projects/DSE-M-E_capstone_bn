package com.dseme.app;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class App {
    public static void main(String[] args) {
        // Load .env file
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        // Set all environment properties for Spring Boot
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });

        SpringApplication.run(App.class, args);
    }
}
