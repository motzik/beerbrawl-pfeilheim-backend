/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Objects;
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
public class UserDetailDto {

    @NotNull(message = "Username must not be null")
    @Size(min = 3, max = 256)
    private String username;

    @NotNull
    private List<String> tournaments;

    @NotNull
    private long startedTournaments;

    @NotNull
    private long notStartedTournaments;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserDetailDto that = (UserDetailDto) o;
        return (
            startedTournaments == that.startedTournaments &&
            notStartedTournaments == that.notStartedTournaments &&
            Objects.equals(username, that.username)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, tournaments, startedTournaments, notStartedTournaments);
    }

    @Override
    public String toString() {
        return (
            "UserDetailDto{" +
            "username='" +
            username +
            '\'' +
            ", TournamentCount=" +
            tournaments.size() +
            ", closedTournaments=" +
            startedTournaments +
            ", openTournaments=" +
            notStartedTournaments +
            '}'
        );
    }
}
