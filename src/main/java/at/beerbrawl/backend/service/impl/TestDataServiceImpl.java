/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.service.impl;

import at.beerbrawl.backend.endpoint.dto.TournamentUpdateQualificationMatchDto;
import at.beerbrawl.backend.entity.BeerPongTable;
import at.beerbrawl.backend.entity.QualificationMatch;
import at.beerbrawl.backend.entity.Team;
import at.beerbrawl.backend.entity.Tournament;
import at.beerbrawl.backend.entity.domainservice.MatchDomainService;
import at.beerbrawl.backend.repository.BeerPongTableRepository;
import at.beerbrawl.backend.repository.QualificationMatchRepository;
import at.beerbrawl.backend.repository.QualificationParticipationRepository;
import at.beerbrawl.backend.repository.TeamRepository;
import at.beerbrawl.backend.repository.TournamentRepository;
import at.beerbrawl.backend.repository.UserRepository;
import at.beerbrawl.backend.service.TestDataService;
import at.beerbrawl.backend.service.TournamentKoPhaseService;
import at.beerbrawl.backend.service.TournamentQualificationService;
import at.beerbrawl.backend.service.TournamentService;
import at.beerbrawl.backend.service.TournamentTeamService;
import at.beerbrawl.backend.util.BeerDateTime;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.IntStream;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class TestDataServiceImpl implements TestDataService {

    private final TournamentRepository tournamentRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TournamentTeamService teamService;
    private final TournamentService tournamentService;
    private final BeerPongTableRepository beerPongTableRepository;
    private final MatchDomainService matchDomainService;
    private final TournamentQualificationService qualificationService;
    private final TournamentKoPhaseService koPhaseService;
    private final QualificationParticipationRepository qualificationParticipationRepository;
    private final QualificationMatchRepository qualificationMatchRepository;

    @Override
    public void generateTestDataForUser(String username) {
        var tournaments = this.tournamentRepository.findAllByOrganizerUsername(username);
        tournamentRepository.deleteAllById(tournaments.stream().map(Tournament::getId).toList());

        final var tournament1 = new Tournament(
            "Semesterclosing Turnier",
            LocalDateTime.now().plusDays(3),
            32L,
            "Willkommen zum Semesterclosing Beerpongturnier! Viel Spaß! Es gibt tolle Preise zu gewinnen!",
            userRepository.findByUsername(username)
        );

        tournamentRepository.saveAllAndFlush(List.of(tournament1));
        var teamNames = getTeamNames();

        final var teams1 = IntStream.range(0, 32)
            .mapToObj(i -> new Team(teamNames[i], tournament1))
            .toList();
        teamRepository.saveAllAndFlush(teams1);

        var tableNames = List.of("Innen1", "Innen2", "Innen3", "Terasse");
        var tables1 = IntStream.range(0, 4)
            .mapToObj(i -> new BeerPongTable(tableNames.get(i), tournament1))
            .toList();

        beerPongTableRepository.saveAllAndFlush(tables1);
        generateTournamentWithFinishedQualiPhaseAndDifferentScores(username);
    }

    protected void generateTournamentWithFinishedQualiPhaseAndDifferentScores(String username) {
        var tournament = new Tournament(
            "Ferienturnier",
            BeerDateTime.nowUtc().plusDays(1),
            40L,
            "Willkommen zum Ferienturnier! Viel Spaß! Es gibt tolle Preise zu gewinnen!",
            userRepository.findByUsername(username)
        );
        tournamentService.create(tournament, username);

        generate32Teams(tournament);

        qualificationService.generateQualificationMatchesForTournament(
            tournament.getId(),
            username
        );

        markAllTeamsAsReady(tournament);

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
        for (var i = 0; i < qMatches.size() - 1; i++) {
            var qm = qMatches.get(i);
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

        var tableNames = List.of("Tisch 1", "Tisch 2", "Tisch 3");
        var tables1 = IntStream.range(0, 3)
            .mapToObj(i -> new BeerPongTable(tableNames.get(i), tournament))
            .toList();
        beerPongTableRepository.saveAllAndFlush(tables1);

        matchDomainService.scheduleQualiMatches(tournament.getId());
    }

    protected void generate32Teams(Tournament tournament) {
        var teamNames = getTeamNames();
        for (int i = 1; i <= 32; i++) {
            var result = teamService.signupTeamForTournament(
                tournament.getId(),
                tournament.getPublicAccessToken(),
                teamNames[i]
            );
            if (result != Tournament.SignupTeamResult.SUCCESS) {
                throw new IllegalStateException("Failed to sign up team for tournament");
            }
        }
    }

    protected void markAllTeamsAsReady(Tournament tournament) {
        for (final var team : teamRepository.findAllByTournamentId(tournament.getId())) {
            teamService.markTeamAsReady(tournament.getId(), team.getId());
        }
    }

    protected void markParticipantsAsDrinksCollected(Tournament tournament, QualificationMatch qm) {
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

    protected WinnerInfo determineWinner(
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

    private static class WinnerInfo {

        Long winnerId;
        Long winnerPoints;

        WinnerInfo(Long winnerId, Long winnerPoints) {
            this.winnerId = winnerId;
            this.winnerPoints = winnerPoints;
        }
    }

    protected Long getWinnerPoints(
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

    String[] getTeamNames() {
        return new String[] {
            "Pongmeister",
            "Bierathleten",
            "Becherstürmer",
            "Hopfenhüpfer",
            "PongProfis",
            "Bierbuddies",
            "Schaumjäger",
            "Ponghelden",
            "Becherritter",
            "Bierwerfer",
            "Bierbongers",
            "Becherbullen",
            "Braumeister",
            "PongPioniere",
            "Bieronauten",
            "Bechermagier",
            "Hopfenheroes",
            "Bierflieger",
            "Pongkönige",
            "Braubrüder",
            "Becherblitz",
            "BierballKrieger",
            "PingpongPrinzen",
            "Bierkapitäne",
            "Pongpiraten",
            "Becherbarden",
            "BierballBrigade",
            "Schaumstürmer",
            "Pongprofs",
            "HopfenHüpfende",
            "BierballBataillon",
            "Pongpartisanen",
            "Becherbomber",
            "Braukrieger",
            "BierballHexer",
            "Pongpropheten",
            "Becherbarone",
            "Hopfenhaie",
            "BierballFestung",
            "Pongpäpste",
            "Becherbären",
            "Bierbaronen",
            "Pongpanther",
            "Becherbrigade",
            "SchaumSchützen",
            "BierballTruppe",
            "Pongpatrioten",
            "Bechergarde",
            "Hopfenhexer",
            "BierballBauern",
            "Pongpiloten",
            "Becherbazis",
            "BrauBanditen",
            "BierballBosse",
            "Becherbrecher",
            "Bierbären",
            "Schaumkrieger",
            "Pongplünderer",
            "Becherbosse",
            "Hopfenhelden",
            "BierballKompanie",
            "Pongprinzen",
            "Becherblitzer",
            "Braublitz",
            "BierballBomber",
            "Pongchamps",
            "Becherbuddies",
            "Bierbrüder",
            "Schaumjäger",
            "Pongpiloten",
            "Bechermagier",
            "Bierbarone",
            "Hopfenhengste",
            "BierballBande",
            "Becherbarone",
            "Schaumstürmer",
            "Bierkapitäne",
            "Pongköniginnen",
            "Becherballer",
            "Braukapitäne",
            "BierballBerserker",
            "Pongmeister",
            "Becherhelden",
            "Hopfenhüpfer",
            "Bieronauten",
            "Pongprofs",
            "Becherwächter",
            "Schaumritter",
            "BierballBotsch",
            "Ponghelden",
            "Becherblitz",
            "Braumeister",
            "BierballZauberer",
            "Pongchampions",
            "Becherbomber",
            "SchaumSchützen",
        };
    }
}
