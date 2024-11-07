/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint.dto;

public record TournamentQualificationMatchParticipantDto(
    Long teamId,
    String name,
    Boolean drinksCollected,
    Boolean isWinner,
    Boolean isReady
) {}
