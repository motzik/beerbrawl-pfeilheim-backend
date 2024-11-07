/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint.mapper;

import at.beerbrawl.backend.endpoint.dto.QualificationTeamScoreDto;
import at.beerbrawl.backend.service.models.QualificationTeamScoreModel;
import org.mapstruct.Mapper;

@Mapper
public interface QualificationTeamScoreMapper {
    QualificationTeamScoreDto modelToDto(QualificationTeamScoreModel entity);
}
