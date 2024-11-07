/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint.mapper;

import at.beerbrawl.backend.endpoint.dto.TournamentOverviewDto;
import at.beerbrawl.backend.service.models.TournamentOverviewModel;
import org.mapstruct.Mapper;

@Mapper
public interface TournamentOverviewMapper {
    TournamentOverviewDto modelToDto(TournamentOverviewModel model);
}
