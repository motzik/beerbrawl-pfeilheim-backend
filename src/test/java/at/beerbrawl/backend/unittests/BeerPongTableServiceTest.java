/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.unittests;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import at.beerbrawl.backend.basetest.TestData;
import at.beerbrawl.backend.datagenerator.TestDataGenerator;
import at.beerbrawl.backend.endpoint.dto.CreateBeerPongTableDto;
import at.beerbrawl.backend.endpoint.dto.UpdateBeerPongTableDto;
import at.beerbrawl.backend.entity.BeerPongTable;
import at.beerbrawl.backend.entity.Tournament;
import at.beerbrawl.backend.exception.NotFoundException;
import at.beerbrawl.backend.repository.BeerPongTableRepository;
import at.beerbrawl.backend.repository.UserRepository;
import at.beerbrawl.backend.service.BeerPongTableService;
import at.beerbrawl.backend.service.TournamentService;
import at.beerbrawl.backend.util.BeerDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class BeerPongTableServiceTest extends TestData {

    @Autowired
    private TournamentService tournamentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BeerPongTableService beerPongTableService;

    @Autowired
    private BeerPongTableRepository beerPongTableRepository;

    @Test
    public void getSingleBeerPongTableById() {
        // setup
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

        var table = assertDoesNotThrow(() -> beerPongTableService.findById(beerPongTable.getId()));

        assertAll(
            () -> assertNotNull(table),
            () -> assertEquals(table.getName(), table.getName()),
            () -> assertEquals(table.getTournament().getId(), table.getTournament().getId())
        );
    }

    @Test
    public void getSingleBeerPongTableThatDoesntExistById() {
        assertThrows(NotFoundException.class, () -> beerPongTableService.findById(-1L));
    }

    @Test
    public void createNewBeerPongTableForExistingTournamentThatWasCreatedByTheCurrentUser() {
        // setup
        var tournament = new Tournament(
            "TEST_TOURNAMENT",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "testdescription",
            userRepository.findByUsername(TEST_USER)
        );
        tournament = tournamentService.create(tournament, TEST_USER);

        var tableDto = new CreateBeerPongTableDto();
        tableDto.setName("TEST_NAME");
        tableDto.setTournamentId(tournament.getId());

        var table = assertDoesNotThrow(
            () -> beerPongTableService.create(tableDto, TestDataGenerator.TEST_USER)
        );

        assertAll(
            () -> assertNotNull(table),
            () -> assertEquals(table.getName(), tableDto.getName()),
            () -> assertEquals(table.getTournament().getId(), tableDto.getTournamentId())
        );
    }

    @Test
    public void createNewBeerPongTableForExistingTournamentThatWasntCreatedByTheCurrentUser() {
        // setup
        var tournament = new Tournament(
            "TEST_TOURNAMENT",
            BeerDateTime.nowUtc().plusDays(1),
            64L,
            "testdescription",
            userRepository.findByUsername(TEST_USER)
        );
        tournament = tournamentService.create(tournament, TEST_USER);

        var tableDto = new CreateBeerPongTableDto();
        tableDto.setName("TEST_NAME");
        tableDto.setTournamentId(tournament.getId());

        assertThrows(
            AccessDeniedException.class,
            () -> beerPongTableService.create(tableDto, TestDataGenerator.TEST_USER + "_")
        );
    }

    @Test
    public void createNewBeerPongTableForNonExistingTournament() {
        var tableDto = new CreateBeerPongTableDto();
        tableDto.setName("TEST_NAME");
        tableDto.setTournamentId(-1l);

        assertThrows(
            NotFoundException.class,
            () -> beerPongTableService.create(tableDto, TestDataGenerator.TEST_USER)
        );
    }

    @Test
    public void updateBeerPongTableForExistingTournamentThatWasCreatedByTheCurrentUser() {
        // setup
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

        var table = assertDoesNotThrow(
            () ->
                beerPongTableService.update(
                    beerPongTable.getId(),
                    tableDto,
                    TestDataGenerator.TEST_USER
                )
        );

        assertAll(
            () -> assertNotNull(table),
            () -> assertEquals(table.getName(), tableDto.getName())
        );
    }

    @Test
    public void updateBeerPongTableForExistingTournamentThatWasntCreatedByTheCurrentUser() {
        // setup
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

        assertThrows(
            AccessDeniedException.class,
            () ->
                beerPongTableService.update(
                    beerPongTable.getId(),
                    tableDto,
                    TestDataGenerator.TEST_USER + "_"
                )
        );
    }

    @Test
    public void updateNonExistantBeerPongTable() {
        var tableDto = new UpdateBeerPongTableDto();
        tableDto.setName("TEST_NAME");

        assertThrows(
            NotFoundException.class,
            () -> beerPongTableService.update(-1L, tableDto, TestDataGenerator.TEST_USER)
        );
    }
}
