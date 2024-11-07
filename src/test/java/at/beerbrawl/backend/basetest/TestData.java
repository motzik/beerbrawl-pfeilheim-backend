/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.basetest;

import at.beerbrawl.backend.datagenerator.TestDataGenerator;
import at.beerbrawl.backend.endpoint.dto.TournamentUpdateQualificationMatchDto;
import at.beerbrawl.backend.entity.QualificationMatch;
import at.beerbrawl.backend.entity.Tournament;
import at.beerbrawl.backend.entity.Tournament.SignupTeamResult;
import at.beerbrawl.backend.entity.domainservice.MatchDomainService;
import at.beerbrawl.backend.repository.BeerPongTableRepository;
import at.beerbrawl.backend.repository.QualificationMatchRepository;
import at.beerbrawl.backend.repository.QualificationParticipationRepository;
import at.beerbrawl.backend.repository.SharedMediaRepository;
import at.beerbrawl.backend.repository.TeamRepository;
import at.beerbrawl.backend.repository.TournamentRepository;
import at.beerbrawl.backend.repository.UserRepository;
import at.beerbrawl.backend.service.TournamentKoPhaseService;
import at.beerbrawl.backend.service.TournamentQualificationService;
import at.beerbrawl.backend.service.TournamentService;
import at.beerbrawl.backend.service.TournamentTeamService;
import at.beerbrawl.backend.util.BeerDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class TestData {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    public TeamRepository teamRepository;

    @Autowired
    private QualificationMatchRepository qualificationMatchRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    protected TournamentService tournamentService;

    @Autowired
    private TournamentQualificationService qualificationService;

    @Autowired
    private TournamentTeamService teamService;

    @Autowired
    private BeerPongTableRepository beerPongTableRepository;

    @Autowired
    private MatchDomainService matchDomainService;

    @Autowired
    private TournamentQualificationService tournamentQualificationService;

    @Autowired
    private TournamentKoPhaseService koPhaseService;

    @Autowired
    private QualificationParticipationRepository qualificationParticipationRepository;

    @Autowired
    private SharedMediaRepository sharedMediaRepository;

    protected String BASE_URI = "/api/v1";
    protected String TOURNAMENT_BASE_URI = BASE_URI + "/tournaments";
    protected String BEER_PONG_TABLE_BASE_URI = BASE_URI + "/beer-pong-tables";

    protected String TEST_USER = "testUser";
    protected List<String> TEST_USER_ROLES = new ArrayList<>() {
        {
            add("ROLE_USER");
        }
    };

    protected Tournament generateTournamentWithQualificationMatches() {
        var tournament = new Tournament(
            "testname",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "testdescription",
            userRepository.findByUsername(TEST_USER)
        );
        tournamentService.create(tournament, TEST_USER);

        generate16Teams(tournament);

        qualificationService.generateQualificationMatchesForTournament(
            tournament.getId(),
            TEST_USER
        );
        return tournament;
    }

    private void generate16Teams(Tournament tournament) {
        for (int i = 1; i <= 16; i++) {
            var result = teamService.signupTeamForTournament(
                tournament.getId(),
                tournament.getPublicAccessToken(),
                "team" + i
            );
            if (result != SignupTeamResult.SUCCESS) {
                throw new IllegalStateException("Failed to sign up team for tournament");
            }
        }
    }

    protected Tournament generateTournamentWithFinishedQualiPhase() {
        var tournament = this.generateTournamentWithQualificationMatches();
        setAllTeamsReadyBypassingScheduling(tournament);
        finishQuali(tournament);
        return tournament;
    }

    private void finishQuali(Tournament tournament) {
        // finish qualification matches, don't care about the results
        final var qMatches = qualificationMatchRepository.findAllByTournamentId(tournament.getId());
        for (var qm : qMatches) {
            // mark all match participants as drinks collected
            qm
                .getParticipations()
                .stream()
                .map(p -> p.getTeam().getId())
                .forEach(
                    p ->
                        qualificationService.updateQualificationMatch(
                            tournament.getId(),
                            qm.getId(),
                            new TournamentUpdateQualificationMatchDto(
                                null,
                                new TournamentUpdateQualificationMatchDto.DrinksPickupDto(p)
                            )
                        )
                );

            // set first as winner
            qualificationService.updateQualificationMatch(
                tournament.getId(),
                qm.getId(),
                new TournamentUpdateQualificationMatchDto(
                    new TournamentUpdateQualificationMatchDto.ScoreUpdateDto(
                        qm.getParticipations().get(0).getTeam().getId(),
                        1L
                    ),
                    null
                )
            );
        }
    }

    /**
     * Sets all teams ready, but without the side effect of doScheduleQualiMatches.
     * This is useful in testing, as scheduling is not necessary for most tests.
     */
    public void setAllTeamsReadyBypassingScheduling(Tournament tournament) {
        for (final var team : teamRepository.findAllByTournamentId(tournament.getId())) {
            team.checkIn();
            teamRepository.saveAndFlush(team);
        }
    }

    protected Tournament generateTournamentWithFinishedQualiPhaseAndDifferentScores() {
        var tournament = this.generateTournamentWithQualificationMatches();

        markAllTeamsAsReady(tournament);

        /*
         * We want to have a score table like this:
         * First of all we want one team that wins 2 matches and one team that looses 2 matches.
         * In between all the teams should have 1 win and 1 loss.
         *      -   In the middle of these teams there are 4 teams with exactly 5 points. So they share the places
         *          15, 16, 17 and 18. In a real tournament it is not clear who will be qualified for the ko phase,
         *          so the organizer has to pick 2 of them.
         *
         *      -   All the other teams are split into two groups with the same amount of teams. One group has more
         *          than 5 points and the other group has less than 5 points. The exact amount of points is random.
         */

        var alwaysWinsId = teamRepository.findAllByTournamentId(tournament.getId()).get(0).getId();
        var alwaysLoosesId = teamRepository
            .findAllByTournamentId(tournament.getId())
            .get(1)
            .getId();

        // all the team ids that have already won one match
        var teamsWithOneWin = new LinkedList<Long>();

        // we also track the teams in the specific
        var teamsWith5Points = new LinkedList<Long>();
        var teamsWithMoreThan5Points = new LinkedList<Long>();
        var teamsWithLessThan5Points = new LinkedList<Long>();

        final var qMatches = qualificationMatchRepository.findAllByTournamentId(tournament.getId());
        for (var qm : qMatches) {
            markParticipantsAsDrinksCollected(tournament, qm);

            var winnerInfo = determineWinner(
                qm,
                alwaysWinsId,
                alwaysLoosesId,
                teamsWithOneWin,
                teamsWith5Points,
                teamsWithMoreThan5Points,
                teamsWithLessThan5Points
            );

            qualificationService.updateQualificationMatch(
                tournament.getId(),
                qm.getId(),
                new TournamentUpdateQualificationMatchDto(
                    new TournamentUpdateQualificationMatchDto.ScoreUpdateDto(
                        winnerInfo.winnerId,
                        winnerInfo.winnerPoints
                    ),
                    null
                )
            );
        }

        return tournament;
    }

    private void markAllTeamsAsReady(Tournament tournament) {
        for (final var team : teamRepository.findAllByTournamentId(tournament.getId())) {
            teamService.markTeamAsReady(tournament.getId(), team.getId());
        }
    }

    private void markParticipantsAsDrinksCollected(Tournament tournament, QualificationMatch qm) {
        qm
            .getParticipations()
            .stream()
            .map(p -> p.getTeam().getId())
            .forEach(
                p ->
                    qualificationService.updateQualificationMatch(
                        tournament.getId(),
                        qm.getId(),
                        new TournamentUpdateQualificationMatchDto(
                            null,
                            new TournamentUpdateQualificationMatchDto.DrinksPickupDto(p)
                        )
                    )
            );
    }

    private WinnerInfo determineWinner(
        QualificationMatch qm,
        Long alwaysWinsId,
        Long alwaysLoosesId,
        LinkedList<Long> teamsWithOneWin,
        LinkedList<Long> teamsWith5Points,
        LinkedList<Long> teamsWithMoreThan5Points,
        LinkedList<Long> teamsWithLessThan5Points
    ) {
        var currentTeamIds = qm.getParticipations().stream().map(p -> p.getTeam().getId()).toList();
        Long winnerId;
        Long winnerPoints;

        if (currentTeamIds.contains(alwaysWinsId)) {
            // the match includes the "alwaysWins" team, so it wins
            winnerId = alwaysWinsId;
            winnerPoints = 10L;
        } else if (currentTeamIds.contains(alwaysLoosesId)) {
            // the match includes the "alwaysLooses" team, so it looses
            winnerId = currentTeamIds
                .stream()
                .filter(id -> !Objects.equals(id, alwaysLoosesId))
                .findFirst()
                .get();
            teamsWithOneWin.add(winnerId);
            winnerPoints = getWinnerPoints(
                teamsWith5Points,
                teamsWithMoreThan5Points,
                teamsWithLessThan5Points,
                winnerId
            );
        } else {
            var teamWithOneWin = currentTeamIds
                .stream()
                .filter(teamsWithOneWin::contains)
                .findFirst();
            if (teamWithOneWin.isPresent()) {
                //the match includes a team that has already won one match, so the other team wins
                winnerId = currentTeamIds
                    .stream()
                    .filter(id -> !Objects.equals(id, teamWithOneWin.get()))
                    .findFirst()
                    .get();
            } else {
                // the match includes two teams that have not won a match yet, so we just pick the first one
                winnerId = currentTeamIds.getFirst();
            }
            teamsWithOneWin.add(winnerId);
            winnerPoints = getWinnerPoints(
                teamsWith5Points,
                teamsWithMoreThan5Points,
                teamsWithLessThan5Points,
                winnerId
            );
        }

        return new WinnerInfo(winnerId, winnerPoints);
    }

    private static Long getWinnerPoints(
        LinkedList<Long> teamsWith5Points,
        LinkedList<Long> teamsWithMoreThan5Points,
        LinkedList<Long> teamsWithLessThan5Points,
        Long winnerId
    ) {
        Long winnerPoints;
        if (teamsWith5Points.size() < 4) {
            // it joins the group with exactly 5 points
            winnerPoints = 5L;
            teamsWith5Points.add(winnerId);
        } else {
            if (teamsWithMoreThan5Points.size() < teamsWithLessThan5Points.size()) {
                // it joins the group with more than 5 points
                // random number from 1 to 4 (both inclusive)
                winnerPoints = 1L + new Random().nextInt(4);
                teamsWithMoreThan5Points.add(winnerId);
            } else {
                // it joins the group with less than 5 points
                // random number from 6 to 9 (both inclusive)
                winnerPoints = 6L + new Random().nextInt(4);
                teamsWithLessThan5Points.add(winnerId);
            }
        }
        return winnerPoints;
    }

    @BeforeEach
    public void seedDatabase() {
        userRepository.deleteAll(); // cascading should clear all data
        var dataGenerator = new TestDataGenerator(
            passwordEncoder,
            userRepository,
            teamRepository,
            tournamentRepository,
            tournamentService,
            teamService,
            beerPongTableRepository,
            matchDomainService,
            tournamentQualificationService,
            this.qualificationService,
            this.koPhaseService,
            this.qualificationParticipationRepository,
            sharedMediaRepository
        );
        dataGenerator.generateTestUser();
        dataGenerator.generateTestTournaments();
    }

    private static class WinnerInfo {

        Long winnerId;
        Long winnerPoints;

        WinnerInfo(Long winnerId, Long winnerPoints) {
            this.winnerId = winnerId;
            this.winnerPoints = winnerPoints;
        }
    }
}
