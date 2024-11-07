/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint.dto;

import at.beerbrawl.backend.entity.BeerPongTable;

public record BeerPongTableDto(Long id, String name, Long tournamentId) {
    public static BeerPongTableDto fromTable(BeerPongTable table) {
        return new BeerPongTableDto(table.getId(), table.getName(), table.getTournament().getId());
    }
}
