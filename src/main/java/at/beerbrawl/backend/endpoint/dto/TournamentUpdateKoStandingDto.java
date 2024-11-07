/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Describes updates that might be made to a tournament KO-phase match.
 *
 * @param teamSet Sets the team for this standing, aka. the winner of the two preceding standings
 *                If <code>null</code>, it is ignored without side-effects.
 * @param drinksPickup Marks the team as having drinks picked up for this KO-phase match
 *                     If <code>null</code>, it is ignored without side-effects.
 */
public record TournamentUpdateKoStandingDto(
    SetWinnerTeamDto teamSet,
    DrinksPickupDto drinksPickup
) {
    /**
     * Sets the winner team for this KO match. Can be <code>null</code>, in which
     * case it is unset.
     *
     * @param teamId ID of the team to set as winner, or <code>null</code> to unset
     */
    public record SetWinnerTeamDto(Long teamId) {}

    /**
     * Sets a team as having drinks picked up for this KO-phase match.
     * May not be <code>null</code>, as drinks can only ever be picked up once
     * exactly.
     *
     * @param teamId ID of the team that picked up its drinks
     */
    public record DrinksPickupDto(@NotNull long teamId) {}
}
