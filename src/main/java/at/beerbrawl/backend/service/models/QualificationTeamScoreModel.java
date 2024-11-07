/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.service.models;

import at.beerbrawl.backend.entity.Team;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class QualificationTeamScoreModel {

    Long id;
    String name;
    Boolean ready;
    Long wins;
    Long losses;
    Long points;
    Long gamesPlayed;
    Long position;

    public static QualificationTeamScoreModel empty(Team team) {
        var model = new QualificationTeamScoreModel();
        model.setId(team.getId());
        model.setName(team.getName());
        model.setReady(team.getCheckedIn());
        model.setWins(0L);
        model.setLosses(0L);
        model.setPoints(0L);
        model.setGamesPlayed(0L);
        model.setPosition(-1L);
        return model;
    }

    public void addWin(Long winPoints) {
        this.gamesPlayed++;
        this.wins++;
        this.points += winPoints;
    }

    public void addLoss() {
        this.gamesPlayed++;
        this.losses++;
    }
}
