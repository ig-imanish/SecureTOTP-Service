package com.twofactorauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class TwofactorauthApplication {
	public static void main(String[] args) {
		
        try {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            dotenv.entries().forEach(e -> {
                System.setProperty(e.getKey(), e.getValue());
                System.out.println("Loaded env var: " + e.getKey());
            });
        } catch (Exception e) {
            System.err.println("Warning: Failed to load .env file. " + e.getMessage());
        }
        SpringApplication.run(TwofactorauthApplication.class, args);
	}
}
