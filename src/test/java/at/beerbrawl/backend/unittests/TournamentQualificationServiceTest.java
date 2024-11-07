/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.unittests;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.beerbrawl.backend.basetest.TestData;
import at.beerbrawl.backend.endpoint.dto.TournamentUpdateQualificationMatchDto;
import at.beerbrawl.backend.entity.QualificationParticipation;
import at.beerbrawl.backend.entity.Team;
import at.beerbrawl.backend.entity.Tournament;
import at.beerbrawl.backend.exception.NotFoundException;
import at.beerbrawl.backend.exception.PreconditionFailedException;
import at.beerbrawl.backend.repository.QualificationMatchRepository;
import at.beerbrawl.backend.repository.QualificationParticipationRepository;
import at.beerbrawl.backend.repository.TeamRepository;
import at.beerbrawl.backend.repository.TournamentRepository;
import at.beerbrawl.backend.repository.UserRepository;
import at.beerbrawl.backend.service.TournamentQualificationService;
import at.beerbrawl.backend.service.TournamentService;
import at.beerbrawl.backend.service.TournamentTeamService;
import at.beerbrawl.backend.util.BeerDateTime;
import java.util.HashMap;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;

public class TournamentQualificationServiceTest extends TestData {

    @Autowired
    UserRepository userRepository;

    @Autowired
    TournamentRepository tournamentRepository;

    @Autowired
    TeamRepository teamRepository;

    @Autowired
    QualificationMatchRepository qualificationMatchRepository;

    @Autowired
    QualificationParticipationRepository qualificationParticipationRepository;

    @Autowired
    TournamentQualificationService qualificationService;

    @Autowired
    TournamentService tournamentService;

    @Autowired
    TournamentTeamService teamService;

    @Test
    public void generateQualificationMatchesForTournamentWithEnoughTeams() throws Exception {
        var tournament = new Tournament(
            "TOURNAMENT_WITH_TEAMS",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "testdescription",
            userRepository.findByUsername(TEST_USER)
        );

        tournamentRepository.saveAndFlush(tournament);

        var numberOfTeams = 16;
        for (int i = 0; i < numberOfTeams; i++) {
            var team = new Team("Team" + Integer.toString(i), tournament);
            teamRepository.save(team);
        }
        teamRepository.flush();

        var matches = qualificationService.generateQualificationMatchesForTournament(
            tournament.getId(),
            TEST_USER
        );
        assertEquals(matches.size(), numberOfTeams);

        var qualificationParticipations = matches
            .stream()
            .flatMap(
                m ->
                    qualificationParticipationRepository
                        .findAllByQualificationMatchId(m.getId())
                        .stream()
            )
            .toList();

        var numberOfMatchesForTeam = new HashMap<Long, Integer>();
        for (var participation : qualificationParticipations) {
            if (!numberOfMatchesForTeam.containsKey(participation.getTeam().getId())) {
                numberOfMatchesForTeam.put(participation.getTeam().getId(), 0);
            }
            numberOfMatchesForTeam.put(
                participation.getTeam().getId(),
                numberOfMatchesForTeam.get(participation.getTeam().getId()) + 1
            );
        }

        for (var matchesCount : numberOfMatchesForTeam.values()) {
            assertEquals(matchesCount, 2);
        }
    }

    @Test
    public void generateQualificationMatchesForTournamentWithoutEnoughTeams() throws Exception {
        // setup
        var tournament = new Tournament(
            "TOURNAMENT_WITHOUT_TEAMS",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "testdescription",
            userRepository.findByUsername(TEST_USER)
        );

        tournamentRepository.saveAndFlush(tournament);

        assertThrows(
            PreconditionFailedException.class,
            () ->
                qualificationService.generateQualificationMatchesForTournament(
                    tournament.getId(),
                    TEST_USER
                )
        );
    }

    @Test
    public void generateQualificationMatchesForTournamentFromAnotherOrganizerWhenItIsntAllowed()
        throws Exception {
        // setup
        var tournament = new Tournament(
            "TOURNAMENT_WITHOUT_TEAMS",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "testdescription",
            userRepository.findByUsername(TEST_USER)
        );

        tournamentRepository.saveAndFlush(tournament);

        assertThrows(
            AccessDeniedException.class,
            () ->
                qualificationService.generateQualificationMatchesForTournament(
                    tournament.getId(),
                    TEST_USER + "_"
                )
        );
    }

    @Test
    public void generateQualificationMatchesForTournamentThatDoesntExist() throws Exception {
        assertThrows(
            NotFoundException.class,
            () ->
                qualificationService.generateQualificationMatchesForTournament(-1l, TEST_USER + "_")
        );
    }

    @Test
    public void teamCannotCollectDrinksForQualificationMatchIsNotReadyYet() {
        var tournament = this.generateTournamentWithQualificationMatches();

        this.qualificationMatchRepository.findAllByTournamentId(tournament.getId()).forEach(
                m ->
                    assertAll(
                        () -> assertNull(m.getStartTime()),
                        () -> assertNull(m.getWinner()),
                        () -> assertNull(m.getEndTime()),
                        () -> assertNull(m.getWinnerPoints())
                    )
            );

        var match =
            this.qualificationMatchRepository.findAllByTournamentId(tournament.getId()).getFirst();

        final var teamIds = match
            .getParticipations()
            .stream()
            .map(p -> p.getTeam().getId())
            .toList();

        assertThrows(
            PreconditionFailedException.class,
            () ->
                this.qualificationService.updateQualificationMatch(
                        tournament.getId(),
                        match.getId(),
                        new TournamentUpdateQualificationMatchDto(
                            null,
                            new TournamentUpdateQualificationMatchDto.DrinksPickupDto(
                                teamIds.get(0)
                            )
                        )
                    ),
            "Team is not marked as ready yet"
        );
    }

    @Test
    public void qualificationMatchStartsWhenBothTeamsHaveDrinksCollected() {
        var tournament = this.generateTournamentWithQualificationMatches();

        this.qualificationMatchRepository.findAllByTournamentId(tournament.getId()).forEach(
                m ->
                    assertAll(
                        () -> assertNull(m.getStartTime()),
                        () -> assertNull(m.getWinner()),
                        () -> assertNull(m.getEndTime()),
                        () -> assertNull(m.getWinnerPoints())
                    )
            );

        var match =
            this.qualificationMatchRepository.findAllByTournamentId(tournament.getId()).getFirst();

        final var teamIds = match
            .getParticipations()
            .stream()
            .map(p -> p.getTeam().getId())
            .toList();

        for (final var id : teamIds) {
            teamService.markTeamAsReady(tournament.getId(), id);
        }

        for (final var id : teamIds) {
            qualificationService.updateQualificationMatch(
                tournament.getId(),
                match.getId(),
                new TournamentUpdateQualificationMatchDto(
                    null,
                    new TournamentUpdateQualificationMatchDto.DrinksPickupDto(id)
                )
            );
        }

        final var m = this.qualificationMatchRepository.findById(match.getId());
        assertTrue(m.isPresent());
        assertNotNull(m.get().getStartTime());
        assertTrue(m.get().getStartTime().isBefore(BeerDateTime.nowUtc()));
    }

    @Test
    public void teamCannotPickupDrinksForOtherQualificationMatchIfCurrentlyPlaying() {
        var tournament = this.generateTournamentWithQualificationMatches();

        this.qualificationMatchRepository.findAllByTournamentId(tournament.getId()).forEach(
                m ->
                    assertAll(
                        () -> assertNull(m.getStartTime()),
                        () -> assertNull(m.getWinner()),
                        () -> assertNull(m.getEndTime()),
                        () -> assertNull(m.getWinnerPoints())
                    )
            );

        var teamId =
            this.qualificationMatchRepository.findAllByTournamentId(tournament.getId())
                .getFirst()
                .getParticipations()
                .getFirst()
                .getTeam()
                .getId();

        final var allTeamParticipations =
            this.qualificationParticipationRepository.findByTeamId(teamId);

        // mark all other teams ready and their drinks picked up, for matches where the picked team participates
        final var matches = allTeamParticipations
            .stream()
            .map(QualificationParticipation::getQualificationMatch)
            .toList();

        assertEquals(matches.size(), 2);

        // first, mark all teams as ready
        for (final var match : matches) {
            for (final var participant : match.getParticipations()) {
                teamService.markTeamAsReady(tournament.getId(), participant.getTeam().getId());
            }
        }

        // now, mark drinks as collected for all the *other* teams in
        for (final var match : matches) {
            for (final var participant : match.getParticipations()) {
                final var otherTeamId = participant.getTeam().getId();
                if (!Objects.equals(otherTeamId, teamId)) {
                    qualificationService.updateQualificationMatch(
                        tournament.getId(),
                        participant.getQualificationMatch().getId(),
                        new TournamentUpdateQualificationMatchDto(
                            null,
                            new TournamentUpdateQualificationMatchDto.DrinksPickupDto(otherTeamId)
                        )
                    );
                }
            }
        }

        // start the first match for this team, so that it is currently playing
        final var match1 = assertDoesNotThrow(
            () ->
                qualificationService.updateQualificationMatch(
                    tournament.getId(),
                    matches.get(0).getId(),
                    new TournamentUpdateQualificationMatchDto(
                        null,
                        new TournamentUpdateQualificationMatchDto.DrinksPickupDto(teamId)
                    )
                )
        );
        assertNotNull(match1.getStartTime());

        // now try to pick up drinks for a team that is currently playing
        final var ex = assertThrows(
            PreconditionFailedException.class,
            () ->
                qualificationService.updateQualificationMatch(
                    tournament.getId(),
                    matches.get(1).getId(),
                    new TournamentUpdateQualificationMatchDto(
                        null,
                        new TournamentUpdateQualificationMatchDto.DrinksPickupDto(teamId)
                    )
                )
        );
        assertEquals(
            "Cannot mark drinks for team %d as picked up, currently playing in another match".formatted(
                    teamId
                ),
            ex.getMessage()
        );

        // assert that the match has *not* started yet
        final var match2 = qualificationMatchRepository.findById(matches.get(1).getId()).get();
        assertNotNull(match2);
        assertNull(match2.getStartTime());
    }
}
