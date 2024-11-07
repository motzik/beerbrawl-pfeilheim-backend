/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint.dto;

import java.time.LocalDateTime;
import java.util.List;

public record KoStandingDto(
    long id,
    LocalDateTime startTime,
    LocalDateTime endTime,
    TeamDto team,
    boolean drinksCollected,
    List<KoStandingDto> preceedingStandings,
    BeerPongTableDto table
) {}
