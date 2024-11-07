/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint.mapper;

import at.beerbrawl.backend.endpoint.dto.TeamDto;
import at.beerbrawl.backend.entity.Team;
import at.beerbrawl.backend.service.models.TeamModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface TeamMapper {
    TeamDto entityToDto(Team entity);

    @Mapping(target = "currentlyPlaying", expression = "java(currentlyPlaying)")
    TeamModel entityToModel(Team entity, boolean currentlyPlaying);

    TeamDto modelToDto(TeamModel model);
}
