/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.service;

import at.beerbrawl.backend.endpoint.dto.TournamentUpdateKoStandingDto;
import at.beerbrawl.backend.entity.KoStanding;
import at.beerbrawl.backend.exception.NotFoundException;
import at.beerbrawl.backend.exception.PreconditionFailedException;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;

public interface TournamentKoPhaseService {
    /**
     * Retrieve a standing KO-phase standing.
     *
     * @param standingId the id of the standing entity to retrieve
     * @return the requested standing
     * @throws NotFoundException if the requested standing was not found
     */
    KoStanding getStandingById(long standingId) throws NotFoundException;

    /**
     * Generate ko matches for a tournament.
     *
     * @param tournamentId the id of the tournament entity
     * @param teamIds      the ids of the teams to generate matches for
     * @param subject      the name of user subject performing the action
     *                     (domain-level authorization)
     */
    void generateKoMatchesForTournament(Long tournamentId, List<Long> teamIds, String subject);

    /**
     * Get all standings for a tournament.
     *
     * @param tournamentId to identify target
     * @return a tree with all corresponding standings
     */
    KoStanding getKoStandingsTree(Long tournamentId)
        throws NotFoundException, PreconditionFailedException;

    /**
     * Updates the team of a standing.
     *
     * @param userName          of the user that does the update, needs to be the
     *                          same as the organizer
     * @param tournamentId      tournament the standing belongs to
     * @param standingId        id of the ko standing to update
     * @param updateDto details to update the standing with
     */
    void updateKoStanding(
        String userName,
        Long tournamentId,
        Long standingId,
        TournamentUpdateKoStandingDto updateDto
    ) throws AccessDeniedException, NotFoundException, PreconditionFailedException;
}
