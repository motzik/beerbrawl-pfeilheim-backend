/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.repository;

import at.beerbrawl.backend.entity.BeerPongTable;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BeerPongTableRepository extends JpaRepository<BeerPongTable, Long> {
    List<BeerPongTable> findAllByTournamentId(long tournamentId);

    List<BeerPongTable> findByTournamentIdAndCurrentMatchIsNull(long tournamentId);

    /**
     * Check if a table with the given name already exists.
     *
     * @param name of the table
     * @return wether the table with the given name exists
     */
    Boolean existsByNameAndTournamentIdIs(String name, Long tournamentId);

    /**
     * Check if a table with the given name already exists, whose id isn't the same as the parameters.
     *
     * @param name of the table
     * @param id of the table that should't be matched
     * @return wether the table with the given name exists
     */
    Boolean existsByNameAndIdNotAndTournamentIdIs(String name, Long id, Long tournamentId);
}
