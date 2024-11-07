/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.unittests;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import at.beerbrawl.backend.endpoint.dto.UserLoginDto;
import at.beerbrawl.backend.entity.ApplicationUser;
import at.beerbrawl.backend.entity.BeerPongTable;
import at.beerbrawl.backend.entity.QualificationMatch;
import at.beerbrawl.backend.entity.Team;
import at.beerbrawl.backend.entity.Tournament;
import at.beerbrawl.backend.exception.UserAlreadyExistsException;
import at.beerbrawl.backend.repository.BeerPongTableRepository;
import at.beerbrawl.backend.repository.QualificationMatchRepository;
import at.beerbrawl.backend.repository.TeamRepository;
import at.beerbrawl.backend.repository.TournamentRepository;
import at.beerbrawl.backend.repository.UserRepository;
import at.beerbrawl.backend.service.UserService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class UserDetailServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QualificationMatchRepository qualificationMatchRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private BeerPongTableRepository beerPongTableRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    public void clearDatabase() {
        userRepository.deleteAll();
    }

    @Test
    public void checkDetailedInformationOfRegisteredUser() {
        var user = new ApplicationUser("Username", "Password", false);
        userRepository.save(user);
        var details = userService.findApplicationUserByUsername(user.getUsername());

        assertAll(
            () -> assertNotNull(details),
            () -> assertEquals(user.getAdmin(), details.getAdmin()),
            () -> assertEquals(user.getPassword(), details.getPassword()),
            () -> assertEquals(user.getUsername(), details.getUsername())
        );
    }

    @Test
    public void checkDetailedInformationOfUnknownUser_NotFoundException() throws Exception {
        var username = "Username";
        assertThrows(
            UsernameNotFoundException.class,
            () -> {
                userService.findApplicationUserByUsername(username);
            },
            "Username or password incorrect"
        );
    }

    @Test
    public void checkUpdatedInformationOfRegisteredUser() throws Exception {
        var user = new ApplicationUser("Username", "Password", false);
        userRepository.save(user);
        var oldUserPass = userRepository.findByUsername(user.getUsername()).getPassword();
        var userUpdate = UserLoginDto.UserLoginDtoBuilder.anUserLoginDto()
            .withUsername("Updated")
            .withPassword("UpPass")
            .build();
        var updates = userService.updateUser(userUpdate, user.getUsername());

        assertAll(
            () -> assertNotNull(updates),
            () -> assertEquals(user.getAdmin(), updates.getAdmin()),
            () -> assertNotEquals(oldUserPass, updates.getPassword()),
            () -> assertEquals(userUpdate.getUsername(), updates.getUsername())
        );
    }

    @Test
    public void checkUpdateInformationOfRegisteredUser_UsernameAlreadyExists() throws Exception {
        var sameUsername = "Username";
        var uniqueUsername = "Unique";
        var firstUser = new ApplicationUser(sameUsername, "Password");
        userRepository.save(firstUser);
        var secondUser = new ApplicationUser(uniqueUsername, "Password");
        userRepository.save(secondUser);

        var userUpdate = UserLoginDto.UserLoginDtoBuilder.anUserLoginDto()
            .withUsername(sameUsername)
            .withPassword("UpPass")
            .build();
        assertThrows(
            UserAlreadyExistsException.class,
            () -> {
                userService.updateUser(userUpdate, uniqueUsername);
            },
            "User with username '%s' already exists.".formatted(sameUsername)
        );
    }

    @Test
    public void checkDeleteOfRegisteredUserCascading_SharedQualificationParticipationAndMatch() {
        final var user1 = new ApplicationUser("Username", "Password", true);

        transactionTemplate.executeWithoutResult(_0 -> {
            var tournament = new Tournament(
                "TEST_TOURNAMENT",
                LocalDateTime.of(2024, 12, 12, 12, 0),
                4L,
                "Test Tournament",
                null
            );
            tournament.setOrganizer(user1);
            userRepository.save(user1);

            tournamentRepository.save(tournament);

            var team1 = new Team("Team1", tournament);
            var team2 = new Team("Team2", tournament);
            teamRepository.saveAll(List.of(team1, team2));

            var table = new BeerPongTable("Table", tournament);
            beerPongTableRepository.save(table);

            var match = new QualificationMatch(tournament, List.of(team1, team2));
            qualificationMatchRepository.save(match);
            qualificationMatchRepository.flush();
        });

        assertAll(
            () -> assertEquals(1, userRepository.findAll().size(), "usercount"), // this user and
            // from base class
            () -> assertEquals(1, qualificationMatchRepository.findAll().size(), "matchcount"),
            () -> assertEquals(1, tournamentRepository.findAll().size(), "tournamentcount"),
            () -> assertEquals(1, beerPongTableRepository.findAll().size(), "tablecount"),
            () -> assertEquals(2, teamRepository.findAll().size(), "teamcount")
        );

        transactionTemplate.executeWithoutResult(
            _0 -> userRepository.deleteByUsername(user1.getUsername())
        );

        assertAll(
            () -> assertEquals(0, userRepository.findAll().size(), "usercount"),
            () -> assertEquals(0, qualificationMatchRepository.findAll().size(), "matchcount"),
            () -> assertEquals(0, tournamentRepository.findAll().size(), "tournamentcount"),
            () -> assertEquals(0, beerPongTableRepository.findAll().size(), "tablecount"),
            () -> assertEquals(0, teamRepository.findAll().size(), "teamcount")
        );
    }

    @Test
    public void checkDeleteOfRegisteredUserCascading_NoSharedQualificationParticipationAndMatch() {
        final var user = new ApplicationUser("Username", "Password", true);

        // We need to make sure that this is written
        transactionTemplate.executeWithoutResult(_0 -> {
            userRepository.save(user);
            var tournament = new Tournament(
                "TEST_TOURNAMENT",
                LocalDateTime.of(2024, 12, 12, 12, 0),
                4L,
                "Test Tournament",
                user
            );

            var team1 = new Team("Team1", tournament);
            var team2 = new Team("Team2", tournament);
            var match1 = new QualificationMatch(tournament, List.of(team1, team2));
            var match2 = new QualificationMatch(tournament, List.of(team1, team2));
            var table = new BeerPongTable("Table", tournament);

            // Teams and Tables cascadingly saved by tournament
            tournamentRepository.save(tournament);
            beerPongTableRepository.save(table);
            qualificationMatchRepository.save(match1);
            qualificationMatchRepository.save(match2);
            qualificationMatchRepository.flush();
        });

        assertAll(
            () -> assertEquals(1, userRepository.findAll().size(), "usercount"), // this user and
            // Generated User
            // from baseclass
            () -> assertEquals(2, qualificationMatchRepository.findAll().size(), "matchcount"),
            () -> assertEquals(1, tournamentRepository.findAll().size(), "tournamentcount"),
            () -> assertEquals(1, beerPongTableRepository.findAll().size(), "tablecount"),
            () -> assertEquals(2, teamRepository.findAll().size(), "teamcount")
        );

        transactionTemplate.executeWithoutResult(_0 -> {
            userRepository.deleteByUsername(user.getUsername());
        });

        assertAll(
            () -> assertEquals(0, userRepository.findAll().size(), "usercount"),
            () -> assertEquals(0, qualificationMatchRepository.findAll().size(), "matchcount"),
            () -> assertEquals(0, tournamentRepository.findAll().size(), "tournamentcount"),
            () -> assertEquals(0, beerPongTableRepository.findAll().size(), "tablecount"),
            () -> assertEquals(0, teamRepository.findAll().size(), "teamcount")
        );
    }
}
