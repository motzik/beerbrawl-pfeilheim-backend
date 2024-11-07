/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.datagenerator;

import at.beerbrawl.backend.BackendApplication;
import at.beerbrawl.backend.endpoint.dto.TournamentUpdateQualificationMatchDto;
import at.beerbrawl.backend.endpoint.dto.TournamentUpdateQualificationMatchDto.DrinksPickupDto;
import at.beerbrawl.backend.endpoint.dto.TournamentUpdateQualificationMatchDto.ScoreUpdateDto;
import at.beerbrawl.backend.entity.ApplicationUser;
import at.beerbrawl.backend.entity.BeerPongTable;
import at.beerbrawl.backend.entity.SharedMedia;
import at.beerbrawl.backend.entity.Team;
import at.beerbrawl.backend.entity.Tournament;
import at.beerbrawl.backend.entity.domainservice.MatchDomainService;
import at.beerbrawl.backend.enums.MediaState;
import at.beerbrawl.backend.repository.BeerPongTableRepository;
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
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.stream.IntStream;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Profile("generateData")
@Component
@AllArgsConstructor
public class TestDataGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        MethodHandles.lookup().lookupClass()
    );
    public static final String TEST_USER = "testUser";
    public static final String TEST_USER_PASSWORD = "test_user_password";

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentService tournamentService;
    private final TournamentTeamService teamService;
    private final BeerPongTableRepository beerPongTableRepository;
    private final MatchDomainService matchDomainService;
    private final TournamentQualificationService tournamentQualificationService;
    private final TournamentQualificationService qualificationService;
    private final TournamentKoPhaseService koPhaseService;
    private final QualificationParticipationRepository qualificationParticipationRepository;
    private final SharedMediaRepository sharedMediaRepository;

    public void generateTestUser() {
        userRepository.deleteAll();
        userRepository.flush();

        LOGGER.debug("generating {} test user", TEST_USER);

        var newUser = new ApplicationUser(
            TEST_USER,
            passwordEncoder.encode(TEST_USER_PASSWORD),
            false
        );
        userRepository.save(newUser);
    }

    public void generateTestTournaments() {
        LOGGER.debug("generating TEST_TOURNAMENT tournament");

        final var tournament = new Tournament(
            "TEST_TOURNAMENT",
            BeerDateTime.nowUtc().plusDays(7),
            64L,
            "TEST_TOURNAMENT_DESCRIPTION",
            userRepository.findByUsername(TEST_USER)
        );
        final var tournament2 = new Tournament(
            "TEST_TOURNAMENT2",
            BeerDateTime.nowUtc().plusDays(7),
            32L,
            "TEST_TOURNAMENT2_DESCRIPTION",
            userRepository.findByUsername(TEST_USER)
        );
        final var tournament3 = new Tournament(
            "TEST_TOURNAMENT3",
            BeerDateTime.nowUtc().plusDays(7),
            32L,
            "TEST_TOURNAMENT3_DESCRIPTION",
            userRepository.findByUsername(TEST_USER)
        );

        tournamentRepository.saveAllAndFlush(List.of(tournament, tournament2, tournament3));

        final var teams1 = IntStream.range(0, 64)
            .mapToObj(i -> new Team("Test Team #%02d".formatted(i), tournament))
            .toList();
        teamRepository.saveAllAndFlush(teams1);

        var teams2 = IntStream.range(0, 32)
            .mapToObj(i -> new Team("Test Team #%02d".formatted(i), tournament2))
            .toList();
        teams2 = teamRepository.saveAllAndFlush(teams2);
        teams2.forEach(t -> teamService.markTeamAsReady(tournament2.getId(), t.getId()));

        var teams3 = IntStream.range(0, 32)
            .mapToObj(i -> new Team("Test Team #%02d".formatted(i), tournament3))
            .toList();
        teams3 = teamRepository.saveAllAndFlush(teams3);
        teams3.forEach(t -> teamService.markTeamAsReady(tournament3.getId(), t.getId()));

        tournamentQualificationService.generateQualificationMatchesForTournament(
            tournament3.getId(),
            TEST_USER
        );
        var tables3 = IntStream.range(0, 4)
            .mapToObj(i -> new BeerPongTable("Test Table #%02d".formatted(i), tournament3))
            .toList();
        beerPongTableRepository.saveAllAndFlush(tables3);
        matchDomainService.scheduleQualiMatches(tournament3.getId());

        addTestImagesToTournament(tournament);
        addTestImagesToTournament(tournament2);
        addTestImagesToTournament(tournament3);
    }

    private void addTestImagesToTournament(Tournament tournament) {
        try {
            uploadImageToTournament(
                tournament,
                "John Smith",
                "World Chess Championship",
                "testimage.png"
            );
            uploadImageToTournament(
                tournament,
                "Emily Johnson",
                "Grand Slam Tennis Tournament",
                "testimage.png"
            );
            uploadImageToTournament(
                tournament,
                "Michael Brown",
                "Olympic Games 2024",
                "testimage.png"
            );
            uploadImageToTournament(
                tournament,
                "Jessica Martinez",
                "FIFA World Cup",
                "testimage.png"
            );
            uploadImageToTournament(tournament, "David Garcia", "NBA Finals", "testimage.png");
            uploadImageToTournament(tournament, "Jane Doe", "Super Bowl", "testimage.png");
            uploadImageToTournament(
                tournament,
                "Andrew Wilson",
                "Rugby World Cup",
                "testimage.png"
            );
            uploadImageToTournament(tournament, "Sophia Lee", "Australian Open", "testimage.png");
            uploadImageToTournament(
                tournament,
                "Ethan Clark",
                "UEFA Champions League",
                "testimage.png"
            );
            uploadImageToTournament(tournament, "Olivia Scott", "Wimbledon", "testimage.png");
        } catch (IOException e) {
            LOGGER.error("Failed to upload test images to tournament {}", tournament.getName(), e);
        }
    }

    private void uploadImageToTournament(
        Tournament tournament,
        String author,
        String title,
        String imagePath
    ) throws IOException {
        var inputStream = BackendApplication.class.getClassLoader().getResourceAsStream(imagePath);
        byte[] imageBytes = null;
        if (inputStream != null) {
            imageBytes = inputStream.readAllBytes();
            inputStream.close();
        }

        SharedMedia sharedMedia = new SharedMedia();
        sharedMedia.setAuthor(author);
        sharedMedia.setTitle(title);
        sharedMedia.setImage(imageBytes);
        sharedMedia.setTournament(tournament);
        sharedMedia.setState(MediaState.PENDING);

        sharedMediaRepository.saveAndFlush(sharedMedia);
    }

    private void generateTestTournamentsWithFinishedQualificationAndStartedKoPhase() {
        final var tournament = tournamentService.create(
            new Tournament(
                "TEST_TOURNAMENT4",
                BeerDateTime.nowUtc().plusDays(1),
                32L,
                "testdescription",
                null
            ),
            TEST_USER
        );
        var teams = IntStream.range(0, 32)
            .mapToObj(i -> new Team("Test Team #%02d".formatted(i), tournament))
            .toList();
        teamRepository.saveAllAndFlush(teams);
        var qualificationMatches = qualificationService.generateQualificationMatchesForTournament(
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
            qualificationService.updateQualificationMatch(
                tournament.getId(),
                qualificationMatch.getId(),
                new TournamentUpdateQualificationMatchDto(null, new DrinksPickupDto(team1))
            );
            qualificationService.updateQualificationMatch(
                tournament.getId(),
                qualificationMatch.getId(),
                new TournamentUpdateQualificationMatchDto(null, new DrinksPickupDto(team2))
            );
            qualificationService.updateQualificationMatch(
                tournament.getId(),
                qualificationMatch.getId(),
                new TournamentUpdateQualificationMatchDto(new ScoreUpdateDto(team1, 10L), null)
            );
        }
        koPhaseService.generateKoMatchesForTournament(
            tournament.getId(),
            teams.stream().limit(16).map(Team::getId).toList(),
            TEST_USER
        );

        var tables = IntStream.range(0, 4)
            .mapToObj(i -> new BeerPongTable("Test Table #%02d".formatted(i), tournament))
            .toList();
        beerPongTableRepository.saveAllAndFlush(tables);
        matchDomainService.scheduleQualiMatches(tournament.getId());
        matchDomainService.scheduleKoMatches(tournament.getId());
    }

    @PostConstruct
    private void generateTestData() {
        generateTestUser();
        generateTestTournaments();
        generateTestTournamentsWithFinishedQualificationAndStartedKoPhase();
    }
}
