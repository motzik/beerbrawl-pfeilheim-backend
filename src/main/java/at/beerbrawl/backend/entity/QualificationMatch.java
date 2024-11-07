/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Setter(value = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
// Required for inheritance consistency
@OnDelete(action = OnDeleteAction.CASCADE)
public class QualificationMatch extends Match {

    @ManyToOne
    @Getter
    private Team winner;

    @Getter
    private Long winnerPoints;

    /*
     * The participations of the teams in this match.
     * Guaranteed to have exactly two elements.
     */
    @OneToMany(
        mappedBy = QualificationParticipation_.QUALIFICATION_MATCH,
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<QualificationParticipation> participations = new ArrayList<>();

    public QualificationMatch(Tournament tournament, List<Team> teams) {
        super(tournament);
        if (teams.size() != 2) {
            throw new IllegalArgumentException("A match must have exactly two participations");
        }
        this.participations = teams
            .stream()
            .map(team -> new QualificationParticipation(team, this))
            .toList();
    }

    public void setWinner(Team winner) {
        if (
            !participations
                .stream()
                .map(QualificationParticipation::getTeam)
                .map(Team::getId)
                .toList()
                .contains(winner.getId())
        ) {
            throw new IllegalArgumentException("Winner must be one of the participants.");
        }
        this.winner = winner;
    }

    public void setWinnerPoints(Long winnerPoints) {
        if (winnerPoints < 0) {
            throw new IllegalArgumentException("Winner points must be positive");
        }
        this.winnerPoints = winnerPoints;
    }

    public List<QualificationParticipation> getParticipations() {
        return Collections.unmodifiableList(participations);
    }

    @Override
    public List<Team> getTeams() {
        return Collections.unmodifiableList(
            participations.stream().map(QualificationParticipation::getTeam).toList()
        );
    }

    /**
     * Returns the latest time a team was ready at.
     * If any team is not ready, the maximum date is returned.
     *
     * @return the earliest time we could possibly start the match, given out current Information.
     */
    public LocalDateTime getEarliestPossibleStart() {
        var teamsReadyAt = getTeams().stream().map(Team::getAvailableSince).toList();
        if (teamsReadyAt.contains(null)) {
            return LocalDateTime.MAX;
        }
        return teamsReadyAt.stream().max(LocalDateTime::compareTo).get();
    }
}
