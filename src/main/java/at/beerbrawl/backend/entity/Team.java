/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.entity;

import at.beerbrawl.backend.util.BeerDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

/**
 * Owned by tournament, must not exist outside of a tournament.
 */
@Entity
@Getter(value = AccessLevel.PRIVATE)
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "TOURNAMENT_ID", Team_.NAME }) })
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class Team {

    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Getter
    @Size(min = 3, max = 20)
    private String name;

    private boolean checkedIn;

    /**
     * Scheduling Heuristic: when both teams are available for a longer time, they should play (again).
     */
    @Getter
    private LocalDateTime availableSince;

    @Getter
    @ManyToOne(optional = false)
    @JoinColumn(name = "TOURNAMENT_ID")
    @Check(
        name = "DO_NOT_SURPASS_MAX_PARTICIPANTS",
        constraints = "CHECK (SELECT COUNT(*) FROM TEAM team WHERE team.TOURNAMENT_ID = TOURNAMENT_ID) < (SELECT max(tnmt.MAX_PARTICIPANTS) FROM TOURNAMENT tnmt WHERE tnmt.ID = TOURNAMENT_ID)"
    )
    private Tournament tournament;

    @OneToMany(mappedBy = KoStanding_.TEAM)
    private List<KoStanding> koStandings = new LinkedList<>();

    @OneToMany(mappedBy = QualificationParticipation_.TEAM)
    private List<QualificationParticipation> qualificationParticipations = new LinkedList<>();

    public Team(String name, Tournament tournament) {
        this.name = name;
        this.tournament = tournament;
    }

    /**
     * Remove after 2024-06-24.
     */
    @Deprecated
    public Team(String name) {
        this.name = name;
    }

    public Boolean getCheckedIn() {
        return checkedIn;
    }

    public void checkIn() {
        this.checkedIn = true;
        markAvailable();
    }

    public void markAvailable() {
        this.availableSince = BeerDateTime.nowUtc();
    }

    /**
     * Remove after 2024-06-24.
     */
    @Deprecated
    protected void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }
}
