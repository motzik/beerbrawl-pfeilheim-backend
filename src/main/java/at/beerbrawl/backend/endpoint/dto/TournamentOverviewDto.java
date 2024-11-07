/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentOverviewDto {

    @NotNull(message = "Name can't be null.")
    @Size(max = 200, message = "Name can't be more than 200 characters long.")
    @NotBlank(message = "Name can't be empty.")
    @Pattern(regexp = "[\\w\\s\\.äÄöÖüÜ\\-,]*", message = "Name contains not allowed characters.")
    private String name;

    @NotNull(message = "Registration end can't be null.")
    @Future(message = "Registration end must be in the future.")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") // UTC format
    private LocalDateTime registrationEnd;

    @Min(value = 16, message = "Max participants needs to be a number larger than 16.")
    private Long maxParticipants;

    private String description;

    // Qualification specific
    @NotNull
    private int allQualificationMatches;

    @NotNull
    private int playedQualificationMatches;

    // KO specific
    @NotNull
    private int allKoMatches;

    @NotNull
    private int playedKoMatches;

    // Teams specific
    @NotNull
    private int teams;

    @NotNull
    private int checkedInTeams;

    // Tables specific
    @NotNull
    private int tables;

    private int tablesInUse;

    @NotNull
    private UUID publicAccessToken;
}
