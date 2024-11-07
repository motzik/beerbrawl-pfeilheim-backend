/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.service;

import at.beerbrawl.backend.endpoint.dto.CreateBeerPongTableDto;
import at.beerbrawl.backend.endpoint.dto.UpdateBeerPongTableDto;
import at.beerbrawl.backend.entity.BeerPongTable;
import at.beerbrawl.backend.exception.NotFoundException;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;

public interface BeerPongTableService {
    /**
     * Loads a single beerpong table by its id.
     *
     * @param beerPongTableId beerpong table to load
     * @return found beerpong table entity
     */
    BeerPongTable findById(Long beerPongTableId) throws NotFoundException;

    /**
     * Create a single beerpong table entity for a tournament.
     *
     * @param beerPongTable   to create
     * @param currentUserName username of the current user, is used to check if user
     *                        has access
     * @return created beerpong table entity
     */
    BeerPongTable create(CreateBeerPongTableDto beerPongTable, String currentUserName)
        throws AccessDeniedException;

    /**
     * Update a single beerpong table entity.
     *
     * @param beerPongTableId id of beerpong table to update
     * @param beerPongTable   to update
     * @param currentUserName username of the current user, is used to check if user
     *                        has access
     * @return updated beerpong table entity
     */
    BeerPongTable update(
        Long beerPongTableId,
        UpdateBeerPongTableDto beerPongTable,
        String currentUserName
    ) throws AccessDeniedException, NotFoundException;

    /**
     * Finds beerpong tables by tournament ID.
     *
     * @param tournamentId ID of the tournament
     * @return list of found beerpong table entities
     */
    List<BeerPongTable> findByTournamentId(Long tournamentId, String currentUser)
        throws NotFoundException, AccessDeniedException;

    /**
     * Deletes a single beerpong table entity by its id.
     *
     * @param beerPongTableId id of beerpong table to delete
     * @param currentUserName username of the current user, is used to check if user
     *                        has access
     * @throws AccessDeniedException if the current user does not have permission to delete the table
     * @throws NotFoundException if the beerpong table is not found
     */
    void delete(Long beerPongTableId, String currentUserName)
        throws AccessDeniedException, NotFoundException;
}
