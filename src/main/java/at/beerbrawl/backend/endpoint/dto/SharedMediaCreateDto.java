/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SharedMediaCreateDto {

    @NotBlank(message = "Author cannot be blank")
    private String author;

    @NotBlank(message = "Title cannot be blank")
    private String title;

    @NotNull(message = "Tournament ID cannot be null")
    private Long tournamentId;

    @Null
    private byte[] image;
}
