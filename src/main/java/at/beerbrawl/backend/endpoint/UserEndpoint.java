/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint;

import at.beerbrawl.backend.endpoint.dto.UserDetailDto;
import at.beerbrawl.backend.endpoint.dto.UserLoginDto;
import at.beerbrawl.backend.entity.Tournament;
import at.beerbrawl.backend.exception.UserAlreadyExistsException;
import at.beerbrawl.backend.service.TestDataService;
import at.beerbrawl.backend.service.TournamentService;
import at.beerbrawl.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping(value = UserEndpoint.BASE_ENDPOINT)
public class UserEndpoint {

    public static final String BASE_ENDPOINT = "/api/v1/user";
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final UserService userService;
    private final TournamentService tournamentService;
    private final TestDataService testDataService;

    /**
     * Registers a new user in the system. If a user with the same username already exists,
     * 409 CONFLICT is returned.
     *
     * @param userRegisterDto user information to create the new user from
     * @return JWT if registration is successful
     * @throws URISyntaxException Never happen, as the URI is constructed from a known-good string
     */
    @PermitAll
    @PostMapping("register")
    public ResponseEntity<?> register(@RequestBody UserLoginDto userRegisterDto)
        throws URISyntaxException {
        LOG.info("POST {}", BASE_ENDPOINT);
        LOG.debug("request body: {}", userRegisterDto);

        try {
            var user = userService.register(userRegisterDto);
            var uri = new URI("%s/%s".formatted(BASE_ENDPOINT, user.getId()));
            var jwt = userService.login(userRegisterDto);
            return ResponseEntity.created(uri).body(jwt);
        } catch (UserAlreadyExistsException e) {
            HttpStatus status = HttpStatus.CONFLICT;
            logClientError(status, "User already exists", e);
            return new ResponseEntity<>(e.getMessage(), status);
        }
    }

    /**
     * Remove a registered user for the system.
     * Returns 204 for successful deletion.
     *
     * @param username to identify the suer to delete
     */
    @DeleteMapping("{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Delete user and all data belonging to them(Tournaments, Teams, etc) from the database.",
        security = @SecurityRequirement(name = "apiKey")
    )
    public ResponseEntity<?> delete(
        @PathVariable(value = "username") String username,
        Authentication authentication
    ) {
        LOG.info("DELETE {}/{}", BASE_ENDPOINT, username);
        if (!username.equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("Username does not match.");
        }

        userService.deleteUser(username);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Update password and username for a registered user.
     *
     * @param username target user to update
     * @param userUpdate new information of the user to update
     */
    @PutMapping("{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> update(
        @PathVariable(value = "username") String username,
        @Valid @RequestBody UserLoginDto userUpdate,
        Authentication authentication
    ) {
        LOG.info("PUT {}/{}", BASE_ENDPOINT, username);

        if (!username.equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("Username does not match.");
        }
        try {
            userService.updateUser(userUpdate, username);
        } catch (UserAlreadyExistsException e) {
            HttpStatus status = HttpStatus.CONFLICT;
            logClientError(status, "User already exists", e);
            return new ResponseEntity<>(e.getMessage(), status);
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Get Username and password for user.
     * The target user is retrieved through the JWT
     *
     * @param username target user to extract more information
     */
    @GetMapping(value = "{username}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get detailed information about user and their tournaments.",
        security = @SecurityRequirement(name = "apiKey")
    )
    public ResponseEntity<UserDetailDto> details(
        @PathVariable(value = "username") String username,
        Authentication authentication
    ) {
        LOG.info("GET {}/{}", BASE_ENDPOINT, username);

        if (!username.equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("Username does not match.");
        }

        var user = userService.findApplicationUserByUsername(username);
        var tournaments = tournamentService.findAllByOrganizer(username);
        var tournamentNames = tournaments.stream().map(Tournament::getName).toList();

        if (user != null) {
            var dto = UserDetailDto.builder()
                .username(user.getUsername())
                .tournaments(tournamentNames)
                .notStartedTournaments(
                    tournamentService.countNotStartedTournaments(user.getUsername())
                )
                .startedTournaments(tournamentService.countStartedTournaments(user.getUsername()))
                .build();
            return ResponseEntity.ok(dto);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Get Username and password for user.
     * The target user is retrieved through the JWT
     *r
     */
    @GetMapping(value = "genTestData", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get detailed information about user and their tournaments.",
        security = @SecurityRequirement(name = "apiKey")
    )
    public void generateTestData(Authentication authentication) {
        this.testDataService.generateTestDataForUser(authentication.getName());
    }

    private void logClientError(HttpStatus status, String message, Exception e) {
        LOG.warn(
            "{} {}: {}: {}",
            status.value(),
            message,
            e.getClass().getSimpleName(),
            e.getMessage()
        );
    }
}
