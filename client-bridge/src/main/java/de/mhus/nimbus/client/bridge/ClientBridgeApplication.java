package de.mhus.nimbus.client.bridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class ClientBridgeApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(ClientBridgeApplication.class, args);
    }
}
