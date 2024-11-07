/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.service.impl;

import at.beerbrawl.backend.endpoint.dto.UserLoginDto;
import at.beerbrawl.backend.entity.ApplicationUser;
import at.beerbrawl.backend.exception.NotFoundException;
import at.beerbrawl.backend.exception.UserAlreadyExistsException;
import at.beerbrawl.backend.repository.UserRepository;
import at.beerbrawl.backend.security.JwtTokenizer;
import at.beerbrawl.backend.service.UserService;
import jakarta.transaction.Transactional;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailService implements UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        MethodHandles.lookup().lookupClass()
    );
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenizer jwtTokenizer;

    public CustomUserDetailService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtTokenizer jwtTokenizer
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenizer = jwtTokenizer;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LOGGER.debug("Load all user by username");
        try {
            ApplicationUser applicationUser = findApplicationUserByUsername(username);

            List<GrantedAuthority> grantedAuthorities;
            if (applicationUser.getAdmin()) {
                grantedAuthorities = AuthorityUtils.createAuthorityList("ROLE_ADMIN", "ROLE_USER");
            } else {
                grantedAuthorities = AuthorityUtils.createAuthorityList("ROLE_USER");
            }

            return new User(
                applicationUser.getUsername(),
                applicationUser.getPassword(),
                grantedAuthorities
            );
        } catch (NotFoundException e) {
            throw new UsernameNotFoundException(e.getMessage(), e);
        }
    }

    @Override
    public ApplicationUser findApplicationUserByUsername(String username)
        throws UsernameNotFoundException {
        LOGGER.debug("Find application user by username '{}'", username);
        ApplicationUser applicationUser = userRepository.findByUsername(username);
        if (applicationUser != null) {
            return applicationUser;
        }
        throw new UsernameNotFoundException(
            String.format("Could not find the user with username '%s'", username)
        );
    }

    @Override
    public String login(UserLoginDto userLoginDto) {
        LOGGER.debug("Trying to log in user '{}'", userLoginDto.getUsername());
        UserDetails userDetails = loadUserByUsername(userLoginDto.getUsername());
        if (
            userDetails != null &&
            userDetails.isAccountNonExpired() &&
            userDetails.isAccountNonLocked() &&
            userDetails.isCredentialsNonExpired() &&
            passwordEncoder.matches(userLoginDto.getPassword(), userDetails.getPassword())
        ) {
            List<String> roles = userDetails
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
            return jwtTokenizer.getAuthToken(userDetails.getUsername(), roles);
        }
        throw new BadCredentialsException("Username or password incorrect");
    }

    @Override
    public ApplicationUser register(UserLoginDto dto) throws UserAlreadyExistsException {
        LOGGER.debug("Trying to register user '{}'", dto.getUsername());

        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new UserAlreadyExistsException(
                "Username '%s' already taken".formatted(dto.getUsername())
            );
        }
        var newUser = new ApplicationUser(
            dto.getUsername(),
            passwordEncoder.encode(dto.getPassword()),
            false
        );
        return userRepository.save(newUser);
    }

    @Override
    @Transactional
    public void deleteUser(String username) throws NotFoundException {
        LOGGER.debug("Deleting registered user '{}'", username);

        var targetUser = userRepository.findByUsername(username);
        if (targetUser != null) {
            userRepository.deleteByUsername(targetUser.getUsername());
            return;
        }
        throw new NotFoundException("User %s was not found".formatted(username));
    }

    @Override
    @Transactional
    public ApplicationUser updateUser(UserLoginDto user, String username)
        throws NotFoundException, UserAlreadyExistsException {
        LOGGER.debug("Updating registered user '{}'", user.getUsername());

        if (
            !Objects.equals(username, user.getUsername()) &&
            userRepository.existsByUsername(user.getUsername())
        ) {
            throw new UserAlreadyExistsException(
                "User with username '%s' already exists.".formatted(user.getUsername())
            );
        }

        var targetUser = userRepository.findByUsername(username);
        if (targetUser != null) {
            targetUser.setPassword(passwordEncoder.encode(user.getPassword()));
            targetUser.setUsername(user.getUsername());
            userRepository.save(targetUser); // optional (auto tracking changes by JPA)
            return targetUser;
        }
        throw new NotFoundException("User '%s' not found. ".formatted(username));
    }
}
