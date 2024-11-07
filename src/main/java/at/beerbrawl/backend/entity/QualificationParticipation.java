/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter(value = AccessLevel.PRIVATE)
@Entity
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class QualificationParticipation extends Participation {

    /**
     * Meant to be used by the qualification match.
     */
    protected QualificationParticipation(Team team, QualificationMatch qualificationMatch) {
        this.team = team;
        this.qualificationMatch = qualificationMatch;
    }

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @MapsId("teamId")
    private Team team;

    @ManyToOne(optional = false)
    @MapsId("matchId")
    private QualificationMatch qualificationMatch;
}
