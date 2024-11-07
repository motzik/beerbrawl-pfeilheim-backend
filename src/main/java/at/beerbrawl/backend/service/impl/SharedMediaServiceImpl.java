/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.service.impl;

import at.beerbrawl.backend.endpoint.dto.SharedMediaCreateDto;
import at.beerbrawl.backend.endpoint.dto.SharedMediaMetadataDto;
import at.beerbrawl.backend.entity.SharedMedia;
import at.beerbrawl.backend.enums.MediaState;
import at.beerbrawl.backend.exception.NotFoundException;
import at.beerbrawl.backend.model.Notification;
import at.beerbrawl.backend.repository.SharedMediaRepository;
import at.beerbrawl.backend.repository.TournamentRepository;
import at.beerbrawl.backend.service.SharedMediaService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SharedMediaServiceImpl implements SharedMediaService {

    private final SharedMediaRepository sharedMediaRepository;
    private final TournamentRepository tournamentRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public SharedMediaServiceImpl(
        SharedMediaRepository sharedMediaRepository,
        TournamentRepository tournamentRepository,
        SimpMessagingTemplate messagingTemplate
    ) {
        this.sharedMediaRepository = sharedMediaRepository;
        this.tournamentRepository = tournamentRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public List<SharedMediaMetadataDto> findAllByTournamentIdWithoutImage(
        Long tournamentId,
        boolean onlyApproved
    ) {
        if (onlyApproved) {
            return sharedMediaRepository.findAllPublicByTournamentIdWithoutImage(tournamentId);
        } else {
            return sharedMediaRepository.findAllByTournamentIdWithoutImage(tournamentId);
        }
    }

    public SharedMedia create(SharedMediaCreateDto sharedMediaCreateDto, MultipartFile image)
        throws NotFoundException {
        validateAndProcessImage(sharedMediaCreateDto, image);

        SharedMedia sharedMedia = new SharedMedia();
        sharedMedia.setAuthor(sharedMediaCreateDto.getAuthor());
        sharedMedia.setTitle(sharedMediaCreateDto.getTitle());
        sharedMedia.setImage(sharedMediaCreateDto.getImage());
        sharedMedia.setState(MediaState.PENDING);

        var tournament = tournamentRepository
            .findById(sharedMediaCreateDto.getTournamentId())
            .orElseThrow(() -> new NotFoundException("Tournament not found"));

        // notify all clients about new image
        messagingTemplate.convertAndSend(
            "/partypics/notifications/" + tournament.getOrganizer().getUsername(),
            new Notification(
                tournament.getName() + ": " + sharedMedia.getAuthor() + " uploaded a new image.",
                tournament.getId()
            )
        );

        sharedMedia.setTournament(tournament);

        return sharedMediaRepository.saveAndFlush(sharedMedia);
    }

    private void validateAndProcessImage(
        SharedMediaCreateDto sharedMediaCreateDto,
        MultipartFile image
    ) {
        if (image.isEmpty() || image.getSize() > SharedMedia.MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Invalid image file size");
        }

        boolean isAllowedType = false;
        for (String type : SharedMedia.Companion.getAllowedTypes()) {
            if (type.equalsIgnoreCase(image.getContentType())) {
                isAllowedType = true;
                break;
            }
        }

        if (!isAllowedType) {
            throw new IllegalArgumentException("Invalid image file type");
        }

        try {
            BufferedImage bufferedImage = ImageIO.read(image.getInputStream());
            if (bufferedImage == null) {
                throw new IllegalArgumentException("Invalid image content");
            }

            if (
                bufferedImage.getWidth() > SharedMedia.MAX_IMAGE_WIDTH ||
                bufferedImage.getHeight() > SharedMedia.MAX_IMAGE_HEIGHT
            ) {
                throw new IllegalArgumentException("Invalid image resolution");
            }

            // Convert image to a format compatible with JPEG
            BufferedImage convertedImage = new BufferedImage(
                bufferedImage.getWidth(),
                bufferedImage.getHeight(),
                BufferedImage.TYPE_INT_RGB
            );
            convertedImage
                .createGraphics()
                .drawImage(bufferedImage, 0, 0, java.awt.Color.WHITE, null);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            boolean result = ImageIO.write(convertedImage, "jpeg", baos);
            if (!result) {
                throw new RuntimeException("Failed to write image");
            }
            baos.flush();
            byte[] imageBytes = baos.toByteArray();
            baos.close();

            sharedMediaCreateDto.setImage(imageBytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to process image", e);
        }
    }

    @Override
    public SharedMedia findOne(Long id) throws NotFoundException {
        return sharedMediaRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("SharedMedia not found"));
    }

    @Override
    public void delete(Long id) throws NotFoundException, AccessDeniedException {
        SharedMedia sharedMedia = findOne(id);
        sharedMediaRepository.delete(sharedMedia);
    }

    @Override
    public void setState(Long id, MediaState state) throws NotFoundException {
        SharedMedia sharedMedia = sharedMediaRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("SharedMedia not found"));
        sharedMedia.setState(state);
        sharedMediaRepository.saveAndFlush(sharedMedia);
    }
}
