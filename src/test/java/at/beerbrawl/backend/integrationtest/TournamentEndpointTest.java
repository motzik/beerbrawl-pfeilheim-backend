/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.integrationtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import at.beerbrawl.backend.basetest.TestData;
import at.beerbrawl.backend.config.properties.SecurityProperties;
import at.beerbrawl.backend.datagenerator.TestDataGenerator;
import at.beerbrawl.backend.endpoint.dto.CreateTournamentDto;
import at.beerbrawl.backend.endpoint.dto.TeamDto;
import at.beerbrawl.backend.endpoint.dto.TournamentDto;
import at.beerbrawl.backend.endpoint.dto.TournamentListDto;
import at.beerbrawl.backend.endpoint.dto.TournamentOverviewDto;
import at.beerbrawl.backend.endpoint.dto.TournamentQualificationMatchDto;
import at.beerbrawl.backend.endpoint.dto.TournamentUpdateDto;
import at.beerbrawl.backend.endpoint.dto.TournamentUpdateTeamDto;
import at.beerbrawl.backend.endpoint.dto.ValidationErrorDto;
import at.beerbrawl.backend.entity.ApplicationUser;
import at.beerbrawl.backend.entity.BeerPongTable;
import at.beerbrawl.backend.entity.KoStanding;
import at.beerbrawl.backend.entity.QualificationMatch;
import at.beerbrawl.backend.entity.Team;
import at.beerbrawl.backend.entity.Tournament;
import at.beerbrawl.backend.repository.BeerPongTableRepository;
import at.beerbrawl.backend.repository.KoStandingsRepository;
import at.beerbrawl.backend.repository.QualificationMatchRepository;
import at.beerbrawl.backend.repository.QualificationParticipationRepository;
import at.beerbrawl.backend.repository.TeamRepository;
import at.beerbrawl.backend.repository.TournamentRepository;
import at.beerbrawl.backend.repository.UserRepository;
import at.beerbrawl.backend.security.JwtTokenizer;
import at.beerbrawl.backend.util.BeerDateTime;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class TournamentEndpointTest extends TestData {

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
    private TeamRepository teamRepository;

    @Autowired
    private QualificationParticipationRepository qualificationParticipationRepository;

    @Autowired
    private QualificationMatchRepository qualificationMatchRepository;

    @Autowired
    private KoStandingsRepository koMatchRepository;

    @Autowired
    private BeerPongTableRepository beerPongTableRepository;

    @Test
    public void successfullyCreateNewTournament() throws Exception {
        var registrationEnd = BeerDateTime.nowUtc().plusDays(1).withSecond(0).withNano(0);
        CreateTournamentDto data = new CreateTournamentDto()
            .setName("TOURNAMENT 1")
            .setRegistrationEnd(registrationEnd)
            .setMaxParticipants(64L)
            .setDescription("THIS IS A TEST");
        var mvcResult =
            this.mockMvc.perform(
                    post(TOURNAMENT_BASE_URI)
                        .header(
                            securityProperties.getAuthHeader(),
                            jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data))
                )
                .andDo(print())
                .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        var dtoRes = objectMapper.readValue(response.getContentAsString(), TournamentDto.class);

        assertAll(
            () -> assertEquals(HttpStatus.CREATED.value(), response.getStatus()),
            () -> assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType()),
            () -> assertEquals(dtoRes.name(), data.getName()),
            () -> assertEquals(dtoRes.registrationEnd(), data.getRegistrationEnd()),
            () -> assertEquals(dtoRes.maxParticipants(), data.getMaxParticipants()),
            () -> assertEquals(dtoRes.description(), data.getDescription())
        );
    }

    @Test
    public void createNewTournamentWithValidationErrorsFor_Name_RegistrationEnd_And_MaxParticipants()
        throws Exception {
        CreateTournamentDto data = new CreateTournamentDto()
            .setName(null)
            .setRegistrationEnd(BeerDateTime.nowUtc().minusDays(1))
            .setMaxParticipants(0L);
        var mvcResult =
            this.mockMvc.perform(
                    post(TOURNAMENT_BASE_URI)
                        .header(
                            securityProperties.getAuthHeader(),
                            jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data))
                )
                .andDo(print())
                .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        var tournaments = objectMapper
            .readValue(response.getContentAsString(), ValidationErrorDto.class)
            .errors();

        assertEquals(tournaments.size(), 4);

        System.out.println(tournaments);
        assertThat(tournaments).containsExactlyInAnyOrder(
            "registrationEnd Registration end must be in the future.",
            "maxParticipants Max participants needs to be a number larger than 16.",
            "name Name can't be null.",
            "name Name can't be empty."
        );
    }

    @Test
    public void successfullyGetTournaments() throws Exception {
        var mvcResult =
            this.mockMvc.perform(
                    get(TOURNAMENT_BASE_URI)
                        .header(
                            securityProperties.getAuthHeader(),
                            jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        var dtoRes = objectMapper.readValue(
            response.getContentAsString(),
            TournamentListDto[].class
        );

        assertAll(
            () -> assertEquals(HttpStatus.OK.value(), response.getStatus()),
            () -> assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType()),
            () -> assertNotNull(dtoRes),
            () -> assertTrue(dtoRes.length > 0, "Expected to find tournaments but found none")
        );
    }

    @Test
    public void getTournamentsWhenNoneExist() throws Exception {
        tournamentRepository.deleteAll();

        var mvcResult =
            this.mockMvc.perform(
                    get(TOURNAMENT_BASE_URI)
                        .header(
                            securityProperties.getAuthHeader(),
                            jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        var dtoRes = objectMapper.readValue(
            response.getContentAsString(),
            TournamentListDto[].class
        );

        assertAll(
            () -> assertEquals(HttpStatus.OK.value(), response.getStatus()),
            () -> assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType()),
            () -> assertNotNull(dtoRes),
            () -> assertEquals(0, dtoRes.length, "Expected no tournaments but found some")
        );
    }

    @Test
    public void getTournamentsUnauthorized() throws Exception {
        var mvcResult =
            this.mockMvc.perform(get(TOURNAMENT_BASE_URI).contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertAll(() -> assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus()));
    }

    @Test
    public void generateQualificationMatchesForTournamentWithEnoughTeams() throws Exception {
        // setup
        var tournament = new Tournament(
            "TOURNAMENT_WITHOUT_TEAMS",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "testdescription",
            userRepository.findByUsername(TEST_USER)
        );

        tournament = tournamentService.create(tournament, TEST_USER);

        var numberOfTeams = 16;
        for (int i = 0; i < numberOfTeams; i++) {
            var team = new Team("Team" + Integer.toString(i), tournament);
            teamRepository.save(team);
        }
        tournamentRepository.saveAndFlush(tournament);

        var mvcResult =
            this.mockMvc.perform(
                    post(
                        String.format(
                            "%s/%d/qualification-matches",
                            TOURNAMENT_BASE_URI,
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
        assertEquals(HttpStatus.CREATED.value(), response.getStatus());

        var matches = objectMapper
            .readerFor(TournamentQualificationMatchDto.class)
            .<TournamentQualificationMatchDto>readValues(response.getContentAsString())
            .readAll();

        var qualificationParticipations = matches
            .stream()
            .flatMap(
                m ->
                    qualificationParticipationRepository
                        .findAllByQualificationMatchId(m.id())
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

        var mvcResult =
            this.mockMvc.perform(
                    post(
                        String.format(
                            "%s/%d/qualification-matches",
                            TOURNAMENT_BASE_URI,
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

        assertAll(
            () -> assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus()),
            () ->
                assertEquals(
                    "A precondition wasn't met: Not enough teams in specified tournament.",
                    response.getContentAsString()
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

        var mvcResult =
            this.mockMvc.perform(
                    post(
                        String.format(
                            "%s/%d/qualification-matches",
                            TOURNAMENT_BASE_URI,
                            tournament.getId()
                        )
                    ).header(
                        securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken(TEST_USER + "_", TEST_USER_ROLES)
                    )
                )
                .andDo(print())
                .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
    }

    @Test
    public void generateQualificationMatchesForTournamentThatDoesntExist() throws Exception {
        var mvcResult =
            this.mockMvc.perform(
                    post(
                        String.format("%s/%d/qualification-matches", TOURNAMENT_BASE_URI, -1)
                    ).header(
                        securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken(TEST_USER + "_", TEST_USER_ROLES)
                    )
                )
                .andDo(print())
                .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
    }

    @Test
    public void successfullyDeleteTournament() throws Exception {
        // Setup: Create a tournament to delete
        var tournament = new Tournament(
            "TOURNAMENT_TO_DELETE",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "new Tournament",
            userRepository.findByUsername(TestDataGenerator.TEST_USER)
        );

        tournamentRepository.saveAndFlush(tournament);

        // Perform delete request
        this.mockMvc.perform(
                delete(String.format("%s/%d", TOURNAMENT_BASE_URI, tournament.getId()))
                    .header(
                        securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                    )
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isNoContent())
            .andReturn();

        // Verify the tournament has been deleted
        var deletedTournament = tournamentRepository.findById(tournament.getId());
        assertTrue(
            deletedTournament.isEmpty(),
            "Expected the tournament to be deleted, but it still exists"
        );
    }

    @Test
    public void deleteTournamentUnauthorized() throws Exception {
        // Setup: Create a tournament to delete
        var tournament = new Tournament(
            "TOURNAMENT_TO_DELETE",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "new Tournament",
            userRepository.findByUsername(TestDataGenerator.TEST_USER)
        );

        tournamentRepository.saveAndFlush(tournament);

        // Perform delete request without authorization header
        this.mockMvc.perform(
                delete(String.format("%s/%d", TOURNAMENT_BASE_URI, tournament.getId())).contentType(
                    MediaType.APPLICATION_JSON
                )
            )
            .andDo(print())
            .andExpect(status().isForbidden())
            .andReturn();
    }

    @Test
    public void deleteNonExistingTournament() throws Exception {
        // Attempt to delete a non-existing tournament
        long nonExistingTournamentId = -1L;

        var mvcResult =
            this.mockMvc.perform(
                    delete(String.format("%s/%d", TOURNAMENT_BASE_URI, nonExistingTournamentId))
                        .header(
                            securityProperties.getAuthHeader(),
                            jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andReturn();

        MockHttpServletResponse response = mvcResult.getResponse();
        assertEquals(
            HttpStatus.NOT_FOUND.value(),
            response.getStatus(),
            "Expected status 404 for non-existing tournament"
        );
    }

    @Test
    public void deleteTeamFromTournamentWithNoPermission() throws Exception {
        //given
        var newUser = new ApplicationUser("newUser", "password", false);
        userRepository.saveAndFlush(newUser);

        var tournament = new Tournament(
            "Tournament 1",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "new Tournament",
            newUser
        );
        tournamentRepository.saveAndFlush(tournament);

        //when and then
        this.mockMvc.perform(
                delete(String.format("%s/%d/teams/1", TOURNAMENT_BASE_URI, tournament.getId()))
                    .header(
                        securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                    )
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isForbidden())
            .andReturn();
    }

    @Test
    public void deleteTeamFromTournamentAndTeamDoesNotExists() throws Exception {
        //given
        var tournament = new Tournament(
            "Tournament 1",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "new Tournament",
            userRepository.findByUsername(TestDataGenerator.TEST_USER)
        );
        tournamentRepository.saveAndFlush(tournament);

        //when and then
        this.mockMvc.perform(
                delete(String.format("%s/%d/teams/1", TOURNAMENT_BASE_URI, tournament.getId()))
                    .header(
                        securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                    )
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isNotFound())
            .andReturn();
    }

    @Test
    public void deleteTeamFromTournamentWithWrongTournament() throws Exception {
        //given
        var tournament1 = new Tournament(
            "Tournament 1",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "new Tournament",
            userRepository.findByUsername(TestDataGenerator.TEST_USER)
        );
        var tournament2 = new Tournament(
            "Tournament 1",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "new Tournament",
            userRepository.findByUsername(TestDataGenerator.TEST_USER)
        );
        tournamentRepository.saveAllAndFlush(List.of(tournament1, tournament2));

        var team = new Team("Team 1", tournament1);
        teamRepository.saveAndFlush(team);

        //when and then
        this.mockMvc.perform(
                delete(
                    String.format(
                        "%s/%d/teams/%d",
                        TOURNAMENT_BASE_URI,
                        tournament2.getId(),
                        team.getId()
                    )
                )
                    .header(
                        securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                    )
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isNotFound())
            .andReturn();
    }

    @Test
    public void deleteTeamFromTournamentWhenItsAlreadyStarted() throws Exception {
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

        //create qualification matches
        var mvcResult =
            this.mockMvc.perform(
                    post(
                        String.format(
                            "%s/%d/qualification-matches",
                            TOURNAMENT_BASE_URI,
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
        assertEquals(HttpStatus.CREATED.value(), response.getStatus());

        //when and then
        this.mockMvc.perform(
                delete(
                    String.format(
                        "%s/%d/teams/%d",
                        TOURNAMENT_BASE_URI,
                        tournament.getId(),
                        teams.getFirst().getId()
                    )
                )
                    .header(
                        securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                    )
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isConflict())
            .andReturn();
    }

    @Test
    public void deleteTeamFromTournament() {
        // setup
        var tournament = new Tournament(
            "Tournament 1",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "new Tournament",
            userRepository.findByUsername(TestDataGenerator.TEST_USER)
        );

        tournamentRepository.saveAndFlush(tournament);

        var team = new Team("Team 1", tournament);
        teamRepository.saveAndFlush(team);

        //when and then
        assertDoesNotThrow(() -> {
            this.mockMvc.perform(
                    delete(
                        String.format(
                            "%s/%d/teams/%d",
                            TOURNAMENT_BASE_URI,
                            tournament.getId(),
                            team.getId()
                        )
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
        });
    }

    @Test
    public void cannotSelfSignupTeamForTournamentWithMissingToken() throws Exception {
        final var tournament = tournamentRepository.findAll().stream().findFirst().get();

        var mvcResult =
            this.mockMvc.perform(
                    get(TOURNAMENT_BASE_URI)
                        .header(
                            securityProperties.getAuthHeader(),
                            jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                ).andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        final var tournaments = objectMapper.readValue(
            response.getContentAsString(),
            TournamentListDto[].class
        );

        assertThat(tournaments)
            .isNotNull()
            .hasSize(3)
            .extracting("name")
            .contains(tournament.getName());

        final var listDto = tournaments[0];
        final var createDto = new TournamentUpdateTeamDto("baz");

        mvcResult = this.mockMvc.perform(
                post(TOURNAMENT_BASE_URI + "/{tournamentId}/teams", listDto.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createDto))
            )
            .andDo(print())
            .andReturn();

        response = mvcResult.getResponse();
        assertEquals(response.getStatus(), HttpStatus.UNAUTHORIZED.value());
        assertEquals("public access token missing or incorrect", response.getContentAsString());
    }

    @Test
    public void cannotSelfSignupTeamForTournamentWithInvalidToken() throws Exception {
        final var tournament = tournamentRepository.findAll().stream().findFirst().get();

        var mvcResult =
            this.mockMvc.perform(
                    get(TOURNAMENT_BASE_URI)
                        .header(
                            securityProperties.getAuthHeader(),
                            jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                ).andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        final var tournaments = objectMapper.readValue(
            response.getContentAsString(),
            TournamentListDto[].class
        );

        assertThat(tournaments)
            .isNotNull()
            .hasSize(3)
            .extracting("name")
            .contains(tournament.getName());

        final var listDto = tournaments[0];
        final var createDto = new TournamentUpdateTeamDto("baz");

        mvcResult = this.mockMvc.perform(
                post(TOURNAMENT_BASE_URI + "/{tournamentId}/teams", listDto.getId())
                    .param("token", "11111111-2222-3333-4444-555555555555")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createDto))
            )
            .andDo(print())
            .andReturn();

        response = mvcResult.getResponse();
        assertEquals(response.getStatus(), HttpStatus.UNAUTHORIZED.value());
        assertEquals("public access token missing or incorrect", response.getContentAsString());
    }

    @Test
    public void markTeamAsReadyWithNoPermission() throws Exception {
        //given
        var newUser = new ApplicationUser("newUser", "password", false);
        userRepository.saveAndFlush(newUser);

        var tournament = new Tournament(
            "Tournament 1",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "new Tournament",
            newUser
        );
        tournamentRepository.saveAndFlush(tournament);

        //when and then
        this.mockMvc.perform(
                post(String.format("%s/%d/teams/1/ready", TOURNAMENT_BASE_URI, tournament.getId()))
                    .header(
                        securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                    )
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isForbidden())
            .andReturn();
    }

    @Test
    public void markTeamAsReadyWhenTeamDoesNotExist() throws Exception {
        //given
        var tournament = new Tournament(
            "Tournament 1",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "new Tournament",
            userRepository.findByUsername(TestDataGenerator.TEST_USER)
        );
        tournamentRepository.saveAndFlush(tournament);

        //when and then
        this.mockMvc.perform(
                post(String.format("%s/%d/teams/1/ready", TOURNAMENT_BASE_URI, tournament.getId()))
                    .header(
                        securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                    )
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isNotFound())
            .andReturn();
    }

    @Test
    public void markTeamAsReadyWithWrongTournament() throws Exception {
        //given
        var tournament1 = new Tournament(
            "Tournament 1",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "new Tournament",
            userRepository.findByUsername(TestDataGenerator.TEST_USER)
        );
        var tournament2 = new Tournament(
            "Tournament 1",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "new Tournament",
            userRepository.findByUsername(TestDataGenerator.TEST_USER)
        );
        tournamentRepository.saveAllAndFlush(List.of(tournament1, tournament2));

        var team = new Team("Team 1", tournament1);
        teamRepository.saveAndFlush(team);

        //when and then
        this.mockMvc.perform(
                post(
                    String.format(
                        "%s/%d/teams/%d/ready",
                        TOURNAMENT_BASE_URI,
                        tournament2.getId(),
                        team.getId()
                    )
                )
                    .header(
                        securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                    )
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isNotFound())
            .andReturn();
    }

    @Test
    public void markTeamAsReady() throws Exception {
        //given
        var tournament = new Tournament(
            "Tournament 1",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "new Tournament",
            userRepository.findByUsername(TestDataGenerator.TEST_USER)
        );
        tournamentRepository.saveAndFlush(tournament);

        var team = new Team("Team 1", tournament);
        teamRepository.saveAndFlush(team);

        //when and then
        this.mockMvc.perform(
                post(
                    String.format(
                        "%s/%d/teams/%d/ready",
                        TOURNAMENT_BASE_URI,
                        tournament.getId(),
                        team.getId()
                    )
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

        this.teamRepository.findById(team.getId()).ifPresentOrElse(
                t -> assertTrue(t.getCheckedIn()),
                () -> fail("Team not found")
            );
    }

    @Test
    @Transactional
    public void getTournamentOverviewWithMatches_OnePlayedEach() throws Exception {
        var tournament = new Tournament(
            "Tournament 1",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "new Tournament",
            userRepository.findByUsername(TestDataGenerator.TEST_USER)
        );
        tournamentRepository.save(tournament);

        var team1 = new Team("Team1", tournament);
        team1.checkIn();
        var team2 = new Team("Team2", tournament);
        var match1 = new QualificationMatch(tournament, List.of(team1, team2));
        var match2 = new QualificationMatch(tournament, List.of(team1, team2));
        match2.setStartTime(BeerDateTime.nowUtc());
        match2.setEndTime(BeerDateTime.nowUtc().plusMinutes(1));
        var match3 = new KoStanding(tournament, null, team1);
        match3.setStartTime(BeerDateTime.nowUtc().minusDays(7));
        match3.setEndTime(BeerDateTime.nowUtc().minusDays(1));

        var table = new BeerPongTable("Table", tournament);
        teamRepository.saveAll(List.of(team1, team2));

        // Teams and Tables cascadingly saved by tournament
        tournamentRepository.save(tournament);
        beerPongTableRepository.save(table);
        koMatchRepository.save(match3);
        qualificationMatchRepository.saveAllAndFlush(List.of(match1, match2));

        var userid = userRepository.findByUsername(TEST_USER).getId();

        var tournamentId = tournamentRepository
            .findAllByOrganizerIdOrderByNameAsc(userid)
            .getLast()
            .getId();
        var mvcResult =
            this.mockMvc.perform(
                    get(TOURNAMENT_BASE_URI + "/%d".formatted(tournamentId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(
                            securityProperties.getAuthHeader(),
                            jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                        )
                )
                .andDo(print())
                .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        var dtoRes = objectMapper.readValue(
            response.getContentAsString(),
            TournamentOverviewDto.class
        );

        assertThat(dtoRes).isNotNull();
        assertThat(dtoRes.getCheckedInTeams()).isEqualTo(1);
        assertThat(dtoRes.getTeams()).isEqualTo(2);
        assertThat(dtoRes.getAllKoMatches()).isEqualTo(0); // <- because it's a leaf node and that doesn't count as match
        assertThat(dtoRes.getPlayedKoMatches()).isEqualTo(1);
        assertThat(dtoRes.getTables()).isEqualTo(1);
        assertThat(dtoRes.getAllQualificationMatches()).isEqualTo(2);
        assertThat(dtoRes.getPlayedQualificationMatches()).isEqualTo(1);
        assertThat(dtoRes.getName()).isEqualTo(tournament.getName());
        assertThat(dtoRes.getMaxParticipants()).isEqualTo(tournament.getMaxParticipants());
    }

    @Test
    public void getTournamentOverview_DifferentOrganizer() throws Exception {
        var tournament = new Tournament(
            "Tournament 1",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "new Tournament",
            userRepository.findByUsername(TestDataGenerator.TEST_USER)
        );

        tournamentRepository.save(tournament);

        var userid = userRepository.findByUsername(TEST_USER).getId();
        var tournamentId = tournamentRepository
            .findAllByOrganizerIdOrderByNameAsc(userid)
            .getFirst()
            .getId();
        this.mockMvc.perform(
                get(TOURNAMENT_BASE_URI + "/%d".formatted(tournamentId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(
                        securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken("different", TEST_USER_ROLES)
                    )
            )
            .andDo(print())
            .andExpect(status().isForbidden());
    }

    @Test
    public void getTournamentOverview_NonExistentTournament() throws Exception {
        var tournament = new Tournament(
            "Tournament 1",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "new Tournament",
            userRepository.findByUsername(TestDataGenerator.TEST_USER)
        );

        tournamentRepository.save(tournament);
        this.mockMvc.perform(
                get(TOURNAMENT_BASE_URI + "/%d".formatted(-1))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(
                        securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                    )
            )
            .andDo(print())
            .andExpect(status().isNotFound());
    }

    @Test
    public void updateTournament_MaxParticipationLessThanActual_ThrowsValidationException()
        throws Exception {
        var userid = userRepository.findByUsername(TEST_USER).getId();
        var tournament = tournamentRepository.findAllByOrganizerIdOrderByNameAsc(userid).getFirst();
        var updatesDto = new TournamentUpdateDto(
            "Updated",
            BeerDateTime.nowUtc().plusMinutes(1),
            22L,
            "Updated description"
        );

        this.mockMvc.perform(
                put(TOURNAMENT_BASE_URI + "/{tournamentId}", tournament.getId())
                    .header(
                        securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updatesDto))
            )
            .andDo(print())
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void canUpdateExistingTeamInTournamentSuccessfully() throws Exception {
        final var tournament = tournamentRepository.findAll().stream().findFirst().get();

        var team = teamRepository.saveAndFlush(new Team("bobs team", tournament));

        final var updateDto = new TournamentUpdateTeamDto("alices team");
        final var mvcResult =
            this.mockMvc.perform(
                    put(
                        TOURNAMENT_BASE_URI + "/{tournamentId}/teams/{teamId}",
                        tournament.getId(),
                        team.getId()
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
                .andExpect(
                    header()
                        .string(
                            "Content-Location",
                            TOURNAMENT_BASE_URI +
                            "/%d/teams/%d".formatted(tournament.getId(), team.getId())
                        )
                )
                .andReturn();

        final var responseDto = objectMapper.readValue(
            mvcResult.getResponse().getContentAsString(),
            TeamDto.class
        );

        assertEquals(
            responseDto,
            new TeamDto(team.getId(), updateDto.name(), team.getCheckedIn(), null)
        );
    }
}
