/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CreateBeerPongTableDto {

    @NotNull(message = "Tournament id can't be null.")
    private Long tournamentId;

    @NotNull(message = "Name can't be null.")
    @Size(min = 3, max = 20, message = "Name must be between 3 and 20 characters long.")
    @NotBlank(message = "Name can't be empty.")
    @Pattern(regexp = "[\\w\\s\\.äÄöÖüÜ\\-,#]*", message = "Name contains not allowed characters.")
    private String name;

    public CreateBeerPongTableDto setTournamentId(Long tournamentId) {
        this.tournamentId = tournamentId;
        return this;
    }

    public CreateBeerPongTableDto setName(String name) {
        this.name = name;
        return this;
    }
}
