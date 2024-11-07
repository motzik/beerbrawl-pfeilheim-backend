/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.service;

import at.beerbrawl.backend.endpoint.dto.TournamentUpdateQualificationMatchDto;
import at.beerbrawl.backend.entity.QualificationMatch;
import at.beerbrawl.backend.exception.NotFoundException;
import at.beerbrawl.backend.exception.PreconditionFailedException;
import at.beerbrawl.backend.service.models.QualificationTeamScoreModel;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;

public interface TournamentQualificationService {
    /**
     * Create all qualifying matchups needed for tournament.
     *
     * @param tournamentId to create matchups for
     * @return created qualifying matches
     */
    List<QualificationMatch> generateQualificationMatchesForTournament(
        Long tournamentId,
        String currentUserName
    ) throws PreconditionFailedException, AccessDeniedException, NotFoundException;

    /**
     * Get all qualification matches for a tournament.
     *
     * @param tournamentId to get matches for
     * @return collection of matches
     */
    List<QualificationMatch> getQualificationMatchesForTournament(Long tournamentId);

    /**
     * Updates a qualification match in a tournament with new details.
     *
     * @param tournamentId ID of the tournament to update
     * @param matchId      ID of the match in the tournament
     * @param updateDto    New data for the specified match in the tournament
     * @return The updated qualification match
     */
    QualificationMatch updateQualificationMatch(
        Long tournamentId,
        Long matchId,
        TournamentUpdateQualificationMatchDto updateDto
    );

    /**
     * Get the qualification score table for a tournament.
     *
     * @param tournamentId the id of the tournament entity
     * @return the qualification score list
     */
    List<QualificationTeamScoreModel> getTournamentQualificationScoreTable(Long tournamentId);
}
