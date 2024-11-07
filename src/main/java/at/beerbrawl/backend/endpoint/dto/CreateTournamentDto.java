/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class CreateTournamentDto {

    @NotNull(message = "Name can't be null.")
    @Size(max = 200, message = "Name can't be more than 200 characters long.")
    @NotBlank(message = "Name can't be empty.")
    @Pattern(regexp = "[\\w\\s\\.äÄöÖüÜ\\-,]*", message = "Name contains not allowed characters.")
    private String name;

    @NotNull(message = "Registration end can't be null.")
    @Future(message = "Registration end must be in the future.")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") // UTC format
    private LocalDateTime registrationEnd;

    @Min(value = 16, message = "Max participants needs to be a number larger than 16.")
    @Max(value = 64, message = "No more than 64 teams are allowed.")
    private Long maxParticipants;

    private String description;

    public String getName() {
        return name;
    }

    public CreateTournamentDto setName(String name) {
        this.name = name;
        return this;
    }

    public LocalDateTime getRegistrationEnd() {
        return registrationEnd;
    }

    public CreateTournamentDto setRegistrationEnd(LocalDateTime registrationEnd) {
        this.registrationEnd = registrationEnd;
        return this;
    }

    public Long getMaxParticipants() {
        return maxParticipants;
    }

    public CreateTournamentDto setMaxParticipants(Long maxParticipants) {
        this.maxParticipants = maxParticipants;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public CreateTournamentDto setDescription(String description) {
        this.description = description;
        return this;
    }
}
