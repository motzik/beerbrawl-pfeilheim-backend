/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.integrationtest;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import at.beerbrawl.backend.basetest.TestData;
import at.beerbrawl.backend.config.properties.SecurityProperties;
import at.beerbrawl.backend.datagenerator.TestDataGenerator;
import at.beerbrawl.backend.endpoint.dto.BeerPongTableDto;
import at.beerbrawl.backend.endpoint.dto.CreateBeerPongTableDto;
import at.beerbrawl.backend.endpoint.dto.UpdateBeerPongTableDto;
import at.beerbrawl.backend.entity.ApplicationUser;
import at.beerbrawl.backend.entity.BeerPongTable;
import at.beerbrawl.backend.entity.Tournament;
import at.beerbrawl.backend.repository.BeerPongTableRepository;
import at.beerbrawl.backend.repository.UserRepository;
import at.beerbrawl.backend.security.JwtTokenizer;
import at.beerbrawl.backend.service.TournamentService;
import at.beerbrawl.backend.util.BeerDateTime;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class BeerPongTableEndpointTest extends TestData {

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
    private UserRepository userRepository;

    @Autowired
    private BeerPongTableRepository beerPongTableRepository;

    @Test
    public void getSingleBeerPongTableById() throws Exception {
        // setup
        var tournament = new Tournament(
            "TEST_TOURNAMENT",
            BeerDateTime.nowUtc().plusDays(1),
            64l,
            null,
            null
        );

        tournament = tournamentService.create(tournament, TestDataGenerator.TEST_USER);

        var beerPongTable = new BeerPongTable("TEST", tournament);
        beerPongTableRepository.save(beerPongTable);

        var mvcResult =
            this.mockMvc.perform(
                    get(
                        String.format("%s/%d", BEER_PONG_TABLE_BASE_URI, beerPongTable.getId())
                    ).header(
                        securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken(TestDataGenerator.TEST_USER, TEST_USER_ROLES)
                    )
                )
                .andDo(print())
                .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        var dtoRes = objectMapper.readValue(response.getContentAsString(), BeerPongTableDto.class);

        assertAll(
            () -> assertEquals(HttpStatus.OK.value(), response.getStatus()),
            () -> assertNotNull(dtoRes),
            () -> assertEquals(beerPongTable.getId(), dtoRes.id()),
            () -> assertEquals(beerPongTable.getName(), dtoRes.name()),
            () -> assertEquals(beerPongTable.getTournament().getId(), dtoRes.tournamentId())
        );
    }

    @Test
    public void getSingleBeerPongTableThatDoesntExistById() throws Exception {
        var mvcResult =
            this.mockMvc.perform(
                    get(String.format("%s/%d", BEER_PONG_TABLE_BASE_URI, -1)).header(
                        securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken(TestDataGenerator.TEST_USER, TEST_USER_ROLES)
                    )
                )
                .andDo(print())
                .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
    }

    @Test
    public void createNewBeerPongTableForExistingTournamentThatWasCreatedByTheCurrentUser()
        throws Exception {
        // setup
        var user = new ApplicationUser("TestUser", "Password", false);
        userRepository.save(user);

        var tournament = new Tournament(
            "TEST_TOURNAMENT",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "testdescription",
            user
        );
        tournament = tournamentService.create(tournament, user.getUsername());

        var tableDto = new CreateBeerPongTableDto();
        tableDto.setName("TEST_NAME");
        tableDto.setTournamentId(tournament.getId());

        var mvcResult =
            this.mockMvc.perform(
                    post(BEER_PONG_TABLE_BASE_URI)
                        .header(
                            securityProperties.getAuthHeader(),
                            jwtTokenizer.getAuthToken(user.getUsername(), TEST_USER_ROLES)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tableDto))
                )
                .andDo(print())
                .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        var dtoRes = objectMapper.readValue(response.getContentAsString(), BeerPongTableDto.class);

        assertAll(
            () -> assertEquals(HttpStatus.CREATED.value(), response.getStatus()),
            () -> assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType()),
            () -> assertEquals(dtoRes.name(), tableDto.getName()),
            () -> assertEquals(dtoRes.tournamentId(), tableDto.getTournamentId())
        );
    }

    @Test
    public void createNewBeerPongTableForExistingTournamentThatWasntCreatedByTheCurrentUser()
        throws Exception {
        // setup
        var user = new ApplicationUser("TestUser", "Password", false);
        userRepository.save(user);

        var tournament = new Tournament(
            "TEST_TOURNAMENT",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "testdescription",
            user
        );
        tournament = tournamentService.create(tournament, user.getUsername());

        var tableDto = new CreateBeerPongTableDto();
        tableDto.setName("TEST_NAME");
        tableDto.setTournamentId(tournament.getId());

        var mvcResult =
            this.mockMvc.perform(
                    post(BEER_PONG_TABLE_BASE_URI)
                        .header(
                            securityProperties.getAuthHeader(),
                            jwtTokenizer.getAuthToken(user.getUsername() + "_", TEST_USER_ROLES)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tableDto))
                )
                .andDo(print())
                .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
    }

    @Test
    public void createNewBeerPongTableForNonExistingTournament() throws Exception {
        var tableDto = new CreateBeerPongTableDto();
        tableDto.setName("TEST_NAME");
        tableDto.setTournamentId(-1l);

        var mvcResult =
            this.mockMvc.perform(
                    post(BEER_PONG_TABLE_BASE_URI)
                        .header(
                            securityProperties.getAuthHeader(),
                            jwtTokenizer.getAuthToken(TEST_USER, TEST_USER_ROLES)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tableDto))
                )
                .andDo(print())
                .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
    }

    @Test
    public void updateBeerPongTableForExistingTournamentThatWasCreatedByTheCurrentUser()
        throws Exception {
        var tournament = new Tournament(
            "TEST_TOURNAMENT",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "testdescription",
            userRepository.findByUsername(TEST_USER)
        );
        tournament = tournamentService.create(tournament, TEST_USER);

        var beerPongTable = new BeerPongTable("TEST", tournament);
        beerPongTableRepository.save(beerPongTable);

        var tableDto = new UpdateBeerPongTableDto();
        tableDto.setName("TEST_NAME");

        var mvcResult =
            this.mockMvc.perform(
                    put(String.format("%s/%d", BEER_PONG_TABLE_BASE_URI, beerPongTable.getId()))
                        .header(
                            securityProperties.getAuthHeader(),
                            jwtTokenizer.getAuthToken(TestDataGenerator.TEST_USER, TEST_USER_ROLES)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tableDto))
                )
                .andDo(print())
                .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        var dtoRes = objectMapper.readValue(response.getContentAsString(), BeerPongTableDto.class);

        assertAll(
            () -> assertEquals(HttpStatus.OK.value(), response.getStatus()),
            () -> assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType()),
            () -> assertEquals(dtoRes.name(), tableDto.getName())
        );
    }

    @Test
    public void updateBeerPongTableForExistingTournamentThatWasntCreatedByTheCurrentUser()
        throws Exception {
        var tournament = new Tournament(
            "TEST_TOURNAMENT",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "testdescription",
            userRepository.findByUsername(TEST_USER)
        );
        tournament = tournamentService.create(tournament, TEST_USER);

        var beerPongTable = new BeerPongTable("TEST", tournament);
        beerPongTableRepository.save(beerPongTable);

        var tableDto = new UpdateBeerPongTableDto();
        tableDto.setName("TEST_NAME");

        var mvcResult =
            this.mockMvc.perform(
                    put(String.format("%s/%d", BEER_PONG_TABLE_BASE_URI, beerPongTable.getId()))
                        .header(
                            securityProperties.getAuthHeader(),
                            jwtTokenizer.getAuthToken(
                                TestDataGenerator.TEST_USER + "_",
                                TEST_USER_ROLES
                            )
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tableDto))
                )
                .andDo(print())
                .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
    }

    @Test
    public void updateNonExistantBeerPongTable() throws Exception {
        var tableDto = new UpdateBeerPongTableDto();
        tableDto.setName("TEST_NAME");

        var mvcResult =
            this.mockMvc.perform(
                    put(String.format("%s/%d", BEER_PONG_TABLE_BASE_URI, -1))
                        .header(
                            securityProperties.getAuthHeader(),
                            jwtTokenizer.getAuthToken(
                                TestDataGenerator.TEST_USER + "_",
                                TEST_USER_ROLES
                            )
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tableDto))
                )
                .andDo(print())
                .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
    }

    @Test
    public void deleteBeerPongTable() throws Exception {
        var user = new ApplicationUser("TestUser", "Password", false);
        userRepository.save(user);

        var tournament = new Tournament(
            "TEST_TOURNAMENT",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "testdescription",
            user
        );
        tournament = tournamentService.create(tournament, user.getUsername());

        var beerPongTable = new BeerPongTable("TEST", tournament);
        beerPongTableRepository.save(beerPongTable);

        var mvcResult =
            this.mockMvc.perform(
                    delete(
                        String.format("%s/%d", BEER_PONG_TABLE_BASE_URI, beerPongTable.getId())
                    ).header(
                        securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken(user.getUsername(), TEST_USER_ROLES)
                    )
                )
                .andDo(print())
                .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertEquals(HttpStatus.NO_CONTENT.value(), response.getStatus());
        assertFalse(beerPongTableRepository.findById(beerPongTable.getId()).isPresent());
    }

    @Test
    public void deleteBeerPongTableNotAuthorized() throws Exception {
        // setup
        var user = new ApplicationUser("TestUser", "Password", false);
        userRepository.save(user);

        var tournament = new Tournament(
            "TEST_TOURNAMENT",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "testdescription",
            user
        );
        tournament = tournamentService.create(tournament, user.getUsername());

        var beerPongTable = new BeerPongTable("TEST", tournament);
        beerPongTableRepository.save(beerPongTable);

        var mvcResult =
            this.mockMvc.perform(
                    delete(
                        String.format("%s/%d", BEER_PONG_TABLE_BASE_URI, beerPongTable.getId())
                    ).header(
                        securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken(user.getUsername() + "_", TEST_USER_ROLES)
                    )
                )
                .andDo(print())
                .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
        assertTrue(beerPongTableRepository.findById(beerPongTable.getId()).isPresent());
    }

    @Test
    public void deleteNonExistentBeerPongTable() throws Exception {
        var mvcResult =
            this.mockMvc.perform(
                    delete(String.format("%s/%d", BEER_PONG_TABLE_BASE_URI, -1)).header(
                        securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken(TestDataGenerator.TEST_USER, TEST_USER_ROLES)
                    )
                )
                .andDo(print())
                .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
    }

    @Test
    public void findBeerPongTablesByTournamentId() throws Exception {
        var user = new ApplicationUser("TestUser", "Password", false);
        userRepository.save(user);

        var tournament = new Tournament(
            "TEST_TOURNAMENT",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "testdescription",
            user
        );
        tournament = tournamentService.create(tournament, user.getUsername());

        var beerPongTable1 = new BeerPongTable("TEST_TABLE_1", tournament);
        beerPongTableRepository.save(beerPongTable1);

        var beerPongTable2 = new BeerPongTable("TEST_TABLE_2", tournament);
        beerPongTableRepository.save(beerPongTable2);

        var mvcResult =
            this.mockMvc.perform(
                    get(
                        String.format(
                            "%s/tournament/%d",
                            BEER_PONG_TABLE_BASE_URI,
                            tournament.getId()
                        )
                    ).header(
                        securityProperties.getAuthHeader(),
                        jwtTokenizer.getAuthToken(user.getUsername(), TEST_USER_ROLES)
                    )
                )
                .andDo(print())
                .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        var dtoRes = objectMapper.readValue(
            response.getContentAsString(),
            BeerPongTableDto[].class
        );

        assertAll(
            () -> assertEquals(HttpStatus.OK.value(), response.getStatus()),
            () -> assertNotNull(dtoRes),
            () -> assertEquals(2, dtoRes.length),
            () ->
                assertTrue(
                    dtoRes[0].name().equals("TEST_TABLE_1") ||
                    dtoRes[0].name().equals("TEST_TABLE_2")
                ),
            () ->
                assertTrue(
                    dtoRes[1].name().equals("TEST_TABLE_1") ||
                    dtoRes[1].name().equals("TEST_TABLE_2")
                )
        );
    }
}
