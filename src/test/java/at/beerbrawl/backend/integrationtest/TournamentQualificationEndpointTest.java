/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.integrationtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import at.beerbrawl.backend.basetest.TestData;
import at.beerbrawl.backend.config.properties.SecurityProperties;
import at.beerbrawl.backend.datagenerator.TestDataGenerator;
import at.beerbrawl.backend.endpoint.dto.QualificationTeamScoreDto;
import at.beerbrawl.backend.endpoint.dto.TeamDto;
import at.beerbrawl.backend.endpoint.dto.TournamentQualificationMatchDto;
import at.beerbrawl.backend.endpoint.dto.TournamentUpdateQualificationMatchDto;
import at.beerbrawl.backend.entity.QualificationMatch;
import at.beerbrawl.backend.entity.Team;
import at.beerbrawl.backend.entity.Tournament;
import at.beerbrawl.backend.repository.QualificationMatchRepository;
import at.beerbrawl.backend.repository.TeamRepository;
import at.beerbrawl.backend.repository.TournamentRepository;
import at.beerbrawl.backend.repository.UserRepository;
import at.beerbrawl.backend.security.JwtTokenizer;
import at.beerbrawl.backend.util.BeerDateTime;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class TournamentQualificationEndpointTest extends TestData {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenizer jwtTokenizer;

    @Autowired
    private SecurityProperties securityProperties;

    @Autowired
    private QualificationMatchRepository qualificationMatchRepository;

    @Autowired
    private TeamRepository teamRepository;

    /**
     * I could not get this test to run without @Transactional. ~Matthias
     * Currently relies on multiple repository calls to first fetch teams and then
     * assign them to matches. When not already within an outer transaction, this
     * would normally open multiple independent sessions. Hence attaching the (then
     * detached) winner to the match will crash.
     * That is why @Transactional was added. If possible use service methods already
     * for the setup when testing high-level functionality.
     * Mixing layers (also in tests) can lead to code being inflexible and
     * impossible to change.
     */
    @Test
    @Transactional
    public void getCorrectScoreTable() throws Exception {
        // setup
        var tournament = new Tournament(
            "Tournament 1",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "new Tournament",
            userRepository.findByUsername(TestDataGenerator.TEST_USER)
        );

        var numberOfTeams = 16;
        tournamentRepository.saveAndFlush(tournament);
        var teams = IntStream.range(0, numberOfTeams)
            .mapToObj(i -> new Team("Team" + Integer.toString(i), tournament))
            .map(teamRepository::save)
            .toList();

        var team0 = teams.get(0);
        var team1 = teams.get(1);
        var team2 = teams.get(2);
        var team3 = teams.get(3);
        var team4 = teams.get(4);
        var team5 = teams.get(5);
        var team10 = teams.get(10);

        //team 1 vs team 2 -> team 1 wins(9 points)
        createQualificationMatch(tournament, team1, team2, 9L);

        //team 1 vs team 3 -> team 1 wins(2 points)
        createQualificationMatch(tournament, team1, team3, 2L);

        //team 3 vs team 4 -> team 3 wins(6 points)
        createQualificationMatch(tournament, team3, team4, 6L);

        //team 2 vs team 4 -> team 2 wins(1 points)
        createQualificationMatch(tournament, team2, team4, 1L);

        //team 2 vs team 5 -> team 2 wins(1 points)
        createQualificationMatch(tournament, team2, team5, 1L);

        //summary (sorted by wins, then by points):
        //team 1: 2 wins, 11 points
        //team 2: 2 wins, 2 points
        //team 3: 1 win, 6 points
        //team 4: 0 wins, 0 points

        //when
        var mvcResult =
            this.mockMvc.perform(
                    get(
                        String.format(
                            "%s/%d/qualification-phase/scores",
                            TOURNAMENT_BASE_URI,
                            tournament.getId()
                        )
                    )
                        .header(
                            securityProperties.getAuthHeader(),
                            jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        //then
        var response = mvcResult.getResponse();
        var scoreBoard = objectMapper
            .readerFor(QualificationTeamScoreDto.class)
            .<QualificationTeamScoreDto>readValues(response.getContentAsString())
            .readAll();

        //position 1: team 1(2 wins, 11 points)
        var entry1 = scoreBoard.get(0);
        assertScoreBoardEntry(entry1, team1, 2L, 11L, 1L);

        //position 2: team 2(2 wins, 2 points)
        var entry2 = scoreBoard.get(1);
        assertScoreBoardEntry(entry2, team2, 2L, 2L, 2L);

        //position 3: team 3(1 win, 6 points)
        var entry3 = scoreBoard.get(2);
        assertScoreBoardEntry(entry3, team3, 1L, 6L, 3L);

        //position 4: team 0(0 wins, 0 points) -> team 0 because it is sorted by name
        var entry4 = scoreBoard.get(3);
        assertScoreBoardEntry(entry4, team0, 0L, 0L, 4L);

        //position 5: team 10(0 wins, 0 points) -> team 10 because it is sorted by name
        var entry5 = scoreBoard.get(4);
        assertScoreBoardEntry(entry5, team10, 0L, 0L, 4L);
    }

    private static void assertScoreBoardEntry(
        QualificationTeamScoreDto scoreBoardEntry1,
        Team team1,
        Long wins,
        Long points,
        Long position
    ) {
        if (scoreBoardEntry1 != null) {
            assertEquals(team1.getName(), scoreBoardEntry1.name());
        }
        if (wins != null) {
            assertEquals(wins, scoreBoardEntry1.wins());
        }
        if (points != null) {
            assertEquals(points, scoreBoardEntry1.points());
        }
        if (position != null) {
            assertEquals(position, scoreBoardEntry1.position());
        }
    }

    private void createQualificationMatch(
        Tournament tournament,
        Team winningTeam,
        Team loosingTeam,
        Long winnerPoints
    ) {
        var match1 = new QualificationMatch(tournament, List.of(winningTeam, loosingTeam));
        match1.setWinner(winningTeam);
        match1.setWinnerPoints(winnerPoints);
        qualificationMatchRepository.saveAndFlush(match1);
    }

    @Test
    public void qualificationMatchMarkBothTeamsAsDrinksPickedUpShouldStartMatch() throws Exception {
        final var tournament = generateTournamentWithQualificationMatches();
        final var match = qualificationMatchRepository
            .findAllByTournamentId(tournament.getId())
            .get(0);

        // mark teams as ready
        for (final var participant : match.getParticipations()) {
            this.mockMvc.perform(
                    post(
                        TOURNAMENT_BASE_URI + "/{tournamentId}/teams/{teamId}/ready",
                        tournament.getId(),
                        participant.getTeam().getId()
                    )
                        .header(
                            securityProperties.getAuthHeader(),
                            jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNoContent())
                .andReturn();
        }

        // mark drinks as picked up
        for (final var participant : match.getParticipations()) {
            final var updateDto = new TournamentUpdateQualificationMatchDto(
                null,
                new TournamentUpdateQualificationMatchDto.DrinksPickupDto(
                    participant.getTeam().getId()
                )
            );

            this.mockMvc.perform(
                    put(
                        TOURNAMENT_BASE_URI + "/{tournamentId}/qualification-matches/{matchId}",
                        tournament.getId(),
                        match.getId()
                    )
                        .header(
                            securityProperties.getAuthHeader(),
                            jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        }

        final var response =
            this.mockMvc.perform(
                    get(
                        TOURNAMENT_BASE_URI + "/{tournamentId}/qualification-matches",
                        tournament.getId()
                    )
                        .header(
                            securityProperties.getAuthHeader(),
                            jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        final var result = Arrays.stream(
            objectMapper.readValue(
                response.getContentAsString(),
                TournamentQualificationMatchDto[].class
            )
        )
            .filter(m -> Objects.equals(m.id(), match.getId()))
            .findFirst()
            .orElseThrow();

        assertNotNull(result.startTime());
    }

    @Test
    public void teamIsMarkedCurrentlyPlayingWhenParticipatingInRunningQualificationMatch()
        throws Exception {
        final var tournament = generateTournamentWithQualificationMatches();
        final var match = qualificationMatchRepository
            .findAllByTournamentId(tournament.getId())
            .get(0);

        // mark teams as ready
        for (final var participant : match.getParticipations()) {
            this.mockMvc.perform(
                    post(
                        TOURNAMENT_BASE_URI + "/{tournamentId}/teams/{teamId}/ready",
                        tournament.getId(),
                        participant.getTeam().getId()
                    )
                        .header(
                            securityProperties.getAuthHeader(),
                            jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNoContent())
                .andReturn();
        }

        // mark drinks as picked up
        for (final var participant : match.getParticipations()) {
            final var updateDto = new TournamentUpdateQualificationMatchDto(
                null,
                new TournamentUpdateQualificationMatchDto.DrinksPickupDto(
                    participant.getTeam().getId()
                )
            );

            this.mockMvc.perform(
                    put(
                        TOURNAMENT_BASE_URI + "/{tournamentId}/qualification-matches/{matchId}",
                        tournament.getId(),
                        match.getId()
                    )
                        .header(
                            securityProperties.getAuthHeader(),
                            jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        }

        final var response =
            this.mockMvc.perform(
                    get(TOURNAMENT_BASE_URI + "/{tournamentId}/teams", tournament.getId())
                        .header(
                            securityProperties.getAuthHeader(),
                            jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        final var result = Arrays.stream(
            objectMapper.readValue(response.getContentAsString(), TeamDto[].class)
        ).toList();

        for (final var team : result) {
            final var shouldBePlaying = match
                .getParticipations()
                .stream()
                .map(p -> p.getTeam().getId())
                .toList()
                .contains(team.id());

            assertEquals(team.currentlyPlaying(), shouldBePlaying);
        }
    }
}
