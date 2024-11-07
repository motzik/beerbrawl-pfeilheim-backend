/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.integrationtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import at.beerbrawl.backend.basetest.TestData;
import at.beerbrawl.backend.config.properties.SecurityProperties;
import at.beerbrawl.backend.endpoint.dto.KoStandingDto;
import at.beerbrawl.backend.endpoint.dto.TournamentUpdateKoStandingDto;
import at.beerbrawl.backend.endpoint.dto.TournamentUpdateQualificationMatchDto;
import at.beerbrawl.backend.endpoint.dto.TournamentUpdateQualificationMatchDto.DrinksPickupDto;
import at.beerbrawl.backend.endpoint.dto.TournamentUpdateQualificationMatchDto.ScoreUpdateDto;
import at.beerbrawl.backend.endpoint.dto.UserLoginDto;
import at.beerbrawl.backend.entity.KoStanding;
import at.beerbrawl.backend.entity.Team;
import at.beerbrawl.backend.entity.Tournament;
import at.beerbrawl.backend.repository.KoStandingsRepository;
import at.beerbrawl.backend.repository.QualificationParticipationRepository;
import at.beerbrawl.backend.repository.TeamRepository;
import at.beerbrawl.backend.security.JwtTokenizer;
import at.beerbrawl.backend.service.TournamentKoPhaseService;
import at.beerbrawl.backend.service.TournamentQualificationService;
import at.beerbrawl.backend.service.TournamentService;
import at.beerbrawl.backend.service.TournamentTeamService;
import at.beerbrawl.backend.service.models.TeamModel;
import at.beerbrawl.backend.util.BeerDateTime;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Stack;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class TournamentKoPhaseEndpointTest extends TestData {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenizer jwtTokenizer;

    @Autowired
    private SecurityProperties securityProperties;

    @Autowired
    private TournamentService tournamentService;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TournamentQualificationService qualificationService;

    @Autowired
    private TournamentKoPhaseService koPhaseService;

    @Autowired
    private KoStandingsRepository koStandingsRepository;

    @Autowired
    private QualificationParticipationRepository qualificationParticipationRepository;

    @Autowired
    private TournamentTeamService teamService;

    @Test
    public void getKoStandingsForTournamentThatDoesntExist() throws Exception {
        var mvcResult =
            this.mockMvc.perform(
                    get(String.format("%s/tournaments/%d/ko-standings", BASE_URI, -1)).header(
                        securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                    )
                )
                .andDo(print())
                .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
    }

    @Test
    public void getKoStandingsForTournamentWhileUnauthenticated() throws Exception {
        var mvcResult =
            this.mockMvc.perform(get(String.format("%s/tournaments/%d/ko-standings", BASE_URI, -1)))
                .andDo(print())
                .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertNotEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
    }

    @Test
    public void tryUpdateKoStandingsForTournamentBelongingToOtherUser() throws Exception {
        // register new account
        var dto = UserLoginDto.UserLoginDtoBuilder.anUserLoginDto()
            .withUsername("foo")
            .withPassword("12345678")
            .build();

        var response = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/user/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(this.objectMapper.writeValueAsString(dto))
            )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();

        final var fooBearer = response.getContentAsString();
        assertThat(fooBearer).isNotNull().hasSizeGreaterThanOrEqualTo(1).startsWith("Bearer ");

        // generate new tournament
        final var tournament = this.generateTournamentWithFinishedQualiPhase();
        final var teams = teamService.getTournamentTeams(tournament.getId());
        koPhaseService.generateKoMatchesForTournament(
            tournament.getId(),
            teams.stream().limit(16).map(TeamModel::id).toList(),
            TEST_USER
        );

        final var standings = koPhaseService.getKoStandingsTree(tournament.getId());
        final var firstMatch = standings.getInitialStandings().findFirst().get();

        final var updateDto = new TournamentUpdateKoStandingDto(
            new TournamentUpdateKoStandingDto.SetWinnerTeamDto(firstMatch.getTeam().getId()),
            null
        );

        var mvcResult =
            this.mockMvc.perform(
                    put(
                        String.format(
                            "%s/tournaments/%d/ko-standings/%d",
                            BASE_URI,
                            tournament.getId(),
                            firstMatch.getNextStanding().getId()
                        )
                    )
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(securityProperties.getAuthHeader(), fooBearer)
                        .content(this.objectMapper.writeValueAsString(updateDto))
                )
                .andDo(print())
                .andReturn();

        response = mvcResult.getResponse();
        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
    }

    @Test
    public void getKoStandingsForTournamentWith32ParticipatingTeams() throws Exception {
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
        var flattenedGeneratedMatches = koStandingsRepository.getAllByTournamentId(
            tournament.getId()
        );

        var mvcResult =
            this.mockMvc.perform(
                    get(
                        String.format(
                            "%s/tournaments/%d/ko-standings",
                            BASE_URI,
                            tournament.getId()
                        )
                    ).header(
                        securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                    )
                )
                .andDo(print())
                .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertEquals(HttpStatus.OK.value(), response.getStatus());

        var fetchedMatches = objectMapper.readValue(
            response.getContentAsString(),
            KoStandingDto.class
        );

        var flattenedFetchedMatches = new ArrayList<KoStandingDto>();
        var fetchedStack = new Stack<KoStandingDto>();
        fetchedStack.push(fetchedMatches);
        while (!fetchedStack.isEmpty()) {
            var match = fetchedStack.pop();
            flattenedFetchedMatches.add(match);
            for (var child : match.preceedingStandings()) {
                fetchedStack.push(child);
            }
        }

        assertEquals(flattenedFetchedMatches.size(), flattenedGeneratedMatches.size());
        for (var generatedMatch : flattenedGeneratedMatches) {
            var fetchedMatch = flattenedFetchedMatches
                .stream()
                .filter(m -> m.id() == generatedMatch.getId())
                .findFirst();
            assertTrue(fetchedMatch.isPresent());
            var fetchedMatchUnwrapped = fetchedMatch.get();

            assertAll(
                () -> {
                    if (generatedMatch.getTeam() == null || fetchedMatchUnwrapped.team() == null) {
                        assertEquals(generatedMatch.getTeam(), fetchedMatchUnwrapped.team());
                    } else {
                        assertEquals(
                            generatedMatch.getTeam().getId(),
                            fetchedMatchUnwrapped.team().id()
                        );
                    }
                },
                () ->
                    assertEquals(generatedMatch.getStartTime(), fetchedMatchUnwrapped.startTime()),
                () -> assertEquals(generatedMatch.getEndTime(), fetchedMatchUnwrapped.endTime()),
                () -> {
                    if (
                        generatedMatch.getPreceedingStandings() != null &&
                        fetchedMatchUnwrapped.preceedingStandings() != null
                    ) {
                        assertEquals(
                            generatedMatch.getPreceedingStandings().size(),
                            fetchedMatchUnwrapped.preceedingStandings().size()
                        );
                    }
                }
            );
        }
    }

    @Test
    public void setWinnerOfTournamentThatDoesntExistAndItThrows() throws Exception {
        var mvcResult =
            this.mockMvc.perform(
                    put(String.format("%s/tournaments/%d/ko-standings/%d", BASE_URI, -1, -1))
                        .header(
                            securityProperties.getAuthHeader(),
                            jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(
                                new TournamentUpdateKoStandingDto(
                                    new TournamentUpdateKoStandingDto.SetWinnerTeamDto(-1L),
                                    null
                                )
                            )
                        )
                )
                .andDo(print())
                .andReturn();
        var response = mvcResult.getResponse();
        var content = response.getContentAsString();

        assertAll(
            () -> assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus()),
            () -> assertEquals(content, "No tournament found.")
        );
    }

    @Test
    public void tryToChangeTeamOfFirstRoundStandingAndItThrows() throws Exception {
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
        var matchOfFirstRound = assertDoesNotThrow(
            () ->
                koStandingsRepository
                    .getAllByTournamentId(tournament.getId())
                    .stream()
                    .filter(m -> !m.hasPrecedingMatches())
                    .findFirst()
                    .orElseThrow(Exception::new)
        );

        var mvcResult =
            this.mockMvc.perform(
                    put(
                        String.format(
                            "%s/tournaments/%d/ko-standings/%d",
                            BASE_URI,
                            tournament.getId(),
                            matchOfFirstRound.getId()
                        )
                    )
                        .header(
                            securityProperties.getAuthHeader(),
                            jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(
                                new TournamentUpdateKoStandingDto(
                                    new TournamentUpdateKoStandingDto.SetWinnerTeamDto(
                                        teams.getFirst().getId()
                                    ),
                                    null
                                )
                            )
                        )
                )
                .andDo(print())
                .andReturn();
        var response = mvcResult.getResponse();
        var content = response.getContentAsString();

        assertAll(
            () -> assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus()),
            () ->
                assertEquals(
                    content,
                    "A precondition wasn't met: Team of first round can't be changed."
                )
        );
    }

    @Test
    public void setWinnerOfStandingWhenTheNextStandingAlreadyHasATeamAssignedAndItThrows()
        throws Exception {
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

        var mvcResult =
            this.mockMvc.perform(
                    put(
                        String.format(
                            "%s/tournaments/%d/ko-standings/%d",
                            BASE_URI,
                            tournament.getId(),
                            standingToUpdate.getId()
                        )
                    )
                        .header(
                            securityProperties.getAuthHeader(),
                            jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(
                                new TournamentUpdateKoStandingDto(
                                    new TournamentUpdateKoStandingDto.SetWinnerTeamDto(
                                        teams.getFirst().getId()
                                    ),
                                    null
                                )
                            )
                        )
                )
                .andDo(print())
                .andReturn();
        var response = mvcResult.getResponse();
        var content = response.getContentAsString();

        assertAll(
            () -> assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus()),
            () ->
                assertEquals(
                    content,
                    "A precondition wasn't met: Team of next standing isn't empty"
                )
        );
    }

    @Test
    public void setFinalKoPhaseWinnerWithoutSettingTheWinnersOfThePreviousRoundsAndItThrows()
        throws JsonProcessingException, Exception {
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
        var finalStandingOption =
            koStandingsRepository.findFinaleByTournamentIdAndNextStandingIsNull(tournament.getId());

        assertTrue(finalStandingOption.isPresent());
        var finalStanding = finalStandingOption.get();

        var mvcResult =
            this.mockMvc.perform(
                    put(
                        String.format(
                            "%s/tournaments/%d/ko-standings/%d",
                            BASE_URI,
                            tournament.getId(),
                            finalStanding.getId()
                        )
                    )
                        .header(
                            securityProperties.getAuthHeader(),
                            jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(
                                new TournamentUpdateKoStandingDto(
                                    new TournamentUpdateKoStandingDto.SetWinnerTeamDto(
                                        teams.getFirst().getId()
                                    ),
                                    null
                                )
                            )
                        )
                )
                .andDo(print())
                .andReturn();
        var response = mvcResult.getResponse();
        var content = response.getContentAsString();

        assertAll(
            () -> assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus()),
            () ->
                assertEquals(
                    "A precondition wasn't met: Team isn't assigned to a previous standing",
                    content
                )
        );
    }

    @Test
    public void setTeamAsWinnerThatHasntParticipatedInThePreceedingStandingsAndItThrows()
        throws Exception {
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

        var mvcResult =
            this.mockMvc.perform(
                    put(
                        String.format(
                            "%s/tournaments/%d/ko-standings/%d",
                            BASE_URI,
                            tournament.getId(),
                            standingToUpdate.getId()
                        )
                    )
                        .header(
                            securityProperties.getAuthHeader(),
                            jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(
                                new TournamentUpdateKoStandingDto(
                                    new TournamentUpdateKoStandingDto.SetWinnerTeamDto(
                                        teams
                                            .stream()
                                            .filter(
                                                t -> !teamIdsOfStandingToUpdate.contains(t.getId())
                                            )
                                            .findFirst()
                                            .get()
                                            .getId()
                                    ),
                                    null
                                )
                            )
                        )
                )
                .andDo(print())
                .andReturn();
        var response = mvcResult.getResponse();
        var content = response.getContentAsString();

        assertAll(
            () -> assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus()),
            () ->
                assertEquals(
                    "A precondition wasn't met: Team isn't assigned to a previous standing",
                    content
                )
        );
    }

    @Test
    public void setWinnerOfFirstRoundAndClearItAfterwards() throws Exception {
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

        final var standingToUpdate = koPhaseService
            .getKoStandingsTree(tournament.getId())
            .getInitialStandings()
            .findFirst()
            .get()
            .getNextStanding();

        // mark drinks as picked up
        for (final var preceding : standingToUpdate.getPreceedingStandings()) {
            this.mockMvc.perform(
                    put(
                        String.format(
                            "%s/tournaments/%d/ko-standings/%d",
                            BASE_URI,
                            tournament.getId(),
                            preceding.getId()
                        )
                    )
                        .header(
                            securityProperties.getAuthHeader(),
                            jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(
                                new TournamentUpdateKoStandingDto(
                                    null,
                                    new TournamentUpdateKoStandingDto.DrinksPickupDto(
                                        preceding.getTeam().getId()
                                    )
                                )
                            )
                        )
                )
                .andExpect(status().isNoContent())
                .andDo(print());
        }

        this.mockMvc.perform(
                put(
                    String.format(
                        "%s/tournaments/%d/ko-standings/%d",
                        BASE_URI,
                        tournament.getId(),
                        standingToUpdate.getId()
                    )
                )
                    .header(
                        securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
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
                    )
            )
            .andExpect(status().isNoContent())
            .andDo(print())
            .andReturn();

        this.mockMvc.perform(
                put(
                    String.format(
                        "%s/tournaments/%d/ko-standings/%d",
                        BASE_URI,
                        tournament.getId(),
                        standingToUpdate.getId()
                    )
                )
                    .header(
                        securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new TournamentUpdateKoStandingDto(
                                new TournamentUpdateKoStandingDto.SetWinnerTeamDto(null),
                                null
                            )
                        )
                    )
            )
            .andExpect(status().isNoContent())
            .andDo(print())
            .andReturn();
    }
}
