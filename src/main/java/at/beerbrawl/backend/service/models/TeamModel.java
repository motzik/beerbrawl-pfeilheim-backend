/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.service.models;

import at.beerbrawl.backend.entity.KoStanding;
import at.beerbrawl.backend.entity.QualificationParticipation;
import at.beerbrawl.backend.entity.Tournament;
import java.util.List;

public record TeamModel(
    long id,
    String name,
    boolean checkedIn,
    boolean currentlyPlaying,
    Tournament tournament,
    List<KoStanding> koStandings,
    List<QualificationParticipation> qualificationParticipations
) {}
