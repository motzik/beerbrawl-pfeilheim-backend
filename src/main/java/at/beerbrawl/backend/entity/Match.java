/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.entity;

import at.beerbrawl.backend.exception.PreconditionFailedException;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * protected accesslevel because the no-args is used by the
 * hibernate-required constructor of subclasses.
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter(value = AccessLevel.PRIVATE)
public abstract class Match {

    @Getter
    @Id
    @GeneratedValue
    private Long id;

    @Getter
    private LocalDateTime startTime;

    @Getter
    private LocalDateTime endTime;

    @Getter
    @ManyToOne(optional = false)
    protected Tournament tournament;

    @Getter
    @OneToOne(mappedBy = BeerPongTable_.CURRENT_MATCH, fetch = FetchType.EAGER)
    private BeerPongTable table;

    protected Match(Tournament tournament) {
        this.tournament = tournament;
    }

    public void setStartTime(LocalDateTime startTime) {
        if (this.hasStarted() || this.isFinished()) {
            throw new PreconditionFailedException("Match has already started!");
        }

        this.startTime = startTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        if (!this.hasStarted()) {
            throw new PreconditionFailedException("Match has not started yet!");
        }

        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        this.endTime = endTime;
    }

    public enum MatchStatus {
        NOT_CHECKED_IN_YET,
        TEAMS_CHECKED_IN,
        QUEUED_COLLECTING_DRINKS,
        PLAYING,
        FINISHED,
    }

    public MatchStatus getStatus() {
        if (this.endTime != null) {
            return MatchStatus.FINISHED;
        } else if (this.startTime != null) {
            return MatchStatus.PLAYING;
        } else if (this.table != null) {
            return MatchStatus.QUEUED_COLLECTING_DRINKS;
        } else if (this.getTeams().stream().allMatch(Team::getCheckedIn)) {
            return MatchStatus.TEAMS_CHECKED_IN;
        } else {
            return MatchStatus.NOT_CHECKED_IN_YET;
        }
    }

    public boolean hasStarted() {
        return this.startTime != null;
    }

    public boolean isFinished() {
        return this.endTime != null;
    }

    public abstract List<Team> getTeams();
}
