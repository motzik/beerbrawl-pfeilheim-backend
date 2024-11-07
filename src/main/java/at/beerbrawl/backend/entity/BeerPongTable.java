/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

@Entity
@Setter(value = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BeerPongTable {

    @Getter
    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Setter
    @Length(min = 3, max = 20)
    private String name;

    @Getter
    @ManyToOne(optional = false)
    private Tournament tournament;

    @Getter
    @Setter
    @OneToOne(optional = true)
    private Match currentMatch;

    // Prevent multiple concurrent match assignments
    @Version
    private Instant version;

    public BeerPongTable(String name, Tournament tournament) {
        this.name = name;
        this.tournament = tournament;
    }

    /**
     * This entity always belongs to a tournament.
     * Given that this entity still technically owns the relationship to the tournament,
     * the tournament should be set at all times.
     * Hence on creation, the tournament should already be set.
     *
     * @deprecated Use the constructor that requires the tournament.
     */
    @Deprecated
    public BeerPongTable(String name) {
        this.name = name;
    }

    /**
     * Sets the tournament of this entity.
     *
     * @deprecated The tournament should already be set at the time of creation.
     *      Only then can we ensure that the entity is in a valid state.
     *      See the note on the other deprecated constructor above.
     */
    @Deprecated
    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }
}
