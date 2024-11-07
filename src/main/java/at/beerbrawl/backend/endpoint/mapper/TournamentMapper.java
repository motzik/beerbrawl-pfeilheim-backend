/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint.mapper;

import at.beerbrawl.backend.endpoint.dto.BeerPongTableDto;
import at.beerbrawl.backend.endpoint.dto.CreateTournamentDto;
import at.beerbrawl.backend.endpoint.dto.TournamentDto;
import at.beerbrawl.backend.endpoint.dto.TournamentListDto;
import at.beerbrawl.backend.endpoint.dto.TournamentQualificationMatchDto;
import at.beerbrawl.backend.endpoint.dto.TournamentQualificationMatchParticipantDto;
import at.beerbrawl.backend.endpoint.dto.TournamentUpdateDto;
import at.beerbrawl.backend.entity.QualificationMatch;
import at.beerbrawl.backend.entity.Tournament;
import java.util.List;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface TournamentMapper {
    TournamentListDto tournamentToTournamentListDto(Tournament tournament);

    List<TournamentListDto> tournamentToTournamentListDto(List<Tournament> tournament);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organizer", ignore = true)
    Tournament createDtoToEntity(CreateTournamentDto tournamentDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organizer", ignore = true)
    Tournament updateDtoToEntity(TournamentUpdateDto tournamentDto);

    TournamentDto entityToDto(Tournament entity);

    default TournamentQualificationMatchDto qualificationMatchEntityToDto(
        QualificationMatch entity
    ) {
        return new TournamentQualificationMatchDto(
            entity.getId(),
            entity.getStartTime(),
            entity.getEndTime(),
            entity.getWinnerPoints(),
            entity
                .getParticipations()
                .stream()
                .map(
                    p ->
                        new TournamentQualificationMatchParticipantDto(
                            p.getTeam().getId(),
                            p.getTeam().getName(),
                            p.isDrinksCollected(),
                            Objects.equals(
                                p.getTeam().getId(),
                                entity.getWinner() != null ? entity.getWinner().getId() : null
                            ),
                            p.getTeam().getCheckedIn()
                        )
                )
                .toList(),
            entity.getTable() == null ? null : BeerPongTableDto.fromTable(entity.getTable())
        );
    }
}
