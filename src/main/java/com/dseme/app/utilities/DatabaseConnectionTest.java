package com.dseme.app.utilities;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class DatabaseConnectionTest implements CommandLineRunner {
    
    private final DataSource dataSource;
    
    public DatabaseConnectionTest(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Override
    public void run(String... args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            System.out.println("✅ DATABASE CONNECTION SUCCESSFUL!");
            System.out.println("Database: " + connection.getMetaData().getDatabaseProductName());
            System.out.println("URL: " + connection.getMetaData().getURL());
        } catch (Exception e) {
            System.err.println("❌ DATABASE CONNECTION FAILED!");
            System.err.println("Error: " + e.getMessage());
        }
    }
}
