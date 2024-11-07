/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint.dto;

public record QualificationTeamScoreDto(
    Long id,
    String name,
    Boolean ready,
    Long wins,
    Long losses,
    Long points,
    Long gamesPlayed,
    Long position
) {}
