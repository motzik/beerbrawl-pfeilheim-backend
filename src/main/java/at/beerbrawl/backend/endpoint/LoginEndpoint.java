/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint;

import at.beerbrawl.backend.endpoint.dto.UserLoginDto;
import at.beerbrawl.backend.service.UserService;
import jakarta.annotation.security.PermitAll;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = LoginEndpoint.BASE_ENDPOINT)
public class LoginEndpoint {

    public static final String BASE_ENDPOINT = "/api/v1/authentication";
    private static final Logger LOGGER = LoggerFactory.getLogger(
        MethodHandles.lookup().lookupClass()
    );

    private final UserService userService;

    public LoginEndpoint(UserService userService) {
        this.userService = userService;
    }

    @PermitAll
    @PostMapping
    public String login(@RequestBody UserLoginDto userLoginDto) {
        LOGGER.info("POST {}", BASE_ENDPOINT);
        LOGGER.debug("request body: {}", userLoginDto);
        return userService.login(userLoginDto);
    }
}
