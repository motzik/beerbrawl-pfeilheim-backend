/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint.dto;

import at.beerbrawl.backend.entity.Match;
import at.beerbrawl.backend.entity.Team;

public record QueuedMatchDto(String[] teams, BeerPongTableDto table) {
    public static QueuedMatchDto fromMatch(Match match) {
        return new QueuedMatchDto(
            match.getTeams().stream().map(Team::getName).toArray(String[]::new),
            match.getTable() == null ? null : BeerPongTableDto.fromTable(match.getTable())
        );
    }
}
