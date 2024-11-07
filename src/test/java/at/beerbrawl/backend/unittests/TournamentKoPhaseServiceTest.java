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
import at.beerbrawl.backend.endpoint.dto.TournamentUpdateKoStandingDto;
import at.beerbrawl.backend.endpoint.dto.TournamentUpdateQualificationMatchDto;
import at.beerbrawl.backend.endpoint.dto.TournamentUpdateQualificationMatchDto.DrinksPickupDto;
import at.beerbrawl.backend.endpoint.dto.TournamentUpdateQualificationMatchDto.ScoreUpdateDto;
import at.beerbrawl.backend.entity.KoStanding;
import at.beerbrawl.backend.entity.Team;
import at.beerbrawl.backend.entity.Tournament;
import at.beerbrawl.backend.exception.NotFoundException;
import at.beerbrawl.backend.exception.PreconditionFailedException;
import at.beerbrawl.backend.repository.KoStandingsRepository;
import at.beerbrawl.backend.repository.QualificationParticipationRepository;
import at.beerbrawl.backend.repository.TeamRepository;
import at.beerbrawl.backend.repository.TournamentRepository;
import at.beerbrawl.backend.service.TournamentKoPhaseService;
import at.beerbrawl.backend.service.TournamentQualificationService;
import at.beerbrawl.backend.service.TournamentService;
import at.beerbrawl.backend.service.TournamentTeamService;
import at.beerbrawl.backend.service.models.QualificationTeamScoreModel;
import at.beerbrawl.backend.service.models.TeamModel;
import at.beerbrawl.backend.util.BeerDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Stack;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;

public class TournamentKoPhaseServiceTest extends TestData {

    @Autowired
    private TournamentKoPhaseService koService;

    @Autowired
    private TournamentQualificationService tournamentQualificationService;

    @Autowired
    private KoStandingsRepository koStandingsRepository;

    @Autowired
    private TournamentService tournamentService;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private QualificationParticipationRepository qualificationParticipationRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TournamentTeamService teamService;

    @Test
    public void generateKoMatches_givenValidQualiPhase_succeeds() {
        final var createdTournament = generateTournamentWithFinishedQualiPhase();

        final var best16TeamIds = tournamentQualificationService
            .getTournamentQualificationScoreTable(createdTournament.getId())
            .subList(0, 16)
            .stream()
            .map(QualificationTeamScoreModel::getId)
            .toList();
        koService.generateKoMatchesForTournament(
            createdTournament.getId(),
            best16TeamIds,
            TEST_USER
        );
        var koMatches = koStandingsRepository.findByTournament(createdTournament);
        // total node of binary tree with depth 4
        final var nodeCount = (1 << (4 + 1)) - 1;
        assertEquals(nodeCount, koMatches.size());
    }

    @Test
    public void generateKoMatches_switchedEdgeTeams_succeeds() {
        final var createdTournament = generateTournamentWithFinishedQualiPhaseAndDifferentScores();

        final var best16TeamIds = new LinkedList<>(
            tournamentQualificationService
                .getTournamentQualificationScoreTable(createdTournament.getId())
                .subList(0, 16)
                .stream()
                .map(QualificationTeamScoreModel::getId)
                .toList()
        );

        // remove the first team from the list and put it at the end
        best16TeamIds.addLast(best16TeamIds.removeFirst());

        // should throw an exception because the first team is not in the list
        assertThrows(
            PreconditionFailedException.class,
            () ->
                koService.generateKoMatchesForTournament(
                    createdTournament.getId(),
                    best16TeamIds,
                    TEST_USER
                )
        );
    }

    @Test
    public void generateKoMatches_guaranteedQualifiedTeamIsNotPicked_fails() {
        final var createdTournament = generateTournamentWithFinishedQualiPhaseAndDifferentScores();

        final var best16TeamIds = new LinkedList<>(
            tournamentQualificationService
                .getTournamentQualificationScoreTable(createdTournament.getId())
                .stream()
                .map(QualificationTeamScoreModel::getId)
                .toList()
        );

        // remove the first team from the list and put it at the end
        best16TeamIds.addLast(best16TeamIds.removeFirst());

        // should throw an exception because the first team is not in the list
        assertThrows(
            PreconditionFailedException.class,
            () ->
                koService.generateKoMatchesForTournament(
                    createdTournament.getId(),
                    best16TeamIds.subList(0, 16),
                    TEST_USER
                )
        );
    }

    @Test
    public void generateKoMatches_pickedTeamsAre15_fails() {
        final var createdTournament = generateTournamentWithFinishedQualiPhaseAndDifferentScores();

        final var best16TeamIds = new LinkedList<>(
            tournamentQualificationService
                .getTournamentQualificationScoreTable(createdTournament.getId())
                .stream()
                .map(QualificationTeamScoreModel::getId)
                .toList()
        );

        // remove the first team from the list and put it at the end
        best16TeamIds.addLast(best16TeamIds.removeFirst());

        // should throw an exception because the first team is not in the list
        assertThrows(
            PreconditionFailedException.class,
            () ->
                koService.generateKoMatchesForTournament(
                    createdTournament.getId(),
                    best16TeamIds.subList(0, 16),
                    TEST_USER
                )
        );
    }

    @Test
    public void generateKoMatches_pickedTeamsAre17_fails() {
        final var createdTournament = generateTournamentWithFinishedQualiPhaseAndDifferentScores();

        final var best16TeamIds = new LinkedList<>(
            tournamentQualificationService
                .getTournamentQualificationScoreTable(createdTournament.getId())
                .stream()
                .map(QualificationTeamScoreModel::getId)
                .toList()
        );

        // remove the first team from the list and put it at the end
        best16TeamIds.addLast(best16TeamIds.removeFirst());

        // should throw an exception because the first team is not in the list
        assertThrows(
            PreconditionFailedException.class,
            () ->
                koService.generateKoMatchesForTournament(
                    createdTournament.getId(),
                    best16TeamIds.subList(0, 16),
                    TEST_USER
                )
        );
    }

    @Test
    public void generateKoMatches_disqualifiedTeamIsPicked_fails() {
        final var createdTournament = generateTournamentWithFinishedQualiPhaseAndDifferentScores();

        final var best16TeamIds = new LinkedList<>(
            tournamentQualificationService
                .getTournamentQualificationScoreTable(createdTournament.getId())
                .stream()
                .map(QualificationTeamScoreModel::getId)
                .toList()
        );

        // put last team in the list in the first place
        best16TeamIds.addFirst(best16TeamIds.removeLast());

        // should throw an exception because the first team is not in the list
        assertThrows(
            PreconditionFailedException.class,
            () ->
                koService.generateKoMatchesForTournament(
                    createdTournament.getId(),
                    best16TeamIds.subList(0, 16),
                    TEST_USER
                )
        );
    }

    @Test
    public void getKoStandingsForTournamentThatDoesntExist() {
        assertThrows(NotFoundException.class, () -> koService.getKoStandingsTree(-1L));
    }

    @Test
    public void getKoStandingsForTournamentWith16ParticipatingTeams() {
        final var tournament = tournamentService.create(
            new Tournament(
                "TEST_TOURNAMENT",
                BeerDateTime.nowUtc().plusDays(1),
                64L,
                "testdescription",
                null
            ),
            TEST_USER
        );
        var teams = IntStream.range(0, 16)
            .mapToObj(i -> new Team("Test Team #%02d".formatted(i), tournament))
            .toList();
        teamRepository.saveAllAndFlush(teams);
        var qualificationMatches =
            tournamentQualificationService.generateQualificationMatchesForTournament(
                tournament.getId(),
                TEST_USER
            );

        for (var qualificationMatch : qualificationMatches) {
            var participations = qualificationParticipationRepository.findAllByQualificationMatchId(
                qualificationMatch.getId()
            );
            var team1 = participations.get(0).getTeam().getId();
            var team2 = participations.get(1).getTeam().getId();
            teamService.markTeamAsReady(tournament.getId(), team1);
            teamService.markTeamAsReady(tournament.getId(), team2);
            tournamentQualificationService.updateQualificationMatch(
                tournament.getId(),
                qualificationMatch.getId(),
                new TournamentUpdateQualificationMatchDto(null, new DrinksPickupDto(team1))
            );
            tournamentQualificationService.updateQualificationMatch(
                tournament.getId(),
                qualificationMatch.getId(),
                new TournamentUpdateQualificationMatchDto(null, new DrinksPickupDto(team2))
            );
            tournamentQualificationService.updateQualificationMatch(
                tournament.getId(),
                qualificationMatch.getId(),
                new TournamentUpdateQualificationMatchDto(new ScoreUpdateDto(team1, 10L), null)
            );
        }

        koService.generateKoMatchesForTournament(
            tournament.getId(),
            teams.stream().limit(16).map(Team::getId).toList(),
            TEST_USER
        );
        var flattenedGeneratedMatches = koStandingsRepository.getAllByTournamentId(
            tournament.getId()
        );

        var fetchedMatches = koService.getKoStandingsTree(tournament.getId());
        var flattenedFetchedMatches = new ArrayList<KoStanding>();
        var fetchedStack = new Stack<KoStanding>();
        fetchedStack.push(fetchedMatches);
        while (!fetchedStack.isEmpty()) {
            var match = fetchedStack.pop();
            flattenedFetchedMatches.add(match);
            for (var child : match.getPreceedingStandings()) {
                fetchedStack.push(child);
            }
        }

        assertEquals(flattenedFetchedMatches.size(), flattenedGeneratedMatches.size());
        for (var generatedMatch : flattenedGeneratedMatches) {
            var fetchedMatch = flattenedFetchedMatches
                .stream()
                .filter(m -> Objects.equals(m.getId(), generatedMatch.getId()))
                .findFirst();
            assertTrue(fetchedMatch.isPresent());
            var fetchedMatchUnwrapped = fetchedMatch.get();

            assertAll(
                () -> {
                    if (
                        generatedMatch.getTeam() == null || fetchedMatchUnwrapped.getTeam() == null
                    ) {
                        assertEquals(generatedMatch.getTeam(), fetchedMatchUnwrapped.getTeam());
                    } else {
                        assertEquals(
                            generatedMatch.getTeam().getId(),
                            fetchedMatchUnwrapped.getTeam().getId()
                        );
                    }
                },
                () ->
                    assertEquals(
                        generatedMatch.getStartTime(),
                        fetchedMatchUnwrapped.getStartTime()
                    ),
                () -> assertEquals(generatedMatch.getEndTime(), fetchedMatchUnwrapped.getEndTime()),
                () -> {
                    if (
                        generatedMatch.getPreceedingStandings() != null &&
                        fetchedMatchUnwrapped.getPreceedingStandings() != null
                    ) {
                        assertEquals(
                            generatedMatch.getPreceedingStandings().size(),
                            fetchedMatchUnwrapped.getPreceedingStandings().size()
                        );
                    }
                },
                () -> {
                    if (
                        generatedMatch.getNextStanding() == null ||
                        fetchedMatchUnwrapped.getNextStanding() == null
                    ) {
                        assertEquals(generatedMatch.getTeam(), fetchedMatchUnwrapped.getTeam());
                    } else {
                        assertEquals(
                            generatedMatch.getNextStanding().getId(),
                            fetchedMatchUnwrapped.getNextStanding().getId()
                        );
                    }
                },
                () ->
                    assertEquals(
                        generatedMatch.getTournament().getId(),
                        fetchedMatchUnwrapped.getTournament().getId()
                    )
            );
        }
    }

    @Test
    public void setWinnerOfTournamentThatDoesntExistAndItThrows() {
        var ex = assertThrows(
            NotFoundException.class,
            () ->
                koService.updateKoStanding(
                    TEST_USER,
                    -1L,
                    -1L,
                    new TournamentUpdateKoStandingDto(
                        new TournamentUpdateKoStandingDto.SetWinnerTeamDto(-1L),
                        null
                    )
                )
        );

        assertEquals(ex.getMessage(), "No tournament found.");
    }

    @Test
    public void setWinnerOfTournamenFromAnotherUserAndItThrows() {
        final var tournament = tournamentService.create(
            new Tournament(
                "TEST_TOURNAMENT",
                BeerDateTime.nowUtc().plusDays(1),
                64L,
                "testdescription",
                null
            ),
            TEST_USER
        );
        tournamentRepository.save(tournament);
        var ex = assertThrows(
            AccessDeniedException.class,
            () ->
                koService.updateKoStanding(
                    TEST_USER + "_",
                    tournament.getId(),
                    -1L,
                    new TournamentUpdateKoStandingDto(
                        new TournamentUpdateKoStandingDto.SetWinnerTeamDto(-1L),
                        null
                    )
                )
        );

        assertEquals(ex.getMessage(), "Current user isn't organizer of tournament.");
    }

    @Test
    public void setWinnerOfTournamentThatExistsButHasNoKoStandingsAndItThrows() {
        final var tournament = tournamentService.create(
            new Tournament(
                "TEST_TOURNAMENT",
                BeerDateTime.nowUtc().plusDays(1),
                64L,
                "testdescription",
                null
            ),
            TEST_USER
        );
        tournamentRepository.save(tournament);
        var ex = assertThrows(
            NotFoundException.class,
            () ->
                koService.updateKoStanding(
                    TEST_USER,
                    tournament.getId(),
                    -1L,
                    new TournamentUpdateKoStandingDto(
                        new TournamentUpdateKoStandingDto.SetWinnerTeamDto(-1L),
                        null
                    )
                )
        );

        assertEquals("KO standing not found", ex.getMessage());
    }

    @Test
    public void tryToChangeTeamOfFirstRoundStandingAndItThrows() {
        final var tournament = tournamentService.create(
            new Tournament(
                "TEST_TOURNAMENT",
                BeerDateTime.nowUtc().plusDays(1),
                64L,
                "testdescription",
                null
            ),
            TEST_USER
        );
        var teams = IntStream.range(0, 16)
            .mapToObj(i -> new Team("Test Team #%02d".formatted(i), tournament))
            .toList();
        teamRepository.saveAllAndFlush(teams);
        var qualificationMatches =
            tournamentQualificationService.generateQualificationMatchesForTournament(
                tournament.getId(),
                TEST_USER
            );

        for (var qualificationMatch : qualificationMatches) {
            var participations = qualificationParticipationRepository.findAllByQualificationMatchId(
                qualificationMatch.getId()
            );
            var team1 = participations.get(0).getTeam().getId();
            var team2 = participations.get(1).getTeam().getId();
            teamService.markTeamAsReady(tournament.getId(), team1);
            teamService.markTeamAsReady(tournament.getId(), team2);
            tournamentQualificationService.updateQualificationMatch(
                tournament.getId(),
                qualificationMatch.getId(),
                new TournamentUpdateQualificationMatchDto(null, new DrinksPickupDto(team1))
            );
            tournamentQualificationService.updateQualificationMatch(
                tournament.getId(),
                qualificationMatch.getId(),
                new TournamentUpdateQualificationMatchDto(null, new DrinksPickupDto(team2))
            );
            tournamentQualificationService.updateQualificationMatch(
                tournament.getId(),
                qualificationMatch.getId(),
                new TournamentUpdateQualificationMatchDto(new ScoreUpdateDto(team1, 10L), null)
            );
        }

        koService.generateKoMatchesForTournament(
            tournament.getId(),
            teams.stream().limit(16).map(Team::getId).toList(),
            TEST_USER
        );
        var matchOfFirstRound = assertDoesNotThrow(
            () ->
                koStandingsRepository
                    .getAllByTournamentId(tournament.getId())
                    .stream()
                    .filter(m -> !m.hasPrecedingMatches())
                    .findFirst()
                    .orElseThrow(Exception::new)
        );

        assertThrows(
            PreconditionFailedException.class,
            () ->
                koService.updateKoStanding(
                    TEST_USER,
                    tournament.getId(),
                    matchOfFirstRound.getId(),
                    new TournamentUpdateKoStandingDto(
                        new TournamentUpdateKoStandingDto.SetWinnerTeamDto(
                            teams.getFirst().getId()
                        ),
                        null
                    )
                ),
            "Team of first round can't be changed."
        );
    }

    @Test
    public void setWinnerOfStandingWhenTheNextStandingAlreadyHasATeamAssignedAndItThrows() {
        final var tournament = tournamentService.create(
            new Tournament(
                "TEST_TOURNAMENT",
                BeerDateTime.nowUtc().plusDays(1),
                64L,
                "testdescription",
                null
            ),
            TEST_USER
        );
        var teams = IntStream.range(0, 16)
            .mapToObj(i -> new Team("Test Team #%02d".formatted(i), tournament))
            .toList();
        teamRepository.saveAllAndFlush(teams);
        var qualificationMatches =
            tournamentQualificationService.generateQualificationMatchesForTournament(
                tournament.getId(),
                TEST_USER
            );

        for (var qualificationMatch : qualificationMatches) {
            var participations = qualificationParticipationRepository.findAllByQualificationMatchId(
                qualificationMatch.getId()
            );
            var team1 = participations.get(0).getTeam().getId();
            var team2 = participations.get(1).getTeam().getId();
            teamService.markTeamAsReady(tournament.getId(), team1);
            teamService.markTeamAsReady(tournament.getId(), team2);
            tournamentQualificationService.updateQualificationMatch(
                tournament.getId(),
                qualificationMatch.getId(),
                new TournamentUpdateQualificationMatchDto(null, new DrinksPickupDto(team1))
            );
            tournamentQualificationService.updateQualificationMatch(
                tournament.getId(),
                qualificationMatch.getId(),
                new TournamentUpdateQualificationMatchDto(null, new DrinksPickupDto(team2))
            );
            tournamentQualificationService.updateQualificationMatch(
                tournament.getId(),
                qualificationMatch.getId(),
                new TournamentUpdateQualificationMatchDto(new ScoreUpdateDto(team1, 10L), null)
            );
        }

        koService.generateKoMatchesForTournament(
            tournament.getId(),
            teams.stream().limit(16).map(Team::getId).toList(),
            TEST_USER
        );

        var standingToUpdate = assertDoesNotThrow(
            () ->
                koStandingsRepository
                    .getAllByTournamentId(tournament.getId())
                    .stream()
                    .filter(
                        s ->
                            s
                                .getPreceedingStandings()
                                .stream()
                                .anyMatch(ps -> !ps.hasPrecedingMatches())
                    )
                    .findFirst()
                    .get()
        );
        var nextStanding = assertDoesNotThrow(
            () -> koStandingsRepository.findById(standingToUpdate.getNextStanding().getId()).get()
        );
        nextStanding.setTeam(teams.getFirst());
        koStandingsRepository.save(nextStanding);

        var ex = assertThrows(
            PreconditionFailedException.class,
            () ->
                koService.updateKoStanding(
                    TEST_USER,
                    tournament.getId(),
                    standingToUpdate.getId(),
                    new TournamentUpdateKoStandingDto(
                        new TournamentUpdateKoStandingDto.SetWinnerTeamDto(
                            teams.getFirst().getId()
                        ),
                        null
                    )
                )
        );

        assertEquals(ex.getMessage(), "Team of next standing isn't empty");
    }

    @Test
    public void setFinalKoPhaseWinnerWithoutSettingTheWinnersOfThePreviousRoundsAndItThrows() {
        final var tournament = tournamentService.create(
            new Tournament(
                "TEST_TOURNAMENT",
                BeerDateTime.nowUtc().plusDays(1),
                64L,
                "testdescription",
                null
            ),
            TEST_USER
        );
        var teams = IntStream.range(0, 16)
            .mapToObj(i -> new Team("Test Team #%02d".formatted(i), tournament))
            .toList();
        teamRepository.saveAllAndFlush(teams);
        var qualificationMatches =
            tournamentQualificationService.generateQualificationMatchesForTournament(
                tournament.getId(),
                TEST_USER
            );

        for (var qualificationMatch : qualificationMatches) {
            var participations = qualificationParticipationRepository.findAllByQualificationMatchId(
                qualificationMatch.getId()
            );
            var team1 = participations.get(0).getTeam().getId();
            var team2 = participations.get(1).getTeam().getId();
            teamService.markTeamAsReady(tournament.getId(), team1);
            teamService.markTeamAsReady(tournament.getId(), team2);
            tournamentQualificationService.updateQualificationMatch(
                tournament.getId(),
                qualificationMatch.getId(),
                new TournamentUpdateQualificationMatchDto(null, new DrinksPickupDto(team1))
            );
            tournamentQualificationService.updateQualificationMatch(
                tournament.getId(),
                qualificationMatch.getId(),
                new TournamentUpdateQualificationMatchDto(null, new DrinksPickupDto(team2))
            );
            tournamentQualificationService.updateQualificationMatch(
                tournament.getId(),
                qualificationMatch.getId(),
                new TournamentUpdateQualificationMatchDto(new ScoreUpdateDto(team1, 10L), null)
            );
        }

        koService.generateKoMatchesForTournament(
            tournament.getId(),
            teams.stream().limit(16).map(Team::getId).toList(),
            TEST_USER
        );
        var finalStandingOption =
            koStandingsRepository.findFinaleByTournamentIdAndNextStandingIsNull(tournament.getId());

        assertTrue(finalStandingOption.isPresent());
        var finalStanding = finalStandingOption.get();

        var ex = assertThrows(
            PreconditionFailedException.class,
            () ->
                koService.updateKoStanding(
                    TEST_USER,
                    tournament.getId(),
                    finalStanding.getId(),
                    new TournamentUpdateKoStandingDto(
                        new TournamentUpdateKoStandingDto.SetWinnerTeamDto(
                            teams.getFirst().getId()
                        ),
                        null
                    )
                )
        );

        assertEquals(ex.getMessage(), "Team isn't assigned to a previous standing");
    }

    @Test
    public void setTeamAsWinnerThatHasntParticipatedInThePreceedingStandingsAndItThrows() {
        final var tournament = tournamentService.create(
            new Tournament(
                "TEST_TOURNAMENT",
                BeerDateTime.nowUtc().plusDays(1),
                64L,
                "testdescription",
                null
            ),
            TEST_USER
        );
        var teams = IntStream.range(0, 16)
            .mapToObj(i -> new Team("Test Team #%02d".formatted(i), tournament))
            .toList();
        teamRepository.saveAllAndFlush(teams);
        var qualificationMatches =
            tournamentQualificationService.generateQualificationMatchesForTournament(
                tournament.getId(),
                TEST_USER
            );

        for (var qualificationMatch : qualificationMatches) {
            var participations = qualificationParticipationRepository.findAllByQualificationMatchId(
                qualificationMatch.getId()
            );
            var team1 = participations.get(0).getTeam().getId();
            var team2 = participations.get(1).getTeam().getId();
            teamService.markTeamAsReady(tournament.getId(), team1);
            teamService.markTeamAsReady(tournament.getId(), team2);
            tournamentQualificationService.updateQualificationMatch(
                tournament.getId(),
                qualificationMatch.getId(),
                new TournamentUpdateQualificationMatchDto(null, new DrinksPickupDto(team1))
            );
            tournamentQualificationService.updateQualificationMatch(
                tournament.getId(),
                qualificationMatch.getId(),
                new TournamentUpdateQualificationMatchDto(null, new DrinksPickupDto(team2))
            );
            tournamentQualificationService.updateQualificationMatch(
                tournament.getId(),
                qualificationMatch.getId(),
                new TournamentUpdateQualificationMatchDto(new ScoreUpdateDto(team1, 10L), null)
            );
        }

        koService.generateKoMatchesForTournament(
            tournament.getId(),
            teams.stream().limit(16).map(Team::getId).toList(),
            TEST_USER
        );

        var standingToUpdate = assertDoesNotThrow(
            () ->
                koStandingsRepository
                    .getAllByTournamentId(tournament.getId())
                    .stream()
                    .filter(
                        s ->
                            s
                                .getPreceedingStandings()
                                .stream()
                                .anyMatch(ps -> !ps.hasPrecedingMatches())
                    )
                    .findFirst()
                    .get()
        );

        var teamIdsOfStandingToUpdate = standingToUpdate
            .getPreceedingStandings()
            .stream()
            .map(ps -> ps.getTeam().getId())
            .toList();

        var ex = assertThrows(
            PreconditionFailedException.class,
            () ->
                koService.updateKoStanding(
                    TEST_USER,
                    tournament.getId(),
                    standingToUpdate.getId(),
                    new TournamentUpdateKoStandingDto(
                        new TournamentUpdateKoStandingDto.SetWinnerTeamDto(
                            teams
                                .stream()
                                .filter(t -> !teamIdsOfStandingToUpdate.contains(t.getId()))
                                .findFirst()
                                .get()
                                .getId()
                        ),
                        null
                    )
                )
        );

        assertEquals(ex.getMessage(), "Team isn't assigned to a previous standing");
    }

    @Test
    public void setWinnerOfFirstRoundAndClearItAfterwards() {
        final var tournament = tournamentService.create(
            new Tournament(
                "TEST_TOURNAMENT",
                BeerDateTime.nowUtc().plusDays(1),
                64L,
                "testdescription",
                null
            ),
            TEST_USER
        );
        var teams = IntStream.range(0, 16)
            .mapToObj(i -> new Team("Test Team #%02d".formatted(i), tournament))
            .toList();
        teamRepository.saveAllAndFlush(teams);
        var qualificationMatches =
            tournamentQualificationService.generateQualificationMatchesForTournament(
                tournament.getId(),
                TEST_USER
            );

        for (var qualificationMatch : qualificationMatches) {
            var participations = qualificationParticipationRepository.findAllByQualificationMatchId(
                qualificationMatch.getId()
            );
            var team1 = participations.get(0).getTeam().getId();
            var team2 = participations.get(1).getTeam().getId();
            teamService.markTeamAsReady(tournament.getId(), team1);
            teamService.markTeamAsReady(tournament.getId(), team2);
            tournamentQualificationService.updateQualificationMatch(
                tournament.getId(),
                qualificationMatch.getId(),
                new TournamentUpdateQualificationMatchDto(null, new DrinksPickupDto(team1))
            );
            tournamentQualificationService.updateQualificationMatch(
                tournament.getId(),
                qualificationMatch.getId(),
                new TournamentUpdateQualificationMatchDto(null, new DrinksPickupDto(team2))
            );
            tournamentQualificationService.updateQualificationMatch(
                tournament.getId(),
                qualificationMatch.getId(),
                new TournamentUpdateQualificationMatchDto(new ScoreUpdateDto(team1, 10L), null)
            );
        }

        koService.generateKoMatchesForTournament(
            tournament.getId(),
            teams.stream().limit(16).map(Team::getId).toList(),
            TEST_USER
        );

        final var standingToUpdate = koService
            .getKoStandingsTree(tournament.getId())
            .getInitialStandings()
            .findFirst()
            .get()
            .getNextStanding();

        // collect drinks
        for (final var preceding : standingToUpdate.getPreceedingStandings()) {
            assertDoesNotThrow(
                () ->
                    koService.updateKoStanding(
                        TEST_USER,
                        tournament.getId(),
                        preceding.getId(),
                        new TournamentUpdateKoStandingDto(
                            null,
                            new TournamentUpdateKoStandingDto.DrinksPickupDto(
                                preceding.getTeam().getId()
                            )
                        )
                    )
            );
        }

        assertDoesNotThrow(
            () ->
                koService.updateKoStanding(
                    TEST_USER,
                    tournament.getId(),
                    standingToUpdate.getId(),
                    new TournamentUpdateKoStandingDto(
                        new TournamentUpdateKoStandingDto.SetWinnerTeamDto(
                            standingToUpdate
                                .getPreceedingStandings()
                                .stream()
                                .map(KoStanding::getTeam)
                                .findFirst()
                                .get()
                                .getId()
                        ),
                        null
                    )
                )
        );

        assertDoesNotThrow(
            () ->
                koService.updateKoStanding(
                    TEST_USER,
                    tournament.getId(),
                    standingToUpdate.getId(),
                    new TournamentUpdateKoStandingDto(
                        new TournamentUpdateKoStandingDto.SetWinnerTeamDto(null),
                        null
                    )
                )
        );
    }

    @Test
    public void setDrinksCollectedForBothParticipantsShouldStartMatch() throws Exception {
        final var tournament = this.generateTournamentWithFinishedQualiPhase();
        final var teams = teamService.getTournamentTeams(tournament.getId());
        koService.generateKoMatchesForTournament(
            tournament.getId(),
            teams.stream().limit(16).map(TeamModel::id).toList(),
            TEST_USER
        );

        var standing = koService
            .getKoStandingsTree(tournament.getId())
            .getInitialStandings()
            .findFirst()
            .get()
            .getNextStanding();

        // collect drinks for match 1
        for (final var preceding : standing.getPreceedingStandings()) {
            assertDoesNotThrow(
                () ->
                    koService.updateKoStanding(
                        TEST_USER,
                        tournament.getId(),
                        preceding.getId(),
                        new TournamentUpdateKoStandingDto(
                            null,
                            new TournamentUpdateKoStandingDto.DrinksPickupDto(
                                preceding.getTeam().getId()
                            )
                        )
                    )
            );
        }

        final var standing1 = koService.getStandingById(standing.getId());
        assertNotNull(standing1.getStartTime());
        assertNull(standing1.getEndTime());
        assertNull(standing1.getTeam());

        // set winner for match 1
        assertDoesNotThrow(
            () ->
                koService.updateKoStanding(
                    TEST_USER,
                    tournament.getId(),
                    standing1.getId(),
                    new TournamentUpdateKoStandingDto(
                        new TournamentUpdateKoStandingDto.SetWinnerTeamDto(
                            standing1.getTeams().getFirst().getId()
                        ),
                        null
                    )
                )
        );

        final var finalStanding1 = koService.getStandingById(standing1.getId());
        assertNotNull(finalStanding1.getEndTime());
        assertNotNull(finalStanding1.getTeam());

        // get second on same level
        standing = finalStanding1
            .getNextStanding()
            .getPreceedingStandings()
            .stream()
            .filter(p -> !Objects.equals(p.getId(), standing1.getId()))
            .findFirst()
            .get();

        // collect drinks for match 2
        for (final var preceding : standing.getPreceedingStandings()) {
            assertDoesNotThrow(
                () ->
                    koService.updateKoStanding(
                        TEST_USER,
                        tournament.getId(),
                        preceding.getId(),
                        new TournamentUpdateKoStandingDto(
                            null,
                            new TournamentUpdateKoStandingDto.DrinksPickupDto(
                                preceding.getTeam().getId()
                            )
                        )
                    )
            );
        }

        final var standing2 = koService.getStandingById(standing.getId());
        assertNotNull(standing2.getStartTime());
        assertNull(standing2.getEndTime());
        assertNull(standing2.getTeam());

        // set winner for match 2
        assertDoesNotThrow(
            () ->
                koService.updateKoStanding(
                    TEST_USER,
                    tournament.getId(),
                    standing2.getId(),
                    new TournamentUpdateKoStandingDto(
                        new TournamentUpdateKoStandingDto.SetWinnerTeamDto(
                            standing2.getTeams().getFirst().getId()
                        ),
                        null
                    )
                )
        );

        final var finalStanding2 = koService.getStandingById(standing2.getId());
        assertNotNull(finalStanding2.getEndTime());
        assertNotNull(finalStanding2.getTeam());

        final var next = standing1.getNextStanding();

        // collect drinks for match between standing1 vs. standing2
        assertDoesNotThrow(
            () ->
                koService.updateKoStanding(
                    TEST_USER,
                    tournament.getId(),
                    finalStanding1.getId(),
                    new TournamentUpdateKoStandingDto(
                        null,
                        new TournamentUpdateKoStandingDto.DrinksPickupDto(
                            finalStanding1.getTeam().getId()
                        )
                    )
                )
        );

        assertDoesNotThrow(
            () ->
                koService.updateKoStanding(
                    TEST_USER,
                    tournament.getId(),
                    finalStanding2.getId(),
                    new TournamentUpdateKoStandingDto(
                        null,
                        new TournamentUpdateKoStandingDto.DrinksPickupDto(
                            finalStanding2.getTeam().getId()
                        )
                    )
                )
        );

        final var nextStanding = koService.getStandingById(next.getId());
        assertNotNull(nextStanding.getStartTime());
        assertNull(nextStanding.getEndTime());
        assertNull(nextStanding.getTeam());

        assertDoesNotThrow(
            () ->
                koService.updateKoStanding(
                    TEST_USER,
                    tournament.getId(),
                    nextStanding.getId(),
                    new TournamentUpdateKoStandingDto(
                        new TournamentUpdateKoStandingDto.SetWinnerTeamDto(
                            nextStanding.getTeams().getFirst().getId()
                        ),
                        null
                    )
                )
        );

        final var finalNextStanding = koService.getStandingById(nextStanding.getId());
        assertNotNull(finalNextStanding.getEndTime());
        assertNotNull(finalNextStanding.getTeam());
    }

    @Test
    public void cannotSetWinnerTeamForUnstartedMatch() throws Exception {
        final var tournament = this.generateTournamentWithFinishedQualiPhase();
        final var teams = teamService.getTournamentTeams(tournament.getId());
        koService.generateKoMatchesForTournament(
            tournament.getId(),
            teams.stream().limit(16).map(TeamModel::id).toList(),
            TEST_USER
        );

        final var standing = koService
            .getKoStandingsTree(tournament.getId())
            .getInitialStandings()
            .findFirst()
            .get()
            .getNextStanding();

        assertNull(standing.getStartTime());
        assertNull(standing.getEndTime());
        assertNull(standing.getTeam());

        final var ex = assertThrows(
            PreconditionFailedException.class,
            () ->
                koService.updateKoStanding(
                    TEST_USER,
                    tournament.getId(),
                    standing.getId(),
                    new TournamentUpdateKoStandingDto(
                        new TournamentUpdateKoStandingDto.SetWinnerTeamDto(
                            standing.getTeams().getFirst().getId()
                        ),
                        null
                    )
                )
        );
        assertEquals("Match has not started yet!", ex.getMessage());
    }

    @Test
    public void cannotCollectDrinksForMatchWhereOtherParticipantIsNotSetYet() throws Exception {
        final var tournament = this.generateTournamentWithFinishedQualiPhase();
        final var teams = teamService.getTournamentTeams(tournament.getId());
        koService.generateKoMatchesForTournament(
            tournament.getId(),
            teams.stream().limit(16).map(TeamModel::id).toList(),
            TEST_USER
        );

        final var standing = koService
            .getKoStandingsTree(tournament.getId())
            .getInitialStandings()
            .findFirst()
            .get()
            .getNextStanding();

        assertNull(standing.getStartTime());
        assertNull(standing.getEndTime());
        assertNull(standing.getTeam());

        // collect drinks
        for (final var preceding : standing.getPreceedingStandings()) {
            assertDoesNotThrow(
                () ->
                    koService.updateKoStanding(
                        TEST_USER,
                        tournament.getId(),
                        preceding.getId(),
                        new TournamentUpdateKoStandingDto(
                            null,
                            new TournamentUpdateKoStandingDto.DrinksPickupDto(
                                preceding.getTeam().getId()
                            )
                        )
                    )
            );
        }

        // set winner
        final var winnerId = standing.getTeams().getFirst().getId();
        assertDoesNotThrow(
            () ->
                koService.updateKoStanding(
                    TEST_USER,
                    tournament.getId(),
                    standing.getId(),
                    new TournamentUpdateKoStandingDto(
                        new TournamentUpdateKoStandingDto.SetWinnerTeamDto(winnerId),
                        null
                    )
                )
        );

        final var ex = assertThrows(
            PreconditionFailedException.class,
            () ->
                koService.updateKoStanding(
                    TEST_USER,
                    tournament.getId(),
                    standing.getId(),
                    new TournamentUpdateKoStandingDto(
                        null,
                        new TournamentUpdateKoStandingDto.DrinksPickupDto(winnerId)
                    )
                )
        );
        assertEquals("Both teams for the next match must be set", ex.getMessage());
    }
}
