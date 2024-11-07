/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TournamentListDto {

    @NotNull(message = "ID must not be null")
    private Long id;

    @NotNull(message = "Name must not be null")
    @Size(min = 1, max = 256)
    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @NotNull(message = "Registration end time must not be null")
    private LocalDateTime registrationEnd;

    @NotNull(message = "Maximum participants must not be null")
    private Long maxParticipants;

    @Size(max = 1024)
    private String description;

    @NotNull
    private UUID publicAccessToken;

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            name,
            registrationEnd,
            maxParticipants,
            description,
            publicAccessToken
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TournamentListDto that = (TournamentListDto) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(name, that.name) &&
            Objects.equals(registrationEnd, that.registrationEnd) &&
            Objects.equals(maxParticipants, that.maxParticipants) &&
            Objects.equals(description, that.description) &&
            Objects.equals(publicAccessToken, that.publicAccessToken)
        );
    }

    @Override
    public String toString() {
        return (
            "TournamentDto{" +
            "id=" +
            id +
            ", name='" +
            name +
            '\'' +
            ", registrationEnd=" +
            registrationEnd +
            ", maxParticipants=" +
            maxParticipants +
            ", description='" +
            description +
            '\'' +
            ", publicAccessToken='" +
            publicAccessToken +
            '\'' +
            '}'
        );
    }
}
