/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.service.models;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentOverviewModel {

    private String name;
    private LocalDateTime registrationEnd;
    private Long maxParticipants;
    private String description;
    // Qualification specific
    private int allQualificationMatches;
    private int playedQualificationMatches;
    // KO specific
    private int allKoMatches;
    private int playedKoMatches;
    // Teams specific
    private int teams;
    private int checkedInTeams;
    // Tables specific
    private int tables;
    private int tablesInUse;
    private UUID publicAccessToken;
}
