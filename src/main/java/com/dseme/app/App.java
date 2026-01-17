package com.dseme.app;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class App {
    public static void main(String[] args) {
        // Load .env file
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        // Set environment properties for Spring Boot
        System.setProperty("DB_URL", dotenv.get("DB_URL", ""));
        System.setProperty("DB_USERNAME", dotenv.get("DB_USERNAME", ""));
        System.setProperty("DB_PASSWORD", dotenv.get("DB_PASSWORD", ""));
        System.setProperty("JWT_SECRET", dotenv.get("JWT_SECRET", ""));
        System.setProperty("MAIL_HOST", dotenv.get("MAIL_HOST", ""));
        System.setProperty("MAIL_PORT", dotenv.get("MAIL_PORT", "587"));
        System.setProperty("MAIL_USERNAME", dotenv.get("MAIL_USERNAME", ""));
        System.setProperty("MAIL_PASSWORD", dotenv.get("MAIL_PASSWORD", ""));
        System.setProperty("MAIL_FROM", dotenv.get("MAIL_FROM", ""));
        System.setProperty("PORT", dotenv.get("PORT", "8088"));
        System.setProperty("GOOGLE_CLIENT_ID", dotenv.get("GOOGLE_CLIENT_ID", ""));
        System.setProperty("GOOGLE_CLIENT_SECRET", dotenv.get("GOOGLE_CLIENT_SECRET", ""));

        SpringApplication.run(App.class, args);
    }
}
