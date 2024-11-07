/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint.mapper;

import at.beerbrawl.backend.endpoint.dto.BeerPongTableDto;
import at.beerbrawl.backend.entity.BeerPongTable;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface BeerPongTableMapper {
    @Mapping(target = "tournamentId", source = "tournament.id")
    BeerPongTableDto entityToDto(BeerPongTable entity);

    List<BeerPongTableDto> entityToDtoList(List<BeerPongTable> entities);
}
