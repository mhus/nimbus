package de.mhus.nimbus.server.shared.client;

import de.mhus.nimbus.server.shared.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Client for communicating with the Identity Service.
 * Provides methods to call all Identity Service endpoints.
 */
@Component
@Slf4j
public class IdentityServiceClient {

    private final RestTemplate restTemplate;
    private final String identityServiceUrl;

    public IdentityServiceClient(RestTemplate restTemplate,
                               @Value("${nimbus.identity.service.url:http://localhost:8081}") String identityServiceUrl) {
        this.restTemplate = restTemplate;
        this.identityServiceUrl = identityServiceUrl;
    }

    /**
     * Creates a new user.
     * @param createUserDto user creation data
     * @return created user information
     */
    public UserDto createUser(CreateUserDto createUserDto) {
        String url = identityServiceUrl + "/users";
        HttpEntity<CreateUserDto> request = new HttpEntity<>(createUserDto);

        ResponseEntity<UserDto> response = restTemplate.exchange(
                url, HttpMethod.POST, request, UserDto.class);

        return response.getBody();
    }

    /**
     * Retrieves user information by ID.
     * @param userId the user ID
     * @param authToken JWT token for authentication
     * @return user information
     */
    public UserDto getUser(String userId, String authToken) {
        String url = identityServiceUrl + "/users/" + userId;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        HttpEntity<?> request = new HttpEntity<>(headers);

        ResponseEntity<UserDto> response = restTemplate.exchange(
                url, HttpMethod.GET, request, UserDto.class);

        return response.getBody();
    }

    /**
     * Updates user information.
     * @param userId the user ID
     * @param userDto updated user data
     * @param authToken JWT token for authentication
     * @return updated user information
     */
    public UserDto updateUser(String userId, UserDto userDto, String authToken) {
        String url = identityServiceUrl + "/users/" + userId;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UserDto> request = new HttpEntity<>(userDto, headers);

        ResponseEntity<UserDto> response = restTemplate.exchange(
                url, HttpMethod.PUT, request, UserDto.class);

        return response.getBody();
    }

    /**
     * Deletes a user.
     * @param userId the user ID
     * @param authToken JWT token for authentication
     */
    public void deleteUser(String userId, String authToken) {
        String url = identityServiceUrl + "/users/" + userId;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        HttpEntity<?> request = new HttpEntity<>(headers);

        restTemplate.exchange(url, HttpMethod.DELETE, request, Void.class);
    }

    /**
     * Retrieves all users.
     * @param authToken JWT token for authentication
     * @return list of all users
     */
    public List<UserDto> getAllUsers(String authToken) {
        String url = identityServiceUrl + "/users";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        HttpEntity<?> request = new HttpEntity<>(headers);

        ResponseEntity<List<UserDto>> response = restTemplate.exchange(
                url, HttpMethod.GET, request, new ParameterizedTypeReference<List<UserDto>>() {});

        return response.getBody();
    }

    /**
     * Performs user login.
     * @param loginDto login credentials
     * @return JWT token information
     */
    public TokenDto login(LoginDto loginDto) {
        String url = identityServiceUrl + "/login";
        HttpEntity<LoginDto> request = new HttpEntity<>(loginDto);

        ResponseEntity<TokenDto> response = restTemplate.exchange(
                url, HttpMethod.POST, request, TokenDto.class);

        return response.getBody();
    }

    /**
     * Renews an existing JWT token.
     * @param authToken current JWT token
     * @return new JWT token information
     */
    public TokenDto renewToken(String authToken) {
        String url = identityServiceUrl + "/token/renew";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        HttpEntity<?> request = new HttpEntity<>(headers);

        ResponseEntity<TokenDto> response = restTemplate.exchange(
                url, HttpMethod.POST, request, TokenDto.class);

        return response.getBody();
    }

    /**
     * Changes user password.
     * @param userId the user ID
     * @param changePasswordDto password change data
     * @param authToken JWT token for authentication
     */
    public void changePassword(String userId, ChangePasswordDto changePasswordDto, String authToken) {
        String url = identityServiceUrl + "/users/" + userId + "/change-password";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ChangePasswordDto> request = new HttpEntity<>(changePasswordDto, headers);

        restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
    }
}
