/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.entity.domainservice;

import at.beerbrawl.backend.entity.KoStanding;
import at.beerbrawl.backend.entity.Match.MatchStatus;
import at.beerbrawl.backend.entity.QualificationMatch;
import at.beerbrawl.backend.entity.Team;
import at.beerbrawl.backend.entity.Tournament;
import at.beerbrawl.backend.repository.BeerPongTableRepository;
import at.beerbrawl.backend.repository.KoStandingsRepository;
import at.beerbrawl.backend.repository.QualificationMatchRepository;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This service aims to solve various cross-cutting concerns related to matches.
 * It may only be used by regular services (i.e. may not be used by other domain
 * services).
 */
@AllArgsConstructor
@Service
public class MatchDomainService {

    private QualificationMatchRepository qualificationMatchRepository;
    private KoStandingsRepository koStandingsRepository;
    private BeerPongTableRepository beerPongTableRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(
        MethodHandles.lookup().lookupClass()
    );

    public List<QualificationMatch> getQualificationMatchesByExpectedStart(long tournamentId) {
        var qms = qualificationMatchRepository.findAllByTournamentId(tournamentId);
        var sorted = Tournament.Utils.copySortedByHeuristic(qms);
        return sorted;
    }

    public List<QualificationMatch> getQualificationMatchQueue(long tournamentId) {
        final var qmsSortedByExpectedStart = getQualificationMatchesByExpectedStart(tournamentId);
        final var runningMatches = qmsSortedByExpectedStart
            .stream()
            .filter(
                qm ->
                    List.of(MatchStatus.QUEUED_COLLECTING_DRINKS, MatchStatus.PLAYING).contains(
                        qm.getStatus()
                    )
            )
            .toList();
        final var currentlyPlayingTeams = runningMatches
            .stream()
            .flatMap(qm -> qm.getTeams().stream())
            .toList();
        final var upcomingMatches = qmsSortedByExpectedStart
            .stream()
            .filter(qm -> qm.getStatus() == MatchStatus.TEAMS_CHECKED_IN)
            .collect(Collectors.toList());
        final var queue = new LinkedList<QualificationMatch>();

        for (var qm : upcomingMatches) {
            var allOkay = true;
            for (Team team : qm.getTeams()) {
                // team already playing
                if (currentlyPlayingTeams.stream().anyMatch(team::equals)) {
                    allOkay = false;
                    break;
                }
                // team already queued
                if (queue.stream().flatMap(o -> o.getTeams().stream()).anyMatch(team::equals)) {
                    allOkay = false;
                    break;
                }
            }
            if (!allOkay) {
                continue;
            }
            queue.add(qm);
        }

        return queue;
    }

    public List<KoStanding> getKoMatchQueue(long tournamentId) {
        final var optionalFinale =
            koStandingsRepository.findFinaleByTournamentIdAndNextStandingIsNull(tournamentId);
        if (optionalFinale.isEmpty()) {
            return List.of();
        }
        final var finale = optionalFinale.get();

        // collect all non-leaf standings in a breadth-first manner
        final List<KoStanding> breadthFirstStandings = new LinkedList<>();
        var currentDepthStandings = List.of(finale);
        do {
            // add to end of list
            breadthFirstStandings.addAll(currentDepthStandings.reversed());
            currentDepthStandings = currentDepthStandings
                .stream()
                .flatMap(ks -> ks.getPreceedingStandings().stream())
                .toList();
        } while (currentDepthStandings.get(0).hasPrecedingMatches());

        // reverse bfs, i.e. leafes first
        Collections.reverse(breadthFirstStandings);
        final var inverseBreadthFirstMatches = breadthFirstStandings
            .stream()
            .filter(KoStanding::hasPrecedingMatches)
            .filter(s -> s.getStartTime() == null && s.getTable() == null)
            .filter(KoStanding::havePreceedingMatchesEnded)
            .collect(Collectors.toList());

        return inverseBreadthFirstMatches;
    }

    // region Scheduling
    /*
     * Had to make a method for each concrete type of match, as hibernate somehow
     * does not allow setting up the currentMatch relationship with a compile-time
     * type of Match.
     */
    @Transactional
    public void scheduleQualiMatches(long tournamentId) {
        final var queuedQms = getQualificationMatchQueue(tournamentId);
        final var freeTables = beerPongTableRepository.findByTournamentIdAndCurrentMatchIsNull(
            tournamentId
        );
        if (freeTables.isEmpty()) {
            LOGGER.debug("No free tables found for tournament with id {}", tournamentId);
            return;
        }

        for (final var match : queuedQms) {
            if (freeTables.size() == 0) {
                break;
            }
            var table = freeTables.removeFirst();
            table.setCurrentMatch(match);
            beerPongTableRepository.save(table);
        }
    }

    @Transactional
    public void scheduleKoMatches(long tournamentId) {
        final var freeTables = beerPongTableRepository.findByTournamentIdAndCurrentMatchIsNull(
            tournamentId
        );
        if (freeTables.isEmpty()) {
            LOGGER.debug("No free tables found for tournament with id {}", tournamentId);
            return;
        }

        final var queuedKoMatches = getKoMatchQueue(tournamentId);
        for (final var match : queuedKoMatches) {
            if (freeTables.size() == 0) {
                break;
            }
            var table = freeTables.removeFirst();
            table.setCurrentMatch(match);
            beerPongTableRepository.save(table);
        }
    }
    // endregion
}
