/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint.mapper;

import at.beerbrawl.backend.endpoint.dto.SharedMediaCreateDto;
import at.beerbrawl.backend.endpoint.dto.SharedMediaMetadataDto;
import at.beerbrawl.backend.entity.SharedMedia;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface SharedMediaMapper {
    @Mapping(source = "tournament.id", target = "tournamentId")
    SharedMediaMetadataDto entityToDto(SharedMedia sharedMedia);

    List<SharedMediaMetadataDto> entityToDto(List<SharedMedia> sharedMedia);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tournament", ignore = true)
    SharedMedia dtoToEntity(SharedMediaMetadataDto sharedMediaMetadataDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tournament", ignore = true)
    SharedMedia createDtoToEntity(SharedMediaCreateDto sharedMediaCreateDto);
}
