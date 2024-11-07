/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.unittests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.beerbrawl.backend.basetest.TestData;
import at.beerbrawl.backend.datagenerator.TestDataGenerator;
import at.beerbrawl.backend.endpoint.dto.TournamentUpdateDto;
import at.beerbrawl.backend.entity.BeerPongTable;
import at.beerbrawl.backend.entity.KoStanding;
import at.beerbrawl.backend.entity.Tournament;
import at.beerbrawl.backend.entity.Tournament.SignupTeamResult;
import at.beerbrawl.backend.entity.domainservice.MatchDomainService;
import at.beerbrawl.backend.repository.BeerPongTableRepository;
import at.beerbrawl.backend.repository.KoStandingsRepository;
import at.beerbrawl.backend.repository.QualificationMatchRepository;
import at.beerbrawl.backend.repository.TeamRepository;
import at.beerbrawl.backend.repository.TournamentRepository;
import at.beerbrawl.backend.repository.UserRepository;
import at.beerbrawl.backend.service.TournamentKoPhaseService;
import at.beerbrawl.backend.service.TournamentQualificationService;
import at.beerbrawl.backend.service.TournamentService;
import at.beerbrawl.backend.service.TournamentTeamService;
import at.beerbrawl.backend.service.models.QualificationTeamScoreModel;
import at.beerbrawl.backend.util.BeerDateTime;
import jakarta.validation.ValidationException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
public class TournamentServiceTest extends TestData {

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TournamentService tournamentService;

    @Autowired
    private TournamentQualificationService tournamentQualificationService;

    @Autowired
    private KoStandingsRepository koStandingsRepository;

    @Autowired
    private QualificationMatchRepository qualificationMatchRepository;

    @Autowired
    private BeerPongTableRepository beerpongTableRepository;

    @Autowired
    private TournamentKoPhaseService tournamentKoPhaseService;

    @Autowired
    private MatchDomainService matchDomainService;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TournamentTeamService teamService;

    @Test
    public void createNewTournamentWithTestUserAsOrganizer() {
        var tournament = new Tournament(
            "TEST_TOURNAMENT",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "testdescription",
            userRepository.findByUsername(TEST_USER)
        );
        tournament = tournamentService.create(tournament, TestDataGenerator.TEST_USER);

        assertNotNull(tournament);
        assertTrue(tournamentRepository.existsById(tournament.getId()));

        tournamentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void signupTeam_givenDuplicateTeamName_fails() {
        var tournament = new Tournament(
            "TestTournament",
            LocalDateTime.MAX,
            64L,
            "TestDescription",
            null
        );
        tournamentRepository.saveAndFlush(tournament);

        teamService.signupTeamForTournament(
            tournament.getId(),
            tournament.getPublicAccessToken(),
            "DuplicateName"
        );
        var signupTeamWithDuplicateNameResult = teamService.signupTeamForTournament(
            tournament.getId(),
            tournament.getPublicAccessToken(),
            "DuplicateName"
        );
        assertEquals(SignupTeamResult.TEAM_ALREADY_EXISTS, signupTeamWithDuplicateNameResult);
    }

    @Test
    public void signUpTeam_givenValidData_Works() {
        var tournament = new Tournament(
            "TestTournament",
            LocalDateTime.MAX,
            64L,
            "TestDescription",
            null
        );
        tournament = tournamentRepository.save(tournament);
        var signupTeamResult1 = teamService.signupTeamForTournament(
            tournament.getId(),
            tournament.getPublicAccessToken(),
            "team1"
        );
        assertEquals(SignupTeamResult.SUCCESS, signupTeamResult1);
        var signupTeamResult2 = teamService.signupTeamForTournament(
            tournament.getId(),
            tournament.getPublicAccessToken(),
            "team2"
        );
        assertEquals(SignupTeamResult.SUCCESS, signupTeamResult2);
    }

    @Test
    public void editTournament_ValidUpdate() {
        var tournament = new Tournament(
            "testname",
            BeerDateTime.nowUtc().plusMinutes(2),
            32L,
            "testdescription",
            userRepository.findByUsername(TEST_USER)
        );
        tournamentRepository.saveAndFlush(tournament);

        var testUser = userRepository.findByUsername(TEST_USER);
        var existing = tournamentRepository
            .findAllByOrganizerIdOrderByNameAsc(testUser.getId())
            .getFirst();
        assertNotNull(existing);
        var updates = new TournamentUpdateDto(
            "Updated",
            existing.getRegistrationEnd(),
            64L,
            "Updated description"
        );
        assertNotEquals(existing.getName(), updates.name());

        tournamentService.updateTournament(tournament.getId(), updates);
        var existingUpdated = tournamentRepository.findById(tournament.getId()).get();
        assertNotNull(existingUpdated);
        assertEquals(existingUpdated.getMaxParticipants(), updates.maxParticipants());
        assertEquals(existingUpdated.getRegistrationEnd(), updates.registrationEnd());
        assertEquals(existingUpdated.getName(), updates.name());
    }

    @Test
    public void editTournament_ValidUpdate_StartedTournament_ThrowsValidationException() {
        var tournament = this.generateTournamentWithQualificationMatches();
        final var updates = new TournamentUpdateDto(
            "Updated",
            tournament.getRegistrationEnd(),
            60L,
            "Updated description"
        );
        assertNotEquals(tournament.getName(), updates.name());

        assertThrows(
            ValidationException.class,
            () -> tournamentService.updateTournament(tournament.getId(), updates),
            "maxParticipants cannot be updated for running tournament"
        );

        final var updates2 = new TournamentUpdateDto(
            "Updated",
            tournament.getRegistrationEnd().plusDays(7),
            64L,
            "Updated description"
        );
        assertThrows(
            ValidationException.class,
            () -> tournamentService.updateTournament(tournament.getId(), updates2),
            "registrationEnd cannot be updated for running tournament"
        );
    }

    @Test
    @Transactional
    public void getQueuedQualificationMatches_givenBothMatchesOfTheTeamReady_onlyOneOfTheirMatchesQueued() {
        final var tournament = super.generateTournamentWithQualificationMatches();
        final var focalTeam = teamRepository
            .findByTournamentId(tournament.getId())
            .stream()
            .findAny()
            .get();
        final var allMatches = qualificationMatchRepository.findAllByTournamentId(
            tournament.getId()
        );
        final var matchesOfFocalTeam = allMatches
            .stream()
            .filter(match -> match.getTeams().contains(focalTeam))
            .toList();
        for (var match : matchesOfFocalTeam) {
            for (var team : match.getTeams()) {
                team.checkIn();
                teamRepository.saveAndFlush(team);
            }
        }

        final var queue = matchDomainService.getQualificationMatchQueue(tournament.getId());
        assertTrue(
            queue.size() == 1,
            "Expected exactly one match queued with the same team, but got " + queue.size()
        );
    }

    @Test
    public void getQueuedKoMatches_givenNewlyGeneratedKoPhase_ReturnsFirstNonLeafRound() {
        var tournament = super.generateTournamentWithFinishedQualiPhase();
        var bestTeams = tournamentQualificationService
            .getTournamentQualificationScoreTable(tournament.getId())
            .stream()
            .map(QualificationTeamScoreModel::getId)
            .limit(16)
            .toList();
        tournamentKoPhaseService.generateKoMatchesForTournament(
            tournament.getId(),
            bestTeams,
            TEST_USER
        );

        var finale = koStandingsRepository
            .findFinaleByTournamentIdAndNextStandingIsNull(tournament.getId())
            .get();

        // get first round (represented by the standings just above the leaves)
        var eventuallyLeafes = List.of(finale);
        List<KoStanding> eventuallyFirstRound;
        do {
            eventuallyFirstRound = eventuallyLeafes;
            eventuallyLeafes = eventuallyLeafes
                .stream()
                .flatMap(ko -> ko.getPreceedingStandings().stream())
                .toList();
        } while (eventuallyLeafes.stream().allMatch(ko -> ko.getPreceedingStandings().size() == 2));

        var queuedMatchIds = matchDomainService
            .getKoMatchQueue(tournament.getId())
            .stream()
            .map(KoStanding::getId)
            .toList();

        //matches the queue, irrelevant of order
        assertEquals(eventuallyFirstRound.size(), queuedMatchIds.size());
        assertTrue(
            eventuallyFirstRound.stream().map(KoStanding::getId).allMatch(queuedMatchIds::contains)
        );
    }

    @Test
    public void doSchedule_givenNewlyGeneratedQuali_SchedulesAllMatches() {
        var tournament = super.generateTournamentWithQualificationMatches();
        var bpTable1 = new BeerPongTable("bpTable1", tournament);
        var bpTable2 = new BeerPongTable("bpTable1", tournament);
        beerpongTableRepository.saveAllAndFlush(List.of(bpTable1, bpTable2));

        super.setAllTeamsReadyBypassingScheduling(tournament);
        //todo use service method to assure no tables are unused insted
        matchDomainService.scheduleQualiMatches(tournament.getId());
        var beerPongTables = beerpongTableRepository.findAllByTournamentId(tournament.getId());
        var allOccupied = beerPongTables
            .stream()
            .allMatch(table -> table.getCurrentMatch() != null);
        assertTrue(allOccupied);
    }

    @Test
    public void doScheduleJustEnoughTables_givenFreshKo_allocatesAllTables() {
        var tournament = super.generateTournamentWithFinishedQualiPhase();
        var bestTeams = tournamentQualificationService
            .getTournamentQualificationScoreTable(tournament.getId())
            .stream()
            .map(QualificationTeamScoreModel::getId)
            .limit(16)
            .toList();
        tournamentKoPhaseService.generateKoMatchesForTournament(
            tournament.getId(),
            bestTeams,
            TEST_USER
        );
        // 8 is just enough to cover all round 0 matches
        var moreThanEnoughTables = IntStream.range(0, 8)
            .mapToObj(i -> new BeerPongTable("bpTable" + i, tournament))
            .toList();
        beerpongTableRepository.saveAllAndFlush(moreThanEnoughTables);

        matchDomainService.scheduleKoMatches(tournament.getId());
        // todo use service method to assure no tables are unused insted
        var beerPongTables = beerpongTableRepository.findAllByTournamentId(tournament.getId());
        var allOccupied = beerPongTables
            .stream()
            .allMatch(table -> table.getCurrentMatch() != null);
        assertTrue(allOccupied);
    }
}
