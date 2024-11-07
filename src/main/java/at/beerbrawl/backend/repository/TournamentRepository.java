/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.repository;

import at.beerbrawl.backend.entity.Tournament;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    /**
     * Find all tournament entries ordered by name (ascending).
     *
     * @return ordered list of all tournament entries
     */
    List<Tournament> findAllByOrderByNameAsc();

    /**
     * Find all tournaments where the organizer's ID matches the provided ID,
     * ordered by name (ascending).
     *
     * @param organizerId The ID of the organizer
     * @return List of tournaments organized by the provided ID, ordered by name
     */
    List<Tournament> findAllByOrganizerIdOrderByNameAsc(Long organizerId);

    /**
     * Find all tournaments where the organizer's username matches the provided username.
     *
     * @param organizerUsername The username of the organizer
     * @return List of tournaments organized by the provided username
     */
    List<Tournament> findAllByOrganizerUsername(String organizerUsername);

    /**
     * Find all tournaments with their qualification matches eagerly loaded
     * where the organizer's ID matches the provided ID, ordered by name (ascending).
     *
     * @param organizerId The ID of the organizer
     * @return List of tournaments with qualification matches, ordered by name
     */
    @Query(
        "SELECT t FROM Tournament t LEFT JOIN FETCH t.qualificationMatches WHERE t.organizer.id = :organizerId ORDER BY t.name ASC"
    )
    List<Tournament> findAllWithQualificationMatchesByOrganizerIdOrderByNameAsc(
        @Param("organizerId") Long organizerId
    );

    /**
     * Check if a tournament with the given name already exists.
     *
     * @param name of the tournament
     * @return wether the tournament with the given name exists
     */
    Boolean existsByName(String name);

    /**
     * Delete a tournament by its ID.
     *
     * @param id The ID of the tournament to delete
     */
    void deleteById(Long id);
}
