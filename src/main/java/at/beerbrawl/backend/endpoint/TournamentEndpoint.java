/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint;

import at.beerbrawl.backend.endpoint.dto.CreateTournamentDto;
import at.beerbrawl.backend.endpoint.dto.QualificationTeamScoreDto;
import at.beerbrawl.backend.endpoint.dto.QueuedMatchDto;
import at.beerbrawl.backend.endpoint.dto.TeamDto;
import at.beerbrawl.backend.endpoint.dto.TournamentDto;
import at.beerbrawl.backend.endpoint.dto.TournamentListDto;
import at.beerbrawl.backend.endpoint.dto.TournamentOverviewDto;
import at.beerbrawl.backend.endpoint.dto.TournamentQualificationMatchDto;
import at.beerbrawl.backend.endpoint.dto.TournamentSignupTeamResponseDto;
import at.beerbrawl.backend.endpoint.dto.TournamentUpdateDto;
import at.beerbrawl.backend.endpoint.dto.TournamentUpdateQualificationMatchDto;
import at.beerbrawl.backend.endpoint.dto.TournamentUpdateTeamDto;
import at.beerbrawl.backend.endpoint.mapper.QualificationTeamScoreMapper;
import at.beerbrawl.backend.endpoint.mapper.TeamMapper;
import at.beerbrawl.backend.endpoint.mapper.TournamentMapper;
import at.beerbrawl.backend.endpoint.mapper.TournamentOverviewMapper;
import at.beerbrawl.backend.entity.Tournament.SignupTeamResult;
import at.beerbrawl.backend.entity.domainservice.MatchDomainService;
import at.beerbrawl.backend.exception.BadTournamentPublicAccessTokenException;
import at.beerbrawl.backend.service.TournamentQualificationService;
import at.beerbrawl.backend.service.TournamentService;
import at.beerbrawl.backend.service.TournamentTeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = TournamentEndpoint.BASE_ENDPOINT)
@AllArgsConstructor
public class TournamentEndpoint {

    public static final String BASE_ENDPOINT = "/api/v1/tournaments";
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final TournamentService tournamentService;
    private final TournamentQualificationService qualificationService;
    private final TournamentTeamService teamService;
    private final TournamentMapper tournamentMapper;
    private final TeamMapper teamMapper;
    private final QualificationTeamScoreMapper qualificationTeamScoreMapper;
    private final MatchDomainService matchDomainService;
    private final TournamentOverviewMapper tournamentOverviewMapper;

    @Secured("ROLE_USER")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get a list of all tournaments from a specific Organizer",
        security = @SecurityRequirement(name = "apiKey")
    )
    public ResponseEntity<List<TournamentListDto>> tournaments(Authentication authentication) {
        LOG.info("GET {}", BASE_ENDPOINT);
        var tournaments = tournamentService.findAllByOrganizer(authentication.getName());
        return ResponseEntity.ok(tournamentMapper.tournamentToTournamentListDto(tournaments));
    }

    @Secured("ROLE_USER")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = "/{tournamentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get the overview for a specific tournament",
        security = @SecurityRequirement(name = "apiKey")
    )
    public ResponseEntity<TournamentOverviewDto> getTournamentOverview(
        @PathVariable(value = "tournamentId") long tournamentId,
        Authentication authentication
    ) {
        LOG.info("GET {}/{}", BASE_ENDPOINT, tournamentId);
        // check if user is organizer of tournament
        if (!tournamentService.isOrganizer(authentication.getName(), tournamentId)) {
            throw new AccessDeniedException("Current user isn't organizer of the tournament.");
        }
        var tournamentOverview = tournamentService.getTournamentOverview(tournamentId);
        return ResponseEntity.ok(tournamentOverviewMapper.modelToDto(tournamentOverview));
    }

    @Secured("ROLE_USER")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @Operation(
        summary = "Create a new tournament",
        security = @SecurityRequirement(name = "apiKey")
    )
    public TournamentDto createTournament(
        @Valid @RequestBody CreateTournamentDto createTournamentDto,
        Authentication authentication
    ) {
        LOG.info("POST {}", BASE_ENDPOINT);
        LOG.debug("request body: {}", createTournamentDto);

        return tournamentMapper.entityToDto(
            tournamentService.create(
                tournamentMapper.createDtoToEntity(createTournamentDto),
                authentication.getName()
            )
        );
    }

    @Secured("ROLE_USER")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(value = "{id}/qualification-matches")
    @Operation(
        summary = "Generate qualification matches for tournament",
        security = @SecurityRequirement(name = "apiKey")
    )
    public List<TournamentQualificationMatchDto> generateQualificationMatches(
        @PathVariable(name = "id") Long tournamentId,
        Authentication authentication
    ) {
        LOG.info("POST {}/{}/qualification-matches", BASE_ENDPOINT, tournamentId);

        return qualificationService
            .generateQualificationMatchesForTournament(tournamentId, authentication.getName())
            .stream()
            .map(tournamentMapper::qualificationMatchEntityToDto)
            .toList();
    }

    @Secured("ROLE_USER")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = "{id}/qualification-matches")
    @Operation(
        summary = "Get qualification matches for tournament",
        security = @SecurityRequirement(name = "apiKey")
    )
    public List<TournamentQualificationMatchDto> getQualificationMatches(
        @PathVariable(name = "id") Long tournamentId,
        Authentication authentication
    ) {
        LOG.info("GET {}/{}/qualification-matches/queued", BASE_ENDPOINT, tournamentId);

        if (!tournamentService.isOrganizer(authentication.getName(), tournamentId)) {
            throw new AccessDeniedException("Current user isn't organizer of tournament.");
        }

        var queuedQualiMatches = matchDomainService.getQualificationMatchesByExpectedStart(
            tournamentId
        );

        return queuedQualiMatches
            .stream()
            .map(tournamentMapper::qualificationMatchEntityToDto)
            .toList();
    }

    @PermitAll
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = "{id}/qualification-matches/public")
    @Operation(summary = "Get qualification matches for tournament")
    public List<TournamentQualificationMatchDto> getQualificationMatchesPublic(
        @PathVariable(name = "id") Long tournamentId,
        @RequestParam(name = "token") Optional<UUID> token
    ) {
        LOG.info("GET {}/{}/qualification-matches/public", BASE_ENDPOINT, tournamentId);

        // Explicitly use an `Optional<>` and check it here, so we can return
        // the appropriate error
        // Otherwise, Spring Boot would just return a 400.
        if (token.isEmpty()) {
            throw new BadTournamentPublicAccessTokenException();
        }

        tournamentService.assertAccessTokenIsCorrect(tournamentId, token.get());

        var queuedQualiMatches = matchDomainService.getQualificationMatchesByExpectedStart(
            tournamentId
        );

        return queuedQualiMatches
            .stream()
            .map(tournamentMapper::qualificationMatchEntityToDto)
            .toList();
    }

    @Secured("ROLE_USER")
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("{tournamentId}/qualification-matches/{matchId}")
    @Operation(
        summary = "Update a qualification match of a tournament",
        security = @SecurityRequirement(name = "apiKey")
    )
    public TournamentQualificationMatchDto updateQualificationRound(
        @PathVariable(name = "tournamentId") Long tournamentId,
        @PathVariable(name = "matchId") Long matchId,
        @Valid @RequestBody TournamentUpdateQualificationMatchDto updateDto,
        Authentication authentication
    ) {
        LOG.info("PUT {}/{}/qualification-matches/{}", BASE_ENDPOINT, tournamentId, matchId);
        LOG.debug("request body: {}", updateDto);

        // check if user is organizer of tournament
        if (!tournamentService.isOrganizer(authentication.getName(), tournamentId)) {
            throw new AccessDeniedException("Current user isn't organizer of tournament.");
        }

        final var updatedMatch = qualificationService.updateQualificationMatch(
            tournamentId,
            matchId,
            updateDto
        );

        return this.tournamentMapper.qualificationMatchEntityToDto(updatedMatch);
    }

    @Secured("ROLE_USER")
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/{tournamentId}")
    @Operation(summary = "Update a tournament", security = @SecurityRequirement(name = "apiKey"))
    public TournamentDto updateTournament(
        @PathVariable("tournamentId") long tournamentId,
        @Valid @RequestBody TournamentUpdateDto updates,
        Authentication authentication
    ) {
        LOG.info("UPDATE {}/{}", BASE_ENDPOINT, tournamentId);

        if (!tournamentService.isOrganizer(authentication.getName(), tournamentId)) {
            throw new AccessDeniedException("Current user isn't organizer of tournament.");
        }

        final var tournament = tournamentService.updateTournament(tournamentId, updates);
        return this.tournamentMapper.entityToDto(tournament);
    }

    @Secured("ROLE_USER")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{tournamentId}")
    @Operation(summary = "Delete a tournament", security = @SecurityRequirement(name = "apiKey"))
    public ResponseEntity<Void> deleteTournament(
        @PathVariable("tournamentId") long tournamentId,
        Authentication authentication
    ) {
        LOG.info("DELETE {}/{}", BASE_ENDPOINT, tournamentId);
        tournamentService.deleteTournament(tournamentId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    // as of now, information is accessible to everyone
    @PermitAll
    @GetMapping("{tournamentId}/public")
    @Operation(summary = "Get public info about tournament")
    public ResponseEntity<TournamentDto> get(@PathVariable("tournamentId") long tournamentId) {
        LOG.info("GET {}/{}/public", BASE_ENDPOINT, tournamentId);
        final var tournament = tournamentService.findOne(tournamentId);
        final var dto = TournamentDto.fromEntity(tournament);
        return ResponseEntity.ok(dto);
    }

    @Secured("ROLE_USER")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = "{id}/qualification-phase/scores")
    @Operation(
        summary = "Get the score list of the qualification phase of a tournament",
        security = @SecurityRequirement(name = "apiKey")
    )
    public List<QualificationTeamScoreDto> getTournamentQualificationScoreTable(
        @PathVariable(name = "id") Long tournamentId,
        Authentication authentication
    ) {
        LOG.info("GET {}/{}/qualification-phase/scores", BASE_ENDPOINT, tournamentId);

        // check if user is organizer of tournament
        if (!tournamentService.isOrganizer(authentication.getName(), tournamentId)) {
            throw new AccessDeniedException("Current user isn't organizer of tournament.");
        }

        return qualificationService
            .getTournamentQualificationScoreTable(tournamentId)
            .stream()
            .map(qualificationTeamScoreMapper::modelToDto)
            .toList();
    }

    @PermitAll
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = "{id}/qualification-phase/scores/public")
    @Operation(summary = "Get the score list of the qualification phase of a tournament")
    public List<QualificationTeamScoreDto> getTournamentQualificationScoreTablePublic(
        @PathVariable(name = "id") Long tournamentId,
        @RequestParam(name = "token") Optional<UUID> token
    ) {
        LOG.info("GET {}/{}/qualification-phase/scores/public", BASE_ENDPOINT, tournamentId);

        // Explicitly use an `Optional<>` and check it here, so we can return
        // the appropriate error
        // Otherwise, Spring Boot would just return a 400.
        if (token.isEmpty()) {
            throw new BadTournamentPublicAccessTokenException();
        }

        tournamentService.assertAccessTokenIsCorrect(tournamentId, token.get());

        return qualificationService
            .getTournamentQualificationScoreTable(tournamentId)
            .stream()
            .map(qualificationTeamScoreMapper::modelToDto)
            .toList();
    }

    // region Team
    @Secured("ROLE_USER")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = "{id}/teams")
    @Operation(
        summary = "Get teams for a tournament",
        security = @SecurityRequirement(name = "apiKey")
    )
    public List<TeamDto> getTournamentTeams(
        @PathVariable(name = "id") Long tournamentId,
        Authentication authentication
    ) {
        LOG.info("GET {}/{}/teams", BASE_ENDPOINT, tournamentId);

        // check if user is organizer of tournament
        if (!tournamentService.isOrganizer(authentication.getName(), tournamentId)) {
            throw new AccessDeniedException("Current user isn't organizer of tournament.");
        }

        return teamService
            .getTournamentTeams(tournamentId)
            .stream()
            .map(teamMapper::modelToDto)
            .toList();
    }

    @Secured("ROLE_USER")
    @ResponseStatus(HttpStatus.OK)
    @PutMapping(value = "{tournamentId}/teams/{teamId}")
    @Operation(
        summary = "Update a team in a tournament",
        security = @SecurityRequirement(name = "apiKey")
    )
    public ResponseEntity<TeamDto> updateTournamentTeam(
        @PathVariable("tournamentId") Long tournamentId,
        @PathVariable("teamId") Long teamId,
        @Valid @RequestBody TournamentUpdateTeamDto updateDto,
        Authentication authentication
    ) {
        LOG.info("PUT {}/{}/teams/{}", BASE_ENDPOINT, tournamentId, teamId);
        LOG.debug("request body: {}", updateDto);

        // check if user is organizer of tournament
        if (!tournamentService.isOrganizer(authentication.getName(), tournamentId)) {
            throw new AccessDeniedException("Current user isn't organizer of tournament.");
        }

        final var updatedTeam = teamService.updateTeam(tournamentId, teamId, updateDto);

        var headers = new HttpHeaders();
        headers.put(
            HttpHeaders.CONTENT_LOCATION,
            List.of("%s/%d/teams/%d".formatted(BASE_ENDPOINT, tournamentId, teamId))
        );

        return ResponseEntity.status(HttpStatus.OK)
            .headers(headers)
            .body(this.teamMapper.entityToDto(updatedTeam));
    }

    @Secured("ROLE_USER")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping(value = "{tournamentId}/teams/{teamId}")
    @Operation(
        summary = "Delete team from a tournament",
        security = @SecurityRequirement(name = "apiKey")
    )
    public ResponseEntity<Void> deleteTournamentTeam(
        @PathVariable("tournamentId") Long tournamentId,
        @PathVariable("teamId") Long teamId,
        Authentication authentication
    ) {
        LOG.info("DELETE {}/{}/teams/{}", BASE_ENDPOINT, tournamentId, teamId);

        // check if user is organizer of tournament
        if (!tournamentService.isOrganizer(authentication.getName(), tournamentId)) {
            throw new AccessDeniedException("Current user isn't organizer of tournament.");
        }

        teamService.deleteTeam(tournamentId, teamId);
        return ResponseEntity.noContent().build();
    }

    @Secured("ROLE_USER")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("{tournamentId}/teams/{teamId}/ready")
    @Operation(
        summary = "Mark a team as ready for a tournament",
        security = @SecurityRequirement(name = "apiKey")
    )
    public ResponseEntity<Void> markTeamAsReady(
        @PathVariable("tournamentId") long tournamentId,
        @PathVariable("teamId") long teamId,
        Authentication authentication
    ) {
        LOG.info("POST {}/{}/teams/{}/ready", BASE_ENDPOINT, tournamentId, teamId);

        // check if user is organizer of tournament
        if (!tournamentService.isOrganizer(authentication.getName(), tournamentId)) {
            throw new AccessDeniedException("Current user isn't organizer of tournament.");
        }

        teamService.markTeamAsReady(tournamentId, teamId);
        return ResponseEntity.noContent().build();
    }

    @PermitAll
    @ResponseStatus(HttpStatus.OK) // No location header, thus 200
    @PostMapping("{tournamentId}/teams")
    @Operation(summary = "Create a new team")
    public ResponseEntity<TournamentSignupTeamResponseDto> signupTeamForTournament(
        @PathVariable("tournamentId") long tournamentId,
        @RequestParam("token") Optional<UUID> selfRegistrationToken,
        @Valid @RequestBody TournamentUpdateTeamDto createTeamDto
    ) {
        LOG.info("POST {}/{}/teams?token={}", BASE_ENDPOINT, tournamentId, selfRegistrationToken);
        LOG.debug("request body: {}", createTeamDto);

        // Explicitly use an `Optional<>` and check it here, so we can return
        // the appropriate error
        // Otherwise, Spring Boot would just return a 400.
        if (selfRegistrationToken.isEmpty()) {
            throw new BadTournamentPublicAccessTokenException();
        }

        final var signupResult = teamService.signupTeamForTournament(
            tournamentId,
            selfRegistrationToken.get(),
            createTeamDto.name()
        );

        if (signupResult != SignupTeamResult.SUCCESS) {
            return ResponseEntity.badRequest()
                .body(new TournamentSignupTeamResponseDto(signupResult));
        }

        return ResponseEntity.ok(new TournamentSignupTeamResponseDto(SignupTeamResult.SUCCESS));
    }

    // endregion team

    @GetMapping("{tournamentId}/ko-matches/queued")
    @Operation(
        summary = "Get upcoming knock-out matches for a tournament",
        security = @SecurityRequirement(name = "apiKey")
    )
    @ResponseStatus(HttpStatus.OK)
    @Secured("ROLE_USER")
    public List<QueuedMatchDto> getQueuedKoMatches(
        @PathVariable(name = "tournamentId") Long tournamentId,
        Authentication authentication
    ) {
        LOG.info("GET {}/{}/ko-matches/queued", BASE_ENDPOINT, tournamentId);

        // check if user is organizer of tournament
        if (!tournamentService.isOrganizer(authentication.getName(), tournamentId)) {
            throw new AccessDeniedException("Current user isn't organizer of tournament.");
        }

        var queuedKoMatches = matchDomainService.getKoMatchQueue(tournamentId);
        return queuedKoMatches.stream().map(QueuedMatchDto::fromMatch).toList();
    }
}
