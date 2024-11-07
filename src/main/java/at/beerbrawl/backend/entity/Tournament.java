/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.entity;

import at.beerbrawl.backend.util.BeerDateTime;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * The tournament entity.
 * Manages relation with teams for consistency reasons.
 *
 */
@Entity
@Setter(value = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tournament {

    @Getter
    @Id
    @GeneratedValue
    private Long id;

    @Setter
    @Getter
    private String name;

    @Getter
    private LocalDateTime registrationEnd;

    @Getter
    private Long maxParticipants;

    @Setter
    @Getter
    private String description;

    @Getter
    @Column(nullable = false, updatable = false)
    private UUID publicAccessToken;

    @Setter
    @Getter
    @ManyToOne
    private ApplicationUser organizer;

    @OneToMany(mappedBy = Team_.TOURNAMENT, cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Team> teams = new LinkedList<>();

    @OneToMany(mappedBy = BeerPongTable_.TOURNAMENT, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<BeerPongTable> tables = new LinkedList<>();

    /*
     * If performance is an issue, consider fetching lazily.
     * But then actions on the list must be done an active trasactional context.
     * E.g. then you cannot change the registration end date outside of a transaction.
     */
    @OneToMany(
        mappedBy = QualificationMatch_.TOURNAMENT,
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<QualificationMatch> qualificationMatches = new LinkedList<>();

    @OneToMany(mappedBy = KoStanding_.TOURNAMENT, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<KoStanding> koStandings = new LinkedList<>();

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<SharedMedia> sharedMedia = new LinkedList<>();

    public Tournament(
        String name,
        LocalDateTime registrationEnd,
        Long maxParticipants,
        String description,
        ApplicationUser organizer
    ) {
        this.name = name;
        if (registrationEnd.isBefore(BeerDateTime.nowUtc())) {
            throw new IllegalArgumentException("Registration end must be in the future");
        }
        this.registrationEnd = registrationEnd;
        this.maxParticipants = maxParticipants;
        this.description = description;
        this.organizer = organizer;
        this.publicAccessToken = UUID.randomUUID();
    }

    public void setMaxParticipants(Long maxParticipants) {
        if (!getQualificationMatches().isEmpty()) {
            throw new IllegalStateException(
                "Cannot change max participants after qualification matches have been created"
            );
        }
        this.maxParticipants = maxParticipants;
    }

    public void setRegistrationEnd(LocalDateTime registrationEnd) {
        if (!getQualificationMatches().isEmpty()) {
            throw new IllegalStateException(
                "Cannot change registration end after qualification matches have been created"
            );
        }
        this.registrationEnd = registrationEnd;
    }

    public List<Team> getTeams() {
        return Collections.unmodifiableList(teams);
    }

    public List<QualificationMatch> getQualificationMatches() {
        return Collections.unmodifiableList(qualificationMatches);
    }

    public enum SignupTeamResult {
        SUCCESS,
        REGISTRATION_CLOSED,
        MAX_PARTICIPANTS_REACHED,
        TEAM_ALREADY_EXISTS,
    }

    public boolean isRegistrationStillOpen() {
        return getRegistrationEnd().isAfter(BeerDateTime.nowUtc());
    }

    public static class Utils {

        /**
         * Copy the list of all qualification matches and sort them by the heuristic.
         *
         * @param allQualificationMatches the list of ALL qualification matches
         * @return the copied and sorted list
         */
        public static List<QualificationMatch> copySortedByHeuristic(
            final List<QualificationMatch> allQualificationMatches
        ) {
            final var noOfMatchesAlreadyPlayedByTeamId = allQualificationMatches
                .stream()
                .flatMap(qm -> qm.getParticipations().stream())
                .collect(
                    Collectors.groupingBy(qm -> qm.getId().getTeamId(), Collectors.counting())
                );

            // order matches ascending by the no of previous matches played by its teams
            Comparator<QualificationMatch> comparator = Comparator.<
                QualificationMatch,
                Long
            >comparing(
                qm -> {
                    // for each match, extracts the no of played matches for the team with the least
                    // played matches
                    var teamIds = qm.getParticipations().stream().map(p -> p.getId().getTeamId());
                    var previousMatchesForTeams = teamIds
                        .map(noOfMatchesAlreadyPlayedByTeamId::get)
                        .toList();
                    var minPreviousMatches = previousMatchesForTeams
                        .stream()
                        .min(Long::compareTo)
                        .get();
                    return minPreviousMatches;
                },
                Comparator.reverseOrder()
            );

            // then further prioritize teams that are ready for the most time
            // TODO lets reset the readyAt time of a team as soon as it finished a match
            comparator = comparator.thenComparing(
                Comparator.comparing(QualificationMatch::getEarliestPossibleStart)
            );

            var orderedByBothReadyAt = allQualificationMatches
                .stream()
                .sorted(comparator)
                .collect(Collectors.toList());
            return orderedByBothReadyAt;
        }
    }
}
