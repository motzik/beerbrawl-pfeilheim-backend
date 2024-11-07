/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint;

import at.beerbrawl.backend.endpoint.dto.GenerateKoMatchesDto;
import at.beerbrawl.backend.endpoint.dto.KoStandingDto;
import at.beerbrawl.backend.endpoint.dto.TournamentUpdateKoStandingDto;
import at.beerbrawl.backend.endpoint.mapper.TournamentKoPhaseMapper;
import at.beerbrawl.backend.service.TournamentKoPhaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.security.PermitAll;
import java.lang.invoke.MethodHandles;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = KoStandingsEndpoint.BASE_ENDPOINT)
@AllArgsConstructor
public class KoStandingsEndpoint {

    public static final String BASE_ENDPOINT = "/api/v1/tournaments/{id}/ko-standings";
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final TournamentKoPhaseService koStandingsService;
    private final TournamentKoPhaseMapper koStandingsMapper;

    @Secured("ROLE_USER")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @Operation(
        summary = "Generate KO matches for tournament",
        security = @SecurityRequirement(name = "apiKey")
    )
    public void generateKoMatches(
        @PathVariable(name = "id") Long tournamentId,
        @RequestBody GenerateKoMatchesDto generateKoMatchesDto,
        Authentication authentication
    ) {
        LOG.info("POST /api/v1/tournaments/{}/ko-standings", tournamentId);

        this.koStandingsService.generateKoMatchesForTournament(
                tournamentId,
                generateKoMatchesDto.qualifiedTeamIds(),
                authentication.getName()
            );
    }

    @PermitAll
    @GetMapping
    @Operation(
        summary = "Get KO standings for tournament",
        security = @SecurityRequirement(name = "apiKey")
    )
    public KoStandingDto getKoStandingsTree(@PathVariable(name = "id") Long tournamentId) {
        LOG.info("GET /api/v1/tournaments/{}/ko-standings", tournamentId);

        final var standings = koStandingsService.getKoStandingsTree(tournamentId);
        return koStandingsMapper.entityToDto(standings);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Secured("ROLE_USER")
    @PutMapping(path = "{standingId}")
    @Operation(
        summary = "Update KO standings for tournament",
        security = @SecurityRequirement(name = "apiKey")
    )
    public KoStandingDto updateKoStanding(
        @PathVariable(name = "id") Long tournamentId,
        @PathVariable(name = "standingId") Long standingId,
        @RequestBody TournamentUpdateKoStandingDto updateStandingDto,
        Authentication authentication
    ) {
        LOG.info("PUT /api/v1/tournaments/{}/ko-standings/{}", tournamentId, standingId);
        LOG.debug("request body: {}", updateStandingDto);

        this.koStandingsService.updateKoStanding(
                authentication.getName(),
                tournamentId,
                standingId,
                updateStandingDto
            );

        return this.koStandingsMapper.entityToDto(
                this.koStandingsService.getStandingById(standingId)
            );
    }
}
