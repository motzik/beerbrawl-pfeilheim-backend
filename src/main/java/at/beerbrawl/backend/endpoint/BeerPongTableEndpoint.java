/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint;

import at.beerbrawl.backend.endpoint.dto.BeerPongTableDto;
import at.beerbrawl.backend.endpoint.dto.CreateBeerPongTableDto;
import at.beerbrawl.backend.endpoint.dto.UpdateBeerPongTableDto;
import at.beerbrawl.backend.endpoint.mapper.BeerPongTableMapper;
import at.beerbrawl.backend.service.BeerPongTableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.lang.invoke.MethodHandles;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = BeerPongTableEndpoint.BASE_ENDPOINT)
public class BeerPongTableEndpoint {

    public static final String BASE_ENDPOINT = "/api/v1/beer-pong-tables";
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final BeerPongTableService beerPongTableService;
    private final BeerPongTableMapper beerPongTableMapper;

    public BeerPongTableEndpoint(
        BeerPongTableService beerPongTableService,
        BeerPongTableMapper beerPongTableMapper
    ) {
        this.beerPongTableService = beerPongTableService;
        this.beerPongTableMapper = beerPongTableMapper;
    }

    @Secured("ROLE_USER")
    @GetMapping(value = "{id}")
    @Operation(
        summary = "Create a new beerpong table for a tournament",
        security = @SecurityRequirement(name = "apiKey")
    )
    public BeerPongTableDto getBeerPongTable(@PathVariable(name = "id") Long beerPongTableId) {
        LOG.info("GET /api/v1/beer-pong-tables/{}", beerPongTableId);

        return beerPongTableMapper.entityToDto(beerPongTableService.findById(beerPongTableId));
    }

    @Secured("ROLE_USER")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @Operation(
        summary = "Create a new beerpong table for a tournament",
        security = @SecurityRequirement(name = "apiKey")
    )
    public BeerPongTableDto createBeerPongTable(
        @Valid @RequestBody CreateBeerPongTableDto createDto,
        Authentication authentication
    ) {
        LOG.info("POST /api/v1/beer-pong-tables body: {}", createDto);

        return beerPongTableMapper.entityToDto(
            beerPongTableService.create(createDto, authentication.getName())
        );
    }

    @Secured("ROLE_USER")
    @ResponseStatus(HttpStatus.OK)
    @PutMapping(value = "{id}")
    @Operation(
        summary = "Update an existing beerpong table for a tournament",
        security = @SecurityRequirement(name = "apiKey")
    )
    public BeerPongTableDto updateBeerPongTable(
        @PathVariable(name = "id") Long beerPongTableId,
        @Valid @RequestBody UpdateBeerPongTableDto updateDto,
        Authentication authentication
    ) {
        LOG.info("PUT /api/v1/beer-pong-tables/{} body: {}", beerPongTableId, updateDto);

        return beerPongTableMapper.entityToDto(
            beerPongTableService.update(beerPongTableId, updateDto, authentication.getName())
        );
    }

    /**
     * Find beer pong tables by tournament ID.
     *
     * @param tournamentId ID of the tournament
     * @return list of beerpong table entities
     */
    @Secured("ROLE_USER")
    @GetMapping(value = "tournament/{tournamentId}")
    @Operation(
        summary = "Find beer pong tables by tournament ID",
        security = @SecurityRequirement(name = "apiKey")
    )
    public List<BeerPongTableDto> findBeerPongTablesByTournamentId(
        @PathVariable(name = "tournamentId") Long tournamentId,
        Authentication authentication
    ) {
        LOG.info("GET /api/v1/beer-pong-tables/tournament/{}", tournamentId);

        return beerPongTableMapper.entityToDtoList(
            beerPongTableService.findByTournamentId(tournamentId, authentication.getName())
        );
    }

    @Secured("ROLE_USER")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping(value = "{id}")
    @Operation(
        summary = "Delete an existing beerpong table",
        security = @SecurityRequirement(name = "apiKey")
    )
    public void deleteBeerPongTable(
        @PathVariable(name = "id") Long beerPongTableId,
        Authentication authentication
    ) {
        LOG.info("DELETE /api/v1/beer-pong-tables/{}", beerPongTableId);

        beerPongTableService.delete(beerPongTableId, authentication.getName());
    }
}
