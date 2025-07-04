package de.mhus.nimbus.identity.service;

import de.mhus.nimbus.identity.entity.User;
import de.mhus.nimbus.shared.avro.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Service für Login-Operationen
 */
@Service
public class LoginService {

    private static final Logger logger = LoggerFactory.getLogger(LoginService.class);

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public LoginService(UserService userService, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Verarbeitet eine Login-Anfrage und gibt eine Login-Response zurück
     */
    public LoginResponse processLoginRequest(LoginRequest request) {
        logger.info("Processing login request: requestId={}, username={}",
                   request.getRequestId(), request.getUsername());

        long currentTimestamp = Instant.now().toEpochMilli();

        try {
            // Finde User anhand Username oder E-Mail
            Optional<User> userOpt = findUserByUsernameOrEmail(request.getUsername());

            if (userOpt.isEmpty()) {
                logger.warn("User not found for login: {}", request.getUsername());
                return createLoginResponse(request, LoginStatus.USER_NOT_FOUND, null, null, null,
                                         currentTimestamp, "User not found");
            }

            User user = userOpt.get();

            // Prüfe ob User aktiv ist
            if (!user.getActive()) {
                logger.warn("Inactive user attempted login: {}", request.getUsername());
                return createLoginResponse(request, LoginStatus.USER_INACTIVE, null, null, null,
                                         currentTimestamp, "User account is inactive");
            }

            // Validiere Passwort
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                logger.warn("Invalid password for user: {}", request.getUsername());
                return createLoginResponse(request, LoginStatus.INVALID_CREDENTIALS, null, null, null,
                                         currentTimestamp, "Invalid credentials");
            }

            // Generiere JWT Token
            String token = jwtService.generateToken(user.getId(), user.getUsername(), user.getEmail());
            Instant expiresAt = jwtService.calculateExpirationTime();

            // Erstelle User-Info für Response
            LoginUserInfo userInfo = createLoginUserInfo(user);

            logger.info("Successful login for user: {} (ID: {})", user.getUsername(), user.getId());

            return createLoginResponse(request, LoginStatus.SUCCESS, token, expiresAt,
                                     userInfo, currentTimestamp, null);

        } catch (Exception e) {
            logger.error("Error processing login request: {}", request.getRequestId(), e);
            return createLoginResponse(request, LoginStatus.ERROR, null, null, null,
                                     currentTimestamp, "Internal error during login");
        }
    }

    /**
     * Findet einen User anhand Username oder E-Mail
     */
    private Optional<User> findUserByUsernameOrEmail(String usernameOrEmail) {
        // Versuche zuerst als Username
        Optional<User> userOpt = userService.findByUsername(usernameOrEmail);

        // Falls nicht gefunden, versuche als E-Mail
        if (userOpt.isEmpty()) {
            userOpt = userService.findByEmail(usernameOrEmail);
        }

        return userOpt;
    }

    /**
     * Erstellt LoginUserInfo aus User Entity
     */
    private LoginUserInfo createLoginUserInfo(User user) {
        return LoginUserInfo.newBuilder()
                .setId(user.getId())
                .setUsername(user.getUsername())
                .setEmail(user.getEmail())
                .setFirstName(user.getFirstName())
                .setLastName(user.getLastName())
                .build();
    }

    /**
     * Erstellt eine LoginResponse
     */
    private LoginResponse createLoginResponse(LoginRequest request, LoginStatus status,
                                            String token, Instant expiresAt, LoginUserInfo user,
                                            long timestamp, String errorMessage) {
        return LoginResponse.newBuilder()
                .setRequestId(request.getRequestId())
                .setStatus(status)
                .setToken(token)
                .setExpiresAt(expiresAt)
                .setUser(user)
                .setTimestamp(Instant.ofEpochMilli(timestamp))
                .setErrorMessage(errorMessage)
                .build();
    }

    /**
     * Validiert eine Login-Anfrage
     */
    public void validateLoginRequest(LoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Login request cannot be null");
        }

        if (request.getRequestId() == null || request.getRequestId().trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be null or empty");
        }

        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        logger.debug("Login request validation passed: {}", request.getRequestId());
    }

    /**
     * Erstellt eine Error-Response für Login-Fehler
     */
    public LoginResponse createLoginErrorResponse(LoginRequest request, String errorMessage) {
        long currentTimestamp = Instant.now().toEpochMilli();
        return createLoginResponse(request, LoginStatus.ERROR, null, null, null,
                                 currentTimestamp, errorMessage);
    }
}
