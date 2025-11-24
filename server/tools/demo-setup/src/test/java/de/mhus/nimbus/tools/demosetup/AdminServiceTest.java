package de.mhus.nimbus.tools.demosetup;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AdminServiceTest {

    @Test
    void parseValidAdminLine() throws IOException {
        Path temp = Files.createTempFile("admin", ".txt");
        Files.writeString(temp, "admin:secretPass\n");
        AdminService svc = new AdminService(temp.toString());
        Optional<String> pw = svc.getAdminPassword();
        assertTrue(pw.isPresent());
        assertEquals("secretPass", pw.get());
    }

    @Test
    void ignoreWrongUser() throws IOException {
        Path temp = Files.createTempFile("admin", ".txt");
        Files.writeString(temp, "bob:pw\n");
        AdminService svc = new AdminService(temp.toString());
        Optional<String> pw = svc.getAdminPassword();
        assertFalse(pw.isPresent());
    }

    @Test
    void ignoreBadFormat() throws IOException {
        Path temp = Files.createTempFile("admin", ".txt");
        Files.writeString(temp, "nocolonline\n");
        AdminService svc = new AdminService(temp.toString());
        Optional<String> pw = svc.getAdminPassword();
        assertFalse(pw.isPresent());
    }
}

