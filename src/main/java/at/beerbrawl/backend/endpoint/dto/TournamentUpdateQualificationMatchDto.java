/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Describes updates that might be made to a tournament qualification match.
 *
 * @param scoreUpdate New match results for the qualification match
 * @param drinksPickup Marks the team as having drinks picked up for this qualification match
 */
public record TournamentUpdateQualificationMatchDto(
    @Valid ScoreUpdateDto scoreUpdate,
    @Valid DrinksPickupDto drinksPickup
) {
    /**
     * Updates or sets this qualification match's results.
     *
     * @param winnerTeamId ID of the team that won
     * @param winnerPoints Number of points the winner of the match achieved
     */
    public record ScoreUpdateDto(
        @NotNull Long winnerTeamId,
        @NotNull @PositiveOrZero @Max(128) Long winnerPoints
    ) {}

    /**
     * Sets a team as having drinks picked up for this qualification match.
     *
     * @param teamId ID of the team that picked up its drinks
     */
    public record DrinksPickupDto(@NotNull long teamId) {}
}
