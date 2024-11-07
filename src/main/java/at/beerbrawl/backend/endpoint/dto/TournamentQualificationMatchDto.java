/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

public record TournamentQualificationMatchDto(
    Long id,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") // UTC format
    LocalDateTime startTime,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") // UTC format
    LocalDateTime endTime,
    Long winnerPoints,
    List<TournamentQualificationMatchParticipantDto> participants,
    BeerPongTableDto table
) {}
