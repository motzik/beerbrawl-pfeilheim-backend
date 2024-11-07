/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint.exceptionhandler;

import at.beerbrawl.backend.endpoint.dto.ValidationErrorDto;
import at.beerbrawl.backend.exception.BadTournamentPublicAccessTokenException;
import at.beerbrawl.backend.exception.NotFoundException;
import at.beerbrawl.backend.exception.PreconditionFailedException;
import at.beerbrawl.backend.exception.TeamMatchDrinksAlreadyPickedUpException;
import at.beerbrawl.backend.exception.TournamentAlreadyStartedException;
import jakarta.validation.ValidationException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Register all your Java exceptions here to map them into meaningful HTTP
 * exceptions. If you have special cases which are only important for specific
 * endpoints, use ResponseStatusExceptions.
 * https://www.baeldung.com/exception-handling-for-rest-with-spring#responsestatusexception
 */
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        MethodHandles.lookup().lookupClass()
    );

    /**
     * Use the @ExceptionHandler annotation to write handler for custom exceptions.
     */
    @ExceptionHandler(value = { NotFoundException.class })
    protected ResponseEntity<Object> handleNotFound(RuntimeException ex, WebRequest request) {
        LOGGER.warn(ex.getMessage());
        return handleExceptionInternal(
            ex,
            ex.getMessage(),
            new HttpHeaders(),
            HttpStatus.NOT_FOUND,
            request
        );
    }

    /**
     * Override methods from ResponseEntityExceptionHandler to send a customized
     * HTTP response for a know exception from e.g. Spring.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        // Get all errors
        List<String> errors = ex
            .getBindingResult()
            .getFieldErrors()
            .stream()
            .map(err -> err.getField() + " " + err.getDefaultMessage())
            .collect(Collectors.toList());

        final var dto = new ValidationErrorDto(errors);
        return new ResponseEntity<>(dto, headers, status);
    }

    /**
     * Overrides the handling of PreconditionFailedException that sends a HTTP 400
     * response.
     */
    @ExceptionHandler(value = { PreconditionFailedException.class })
    protected ResponseEntity<Object> handleAccessPreconditionFailedException(
        RuntimeException ex,
        WebRequest request
    ) {
        var preconditionException = (PreconditionFailedException) ex;
        return handleExceptionInternal(
            ex,
            String.format("A precondition wasn't met: %s", preconditionException.getMessage()),
            new HttpHeaders(),
            HttpStatus.BAD_REQUEST,
            request
        );
    }

    @ExceptionHandler({ BadCredentialsException.class, UsernameNotFoundException.class })
    protected ResponseEntity<Object> handleBadCredentialsException(
        RuntimeException ex,
        WebRequest request
    ) {
        LOGGER.debug(ex.getMessage());

        return handleExceptionInternal(
            ex,
            // NOTE: We *explicitly* and *always* return the error here - otherwise we'd leak data
            "Bad credentials: Username or password incorrect",
            new HttpHeaders(),
            HttpStatus.FORBIDDEN,
            request
        );
    }

    @ExceptionHandler({ TournamentAlreadyStartedException.class })
    protected ResponseEntity<Object> handleTournamentAlreadyStartedException(
        RuntimeException ex,
        WebRequest request
    ) {
        LOGGER.debug(ex.getMessage());

        return handleExceptionInternal(
            ex,
            "Tournament already started",
            new HttpHeaders(),
            HttpStatus.CONFLICT,
            request
        );
    }

    @ExceptionHandler({ TeamMatchDrinksAlreadyPickedUpException.class })
    protected ResponseEntity<Object> handleTeamMatchDrinksAlreadyPickedUp(
        RuntimeException ex,
        WebRequest request
    ) {
        LOGGER.debug(ex.getMessage());
        final var properEx = (TeamMatchDrinksAlreadyPickedUpException) ex;

        return handleExceptionInternal(
            ex,
            "Team %d has already picked up its drinks for match %d!".formatted(
                    properEx.getTeamId(),
                    properEx.getMatchId()
                ),
            new HttpHeaders(),
            HttpStatus.CONFLICT,
            request
        );
    }

    @ExceptionHandler({ BadTournamentPublicAccessTokenException.class })
    protected ResponseEntity<Object> handleBadTournamentPublicAccessTokenException(
        RuntimeException ex,
        WebRequest request
    ) {
        LOGGER.debug(ex.getMessage());

        return handleExceptionInternal(
            ex,
            "public access token missing or incorrect",
            new HttpHeaders(),
            HttpStatus.UNAUTHORIZED,
            request
        );
    }

    @ExceptionHandler({ ValidationException.class })
    protected ResponseEntity<Object> handleValidationException(
        RuntimeException ex,
        WebRequest request
    ) {
        LOGGER.debug(ex.getMessage());

        return handleExceptionInternal(
            ex,
            ex.getMessage(),
            new HttpHeaders(),
            HttpStatus.UNPROCESSABLE_ENTITY,
            request
        );
    }
}
