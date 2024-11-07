/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint

import at.beerbrawl.backend.endpoint.dto.SharedMediaCreateDto
import at.beerbrawl.backend.endpoint.dto.SharedMediaMetadataDto
import at.beerbrawl.backend.endpoint.dto.SharedMediaUpdateStateDto
import at.beerbrawl.backend.endpoint.mapper.SharedMediaMapper
import at.beerbrawl.backend.enums.MediaState
import at.beerbrawl.backend.exception.NotFoundException
import at.beerbrawl.backend.service.SharedMediaService
import jakarta.annotation.security.PermitAll
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.annotation.Secured
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.lang.invoke.MethodHandles

/**
 * REST endpoint for shared media.
 *
 * @param sharedMediaService
 * @param sharedMediaMapper
 */
@RestController
@RequestMapping(value = [SharedMediaEndpoint.BASE_ENDPOINT])
class SharedMediaEndpoint(
    @Autowired private val sharedMediaService: SharedMediaService,
    @Autowired private val sharedMediaMapper: SharedMediaMapper,
) {
    /**
     * Create a new shared media.
     *
     * @param sharedMediaCreateDto the shared media to create
     * @param image the image of the shared media
     * @return the created shared media
     */
    @PermitAll
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createSharedMedia(
        @Validated @RequestPart("sharedMediaCreateDto") sharedMediaCreateDto: SharedMediaCreateDto,
        @RequestPart("image") image: MultipartFile,
    ): SharedMediaMetadataDto {
        log.info("POST {}", BASE_ENDPOINT)
        log.debug("request body: {}", sharedMediaCreateDto)
        val sharedMedia = sharedMediaService.create(sharedMediaCreateDto, image)
        return sharedMediaMapper.entityToDto(sharedMedia)
    }

    /**
     * Get all shared media.
     *
     * @param tournamentId
     * @return all shared media
     * @return all shared media
     * @throws NotFoundException if no shared media is found
     * @throws AccessDeniedException if the user is not allowed to access the shared media
     * @throws Exception if an unexpected error occurs
     */
    @Secured("ROLE_USER")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = ["/tournament/{tournamentId}"], produces = ["application/json"])
    fun getSharedMediaByTournament(
        @PathVariable(name = "tournamentId") tournamentId: Long,
    ): ResponseEntity<List<SharedMediaMetadataDto>> {
        log.info("GET {}/tournament/{}", BASE_ENDPOINT, tournamentId)
        val sharedMediaMetadataDtos =
            sharedMediaService.findAllByTournamentIdWithoutImage(
                tournamentId,
                false,
            )
        return ResponseEntity.ok(sharedMediaMetadataDtos)
    }

    /**
     * Get all public shared media.
     *
     * @param tournamentId
     * @return all public shared media
     * @return all public shared media
     * @throws NotFoundException if no shared media is found
     * @throws AccessDeniedException if the user is not allowed to access the shared media
     * @throws Exception if an unexpected error occurs
     */
    @PermitAll
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = ["/tournament/public/{tournamentId}"], produces = ["application/json"])
    fun getPublicSharedMediaByTournament(
        @PathVariable(name = "tournamentId") tournamentId: Long,
    ): ResponseEntity<List<SharedMediaMetadataDto>> {
        log.info("GET {}/tournament/public/{}", BASE_ENDPOINT, tournamentId)
        val sharedMediaMetadataDtos =
            sharedMediaService.findAllByTournamentIdWithoutImage(
                tournamentId,
                true,
            )
        return ResponseEntity.ok(sharedMediaMetadataDtos)
    }

    /**
     * Get all shared media.
     *
     * @param sharedMediaId
     * @return all shared media
     * @throws NotFoundException if no shared media is found
     * @throws AccessDeniedException if the user is not allowed to access the shared media
     * @throws Exception if an unexpected error occurs
     */
    @Secured("ROLE_USER")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping(value = ["/{sharedMediaId}"])
    fun deleteSharedMedia(
        @PathVariable(name = "sharedMediaId") sharedMediaId: Long?,
    ): ResponseEntity<Void> {
        log.info("DELETE {}/{}", BASE_ENDPOINT, sharedMediaId)
        try {
            sharedMediaService.delete(sharedMediaId)
            return ResponseEntity.noContent().build()
        } catch (e: NotFoundException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        } catch (e: AccessDeniedException) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
    }

    /**
     * Get the image of a shared media.
     *
     * @param sharedMediaId the id of the shared media
     * @return the image of the shared media
     * @throws NotFoundException if the shared media is not found
     */
    @Secured("ROLE_USER")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = ["/image/{sharedMediaId}"], produces = [MediaType.IMAGE_JPEG_VALUE])
    fun getSharedMediaImage(
        @PathVariable(name = "sharedMediaId") sharedMediaId: Long,
    ): ResponseEntity<ByteArray> {
        log.info("GET {}/image/{}", BASE_ENDPOINT, sharedMediaId)
        try {
            val image = sharedMediaService.findOne(sharedMediaId).image
            return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"$sharedMediaId.jpg\"",
                )
                .body(image)
        } catch (e: NotFoundException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
    }

    /**
     * Get the image of a public shared media.
     *
     * @param sharedMediaId the id of the shared media
     * @return the image of the shared media
     * @throws NotFoundException if the shared media is not found
     * @throws AccessDeniedException if the shared media is not public
     */
    @PermitAll
    @ResponseStatus(HttpStatus.OK)
    @GetMapping(value = ["/image/public/{sharedMediaId}"], produces = [MediaType.IMAGE_JPEG_VALUE])
    fun getPublicSharedMediaImage(
        @PathVariable(name = "sharedMediaId") sharedMediaId: Long,
    ): ResponseEntity<ByteArray> {
        log.info("GET {}/image/public/{}", BASE_ENDPOINT, sharedMediaId)
        try {
            val image = sharedMediaService.findOne(sharedMediaId)
            if (image.state != MediaState.APPROVED) {
                throw AccessDeniedException("Image is not public")
            }
            return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"$sharedMediaId.jpg\"",
                )
                .body(image.image)
        } catch (e: NotFoundException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        } catch (e: AccessDeniedException) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
    }

    /**
     * Update the state of a shared media.
     *
     * @param id the id of the shared media
     * @param state the new state of the shared media
     * @return the updated shared media
     */
    @Secured("ROLE_USER")
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/{sharedMediaId}")
    fun updateState(
        @PathVariable(name = "sharedMediaId") id: Long,
        @RequestBody state: SharedMediaUpdateStateDto,
    ): ResponseEntity<Void> {
        log.info("PUT {}/{}", BASE_ENDPOINT, id)
        sharedMediaService.setState(id, state.state)
        return ResponseEntity(HttpStatus.OK)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
        const val BASE_ENDPOINT: String = "/api/v1/shared-media"
    }
}
