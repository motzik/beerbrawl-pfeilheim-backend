/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint.dto;

import at.beerbrawl.backend.entity.Tournament;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record TournamentDto(
    Long id,
    String name,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") // UTC format
    LocalDateTime registrationEnd,
    Long maxParticipants,
    String description
) {
    public static TournamentDto fromEntity(Tournament e) {
        return new TournamentDto(
            e.getId(),
            e.getName(),
            e.getRegistrationEnd(),
            e.getMaxParticipants(),
            e.getDescription()
        );
    }
}
