/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.service.impl;

import at.beerbrawl.backend.endpoint.dto.CreateBeerPongTableDto;
import at.beerbrawl.backend.endpoint.dto.UpdateBeerPongTableDto;
import at.beerbrawl.backend.entity.BeerPongTable;
import at.beerbrawl.backend.entity.domainservice.MatchDomainService;
import at.beerbrawl.backend.exception.NotFoundException;
import at.beerbrawl.backend.exception.PreconditionFailedException;
import at.beerbrawl.backend.repository.BeerPongTableRepository;
import at.beerbrawl.backend.repository.TournamentRepository;
import at.beerbrawl.backend.service.BeerPongTableService;
import java.lang.invoke.MethodHandles;
import java.util.List;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class BeerPongTableServiceImpl implements BeerPongTableService {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        MethodHandles.lookup().lookupClass()
    );

    private final BeerPongTableRepository beerPongTableRepository;
    private final TournamentRepository tournamentRepository;
    private MatchDomainService matchDomainService;

    @Override
    public BeerPongTable findById(Long beerPongTableId) throws NotFoundException {
        LOGGER.debug("Get a beer pong table by its id {}", beerPongTableId);

        return beerPongTableRepository
            .findById(beerPongTableId)
            .orElseThrow(() -> new NotFoundException("No beerpong table found."));
    }

    @Override
    @Transactional
    public BeerPongTable create(CreateBeerPongTableDto beerPongTable, String currentUser) {
        LOGGER.debug("Create new beer pong table {}", beerPongTable);

        var tournament = tournamentRepository
            .findById(beerPongTable.getTournamentId())
            .orElseThrow(() -> new NotFoundException("Tournament not found."));

        if (
            tournament.getOrganizer() == null ||
            !tournament.getOrganizer().getUsername().equals(currentUser)
        ) {
            LOGGER.debug(
                "Couldn't create beer pong table for tournament with id {}, because the user who started tried to create it isn't the same as the organizer of the tournament.",
                beerPongTable.getTournamentId()
            );
            throw new AccessDeniedException("Current user isn't organizer of tournament.");
        }

        if (
            beerPongTableRepository.existsByNameAndTournamentIdIs(
                beerPongTable.getName(),
                beerPongTable.getTournamentId()
            )
        ) {
            LOGGER.debug(
                "Couldn't create beer pong table for tournament with id {}, because a table with the name '{}' already exists.",
                beerPongTable.getTournamentId(),
                beerPongTable.getName()
            );
            throw new PreconditionFailedException("Beer pong table with name already exists.");
        }

        var entity = new BeerPongTable(beerPongTable.getName(), tournament);

        beerPongTableRepository.save(entity);
        matchDomainService.scheduleQualiMatches(tournament.getId());
        matchDomainService.scheduleKoMatches(tournament.getId());

        return entity;
    }

    @Override
    public BeerPongTable update(
        Long beerPongTableId,
        UpdateBeerPongTableDto beerPongTable,
        String currentUser
    ) {
        LOGGER.debug("Update new beer pong table {}", beerPongTable);

        var entity = beerPongTableRepository
            .findById(beerPongTableId)
            .orElseThrow(() -> new NotFoundException("Beerpong table not found."));

        if (entity.getTournament() == null) {
            LOGGER.debug(
                "Couldn't update beer pong table with id {}, because it has no tournament assigned to it.",
                beerPongTableId
            );
            throw new PreconditionFailedException("Beer pong table has no tournament assigned.");
        }

        var tournament = tournamentRepository
            .findById(entity.getTournament().getId())
            .orElseThrow(() -> new NotFoundException("Tournament not found."));

        if (
            tournament.getOrganizer() == null ||
            !tournament.getOrganizer().getUsername().equals(currentUser)
        ) {
            LOGGER.debug(
                "Couldn't update beer pong table with id {}, because the user who started tried to update it isn't the same as the organizer of the tournament.",
                beerPongTableId
            );
            throw new AccessDeniedException("Current user isn't organizer of tournament.");
        }

        if (
            beerPongTableRepository.existsByNameAndIdNotAndTournamentIdIs(
                beerPongTable.getName(),
                beerPongTableId,
                entity.getTournament().getId()
            )
        ) {
            LOGGER.debug(
                "Couldn't update beer pong table with id {}, because a table with the name '{}' already exists.",
                beerPongTableId,
                beerPongTable.getName()
            );
            throw new PreconditionFailedException("Beer pong table with name already exists.");
        }

        entity.setName(beerPongTable.getName());
        beerPongTableRepository.save(entity);

        return entity;
    }

    /**
     * Finds beerpong tables by tournament ID.
     *
     * @param tournamentId ID of the tournament
     * @return list of found beerpong table entities
     */
    public List<BeerPongTable> findByTournamentId(Long tournamentId, String currentUser)
        throws AccessDeniedException, NotFoundException {
        LOGGER.debug("Find beer pong tables by tournament id {}", tournamentId);

        var tournament = tournamentRepository
            .findById(tournamentId)
            .orElseThrow(() -> new NotFoundException("Tournament not found."));

        if (
            tournament.getOrganizer() == null ||
            !tournament.getOrganizer().getUsername().equals(currentUser)
        ) {
            LOGGER.debug(
                "Access denied for tournament with id {}: current user is not the organizer of the tournament.",
                tournamentId
            );
            throw new AccessDeniedException("Current user isn't organizer of tournament.");
        }

        return beerPongTableRepository.findAllByTournamentId(tournamentId);
    }

    @Override
    public void delete(Long beerPongTableId, String currentUser)
        throws AccessDeniedException, NotFoundException {
        LOGGER.debug("Delete beer pong table with id {}", beerPongTableId);

        var entity = beerPongTableRepository
            .findById(beerPongTableId)
            .orElseThrow(() -> new NotFoundException("Beerpong table not found."));

        if (entity.getTournament() == null) {
            LOGGER.debug(
                "Couldn't delete beer pong table with id {}, because it has no tournament assigned to it.",
                beerPongTableId
            );
            throw new PreconditionFailedException("Beer pong table has no tournament assigned.");
        }

        var tournament = tournamentRepository
            .findById(entity.getTournament().getId())
            .orElseThrow(() -> new NotFoundException("Tournament not found."));

        if (
            tournament.getOrganizer() == null ||
            !tournament.getOrganizer().getUsername().equals(currentUser)
        ) {
            LOGGER.debug(
                "Couldn't delete beer pong table with id {}, because the user who tried to delete it isn't the organizer of the tournament.",
                beerPongTableId
            );
            throw new AccessDeniedException("Current user isn't organizer of tournament.");
        }

        beerPongTableRepository.delete(entity);
        LOGGER.debug("Beer pong table with id {} has been deleted", beerPongTableId);
    }
}
