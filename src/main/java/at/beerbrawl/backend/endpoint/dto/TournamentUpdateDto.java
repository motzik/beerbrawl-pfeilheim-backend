/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record TournamentUpdateDto(
    @Size(max = 200, message = "Name can't be more than 200 characters long.")
    @NotBlank(message = "Name can't be empty.")
    @Pattern(regexp = "[\\w\\s\\.äÄöÖüÜ\\-,]*", message = "Name contains not allowed characters.")
    String name,
    LocalDateTime registrationEnd,
    @Min(value = 16, message = "Max participants needs to be a number larger than 16.")
    @Max(value = 64, message = "No more than 64 teams are allowed.")
    Long maxParticipants,
    String description
) {}
