/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Designated usage: Build a tree from leaf to root.
 */
@Entity
@Getter
@Setter(value = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
//Required for inheritance consistency
@OnDelete(action = OnDeleteAction.CASCADE)
public class KoStanding extends Match {

    @ManyToOne
    @Setter(AccessLevel.PUBLIC)
    private KoStanding nextStanding;

    /**
     * Winner of this knock-out match.
     */
    @ManyToOne
    @Setter(AccessLevel.PUBLIC)
    private Team team;

    @Setter(AccessLevel.PUBLIC)
    private boolean drinksCollected;

    /**
     * This being `null` or an empty list does semantically mean
     * the same thing in any case - there are no preceding matches.
     * Eager loading could be removed in case of performance issues.
     * You could then query the KoStandings by the tournament and build the tree manually in memory.
     */
    @OneToMany(
        mappedBy = KoStanding_.NEXT_STANDING,
        cascade = CascadeType.PERSIST,
        fetch = FetchType.EAGER
    )
    private List<KoStanding> preceedingStandings;

    /**
     * Unchecked Precondition: Team is even part of tournament.
     */
    public KoStanding(Tournament tournament, List<KoStanding> preceedingStandings, Team team) {
        super(tournament);
        this.team = team;
        // ok, this is just a round0 standing
        if (preceedingStandings == null || preceedingStandings.isEmpty()) {
            this.preceedingStandings = new LinkedList<>();
            return;
        }

        // remaining: intermediate or final node
        // syntactic error
        if (preceedingStandings.size() != 2) {
            throw new IllegalArgumentException(
                "Non-initial KoStandings must have 2 preceeding standings"
            );
        }

        // if a team is set, it must be eligible
        final var teamsOfPreceedingStandings = preceedingStandings
            .stream()
            .map(KoStanding::getTeam)
            .toList();
        if (team != null && !teamsOfPreceedingStandings.contains(team)) {
            throw new IllegalArgumentException("Team not in preceeding standings");
        }

        this.preceedingStandings = preceedingStandings;
    }

    public enum KoStandingValidationResult {
        OK,
        WRONG_DEPTH,
        DUPLICATE_PARTICIPANTS,
    }

    public KoStandingValidationResult evaluateValidity() throws IllegalArgumentException {
        if (!hasDepth(4)) {
            return KoStandingValidationResult.WRONG_DEPTH;
        }

        if (hasDuplicateInitialTeams()) {
            return KoStandingValidationResult.DUPLICATE_PARTICIPANTS;
        }

        return KoStandingValidationResult.OK;
    }

    private boolean hasDuplicateInitialTeams() {
        var initialTeams = getInitialParticipantsRecursively().toArray();
        boolean hasDuplicateInitialTeams =
            Arrays.stream(initialTeams).distinct().count() != initialTeams.length;
        return hasDuplicateInitialTeams;
    }

    private boolean hasDepth(int depth) {
        if (!this.hasPrecedingMatches() && depth == 0) {
            return true;
        } else if (!this.hasPrecedingMatches() || depth == 0) {
            return false;
        }
        // Should be enforced by the constructor, just in case the hibernate is not
        // working correctly when bypassing the constructor
        if (preceedingStandings.size() != 2) {
            throw new IllegalArgumentException(
                "Non-initial KoStandings must have 2 preceeding standings"
            );
        }

        return preceedingStandings.stream().allMatch(t -> t.hasDepth(depth - 1));
    }

    /**
     * Returns the initial participants of the tournament.
     * Precondition: The tree is dense and syntactically correct.
     *
     * @return The initial participants of the tournament.
     */
    public LongStream getInitialParticipantsRecursively() {
        if (!this.hasPrecedingMatches()) {
            return LongStream.of(team.getId());
        }
        return preceedingStandings
            .stream()
            .map(KoStanding::getInitialParticipantsRecursively)
            .reduce(LongStream::concat)
            .get();
    }

    /**
     * Returns the initial matches of the tournament.
     * Precondition: The tree is dense and syntactically correct.
     *
     * @return The initial matches of the tournament.
     */
    public Stream<KoStanding> getInitialStandings() {
        if (!this.hasPrecedingMatches()) {
            return Stream.of(this);
        }

        return this.preceedingStandings.stream()
            .map(KoStanding::getInitialStandings)
            .reduce(Stream::concat)
            .get();
    }

    public boolean hasPrecedingMatches() {
        return this.preceedingStandings != null && !this.preceedingStandings.isEmpty();
    }

    public List<Team> getTeams() {
        return preceedingStandings.stream().map(KoStanding::getTeam).toList();
    }

    public boolean havePreceedingMatchesEnded() {
        return preceedingStandings
            .stream()
            .allMatch(
                preceeding -> !preceeding.hasPrecedingMatches() || preceeding.getEndTime() != null
            );
    }
}
