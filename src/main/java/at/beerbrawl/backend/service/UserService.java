/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.service;

import at.beerbrawl.backend.endpoint.dto.UserLoginDto;
import at.beerbrawl.backend.entity.ApplicationUser;
import at.beerbrawl.backend.exception.NotFoundException;
import at.beerbrawl.backend.exception.UserAlreadyExistsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public interface UserService extends UserDetailsService {
    /**
     * Find a user in the context of Spring Security based on the user name.
     * <br>
     * For more information have a look at this tutorial:
     * https://www.baeldung.com/spring-security-authentication-with-a-database
     *
     * @param username a user name
     * @return a Spring Security user
     * @throws UsernameNotFoundException is thrown if the specified user does not exists
     */
    @Override
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;

    /**
     * Find an application user based on the user name.
     *
     * @param username a user name
     * @return an application user
     */
    ApplicationUser findApplicationUserByUsername(String username);

    /**
     * Log in a user.
     *
     * @param userLoginDto login credentials
     * @return the JWT, if successful
     * @throws org.springframework.security.authentication.BadCredentialsException if credentials are bad
     */
    String login(UserLoginDto userLoginDto);

    /**
     * Registers a new user in the system.
     *
     * @param dto new user information
     * @throws UserAlreadyExistsException if a user with the same username already exists
     */
    ApplicationUser register(UserLoginDto dto) throws UserAlreadyExistsException;

    /**
     * Delete an already registered user.
     *
     * @param username to identify the user to delete
     * @throws NotFoundException if no registered user with username is not found
     */
    void deleteUser(String username) throws NotFoundException;

    /**
     * Update username and password for a user.
     *
     * @param user updated information for user
     * @param username target user to update
     * @return updated user
     * @throws NotFoundException if the user to update is not found by the username
     */
    ApplicationUser updateUser(UserLoginDto user, String username)
        throws NotFoundException, UserAlreadyExistsException;
}
