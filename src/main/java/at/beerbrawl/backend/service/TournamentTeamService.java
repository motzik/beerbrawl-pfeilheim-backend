/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.service;

import at.beerbrawl.backend.endpoint.dto.TournamentUpdateTeamDto;
import at.beerbrawl.backend.entity.Team;
import at.beerbrawl.backend.entity.Tournament;
import at.beerbrawl.backend.exception.NotFoundException;
import at.beerbrawl.backend.service.models.TeamModel;
import java.util.Collection;
import java.util.UUID;

public interface TournamentTeamService {
    Team getById(long teamId) throws NotFoundException;

    /**
     * Get all teams participating in a tournament.
     *
     * @param tournamentId to get teams for
     * @return collection of teams
     */
    Collection<TeamModel> getTournamentTeams(Long tournamentId);

    /**
     * Create a new tournament.
     * name may not be duplicate within the same tournament
     *
     * @return created tournament entity
     */
    Tournament.SignupTeamResult signupTeamForTournament(
        long tournamentId,
        UUID selfRegistrationToken,
        String name
    );

    /**
     * Update a teams information in a tournament.
     *
     * @param tournamentId ID of the tournament the team belongs to
     * @param teamId       ID of the team to update
     * @param updateDto    New information for the team to update
     * @return The updated team
     */
    Team updateTeam(Long tournamentId, Long teamId, TournamentUpdateTeamDto updateDto);

    /**
     * Delete a team from a tournament.
     *
     * @param tournamentId the id of the tournament entity
     * @param teamId       the id of the team entity
     */
    void deleteTeam(Long tournamentId, Long teamId);

    /**
     * Mark a team as ready for a tournament.
     *
     * @param tournamentId the id of the tournament entity
     * @param teamId       the id of the team entity
     */
    void markTeamAsReady(long tournamentId, long teamId);

    /**
     * Determines whether a team is currently playing in another match or not.
     *
     * @param teamId the id of the team entity
     * @return whether the team is playing another match currently or not
     */
    boolean isTeamCurrentlyPlaying(long teamId);
}
