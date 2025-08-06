package de.mhus.nimbus.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class IdentityApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(IdentityApplication.class, args);
    }
}
