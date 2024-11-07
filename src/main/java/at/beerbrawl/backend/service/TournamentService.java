/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.service;

import at.beerbrawl.backend.endpoint.dto.TournamentUpdateDto;
import at.beerbrawl.backend.entity.Tournament;
import at.beerbrawl.backend.exception.NotFoundException;
import at.beerbrawl.backend.service.models.TournamentOverviewModel;
import jakarta.validation.ValidationException;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;

public interface TournamentService {
    /**
     * Find all tournament entries from specified organizer ordered by name.
     *
     * @return ordered list of all tournament entries
     */
    List<Tournament> findAllByOrganizer(String organizerName);

    /**
     * Create a single tournament entity.
     *
     * @param tournament to create
     * @return created tournament entity
     */
    Tournament create(Tournament tournament, String currentUserName);

    /**
     * Check if user is organizer of tournament.
     *
     * @param tournamentId to get matches for
     * @return collection of matches
     */
    boolean isOrganizer(String username, Long tournamentId);

    /**
     * Find a single tournament entity by id.
     *
     * @param tournamentId the id of the tournament entity
     * @return the tournament entity
     */
    Tournament findOne(long tournamentId);

    /**
     * Delete a single tournament entity by id.
     *
     * @param tournamentId the id of the tournament entity
     * @throws NotFoundException     if the tournament is not found
     * @throws AccessDeniedException if the current user does not have permission to
     *                               delete the tournament
     */
    void deleteTournament(long tournamentId, String currentUserName)
        throws NotFoundException, AccessDeniedException;

    /**
     * Extract information about the current state of a tournament.
     *
     * @param tournamentId to locate the target tournament
     * @return OverViewDto containing wanted information about the tournament.
     */
    TournamentOverviewModel getTournamentOverview(long tournamentId) throws NotFoundException;

    /**
     * Update a tournament.
     *
     * @param tournamentId to identify target
     * @param updates      contains the updated values
     */
    Tournament updateTournament(long tournamentId, TournamentUpdateDto updates)
        throws NotFoundException, ValidationException;

    /**
     * Count the number of started tournaments organized by a specific user.
     *
     * @param username the username of the organizer
     * @return the count of started tournaments
     */
    long countStartedTournaments(String username);

    /**
     * Count the number of not started tournaments organized by a specific user.
     *
     * @param username the username of the organizer
     * @return the count of not started tournaments
     */
    long countNotStartedTournaments(String username);

    void assertAccessTokenIsCorrect(Long tournamentId, UUID uuid);
}
