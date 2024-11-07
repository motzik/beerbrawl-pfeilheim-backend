/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.service;

import at.beerbrawl.backend.endpoint.dto.SharedMediaCreateDto;
import at.beerbrawl.backend.endpoint.dto.SharedMediaMetadataDto;
import at.beerbrawl.backend.entity.SharedMedia;
import at.beerbrawl.backend.enums.MediaState;
import at.beerbrawl.backend.exception.NotFoundException;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

public interface SharedMediaService {
    /**
     * Find all shared media by a specific tournament ID.
     *
     * @param tournamentId The ID of the tournament
     * @return List of shared media entries for the given tournament
     */
    List<SharedMediaMetadataDto> findAllByTournamentIdWithoutImage(
        Long tournamentId,
        boolean onlyApproved
    );

    /**
     * Create a shared media entry.
     *
     * @param sharedMediaCreateDto The shared media entity to create
     * @return The created shared media entity
     */
    SharedMedia create(SharedMediaCreateDto sharedMediaCreateDto, MultipartFile image);

    /**
     * Find a shared media entry by its ID.
     *
     * @param id The ID of the shared media entity
     * @return The shared media entity
     * @throws NotFoundException if the shared media is not found
     */
    SharedMedia findOne(Long id) throws NotFoundException;

    /**
     * Delete a shared media entry by its ID.
     *
     * @param id The ID of the shared media entity
     * @throws NotFoundException if the shared media is not found
     * @throws AccessDeniedException if the current user does not have permission to delete the shared media
     */
    void delete(Long id) throws NotFoundException, AccessDeniedException;

    /**
     * Set the state of a shared media entry by its ID.
     *
     * @param id The ID of the shared media entity
     * @param state The new state of the shared media entity
     * @throws NotFoundException if the shared media is not found
     */
    void setState(Long id, MediaState state) throws NotFoundException;
}
