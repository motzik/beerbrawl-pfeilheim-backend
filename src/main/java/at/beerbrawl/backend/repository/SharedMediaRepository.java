/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.repository;

import at.beerbrawl.backend.endpoint.dto.SharedMediaMetadataDto;
import at.beerbrawl.backend.entity.SharedMedia;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SharedMediaRepository extends JpaRepository<SharedMedia, Long> {
    /**
     * Find all shared media by a specific tournament id without the image field.
     *
     * @param tournamentId The tournament id
     * @return List of shared media entries for the given tournament without the image field
     */
    @Query(
        "SELECT new at.beerbrawl.backend.endpoint.dto.SharedMediaMetadataDto(sm.id, sm.author, sm.title, sm.state, sm.tournament.id) " +
        "FROM SharedMedia sm WHERE sm.tournament.id = :tournamentId"
    )
    List<SharedMediaMetadataDto> findAllByTournamentIdWithoutImage(
        @Param("tournamentId") Long tournamentId
    );

    /**
     * Find all shared media by a specific tournament id without the image field.
     *
     * @param tournamentId The tournament id
     * @return List of shared media entries for the given tournament without the image field
     */
    @Query(
        "SELECT new at.beerbrawl.backend.endpoint.dto.SharedMediaMetadataDto(sm.id, sm.author, sm.title, sm.state, sm.tournament.id) " +
        "FROM SharedMedia sm WHERE sm.tournament.id = :tournamentId AND sm.state = 'APPROVED'"
    )
    List<SharedMediaMetadataDto> findAllPublicByTournamentIdWithoutImage(
        @Param("tournamentId") Long tournamentId
    );

    /**
     * Find all shared media with a specific author.
     *
     * @param author The author of the shared media
     * @return List of shared media entries by the given author
     */
    List<SharedMedia> findAllByAuthor(String author);
}
