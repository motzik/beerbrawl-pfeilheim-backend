/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.service.impl;

import at.beerbrawl.backend.endpoint.dto.TournamentUpdateQualificationMatchDto;
import at.beerbrawl.backend.entity.Participation;
import at.beerbrawl.backend.entity.QualificationMatch;
import at.beerbrawl.backend.entity.Team;
import at.beerbrawl.backend.entity.domainservice.MatchDomainService;
import at.beerbrawl.backend.exception.NotFoundException;
import at.beerbrawl.backend.exception.PreconditionFailedException;
import at.beerbrawl.backend.exception.TeamMatchDrinksAlreadyPickedUpException;
import at.beerbrawl.backend.repository.BeerPongTableRepository;
import at.beerbrawl.backend.repository.QualificationMatchRepository;
import at.beerbrawl.backend.repository.QualificationParticipationRepository;
import at.beerbrawl.backend.repository.TeamRepository;
import at.beerbrawl.backend.repository.TournamentRepository;
import at.beerbrawl.backend.service.TournamentQualificationService;
import at.beerbrawl.backend.service.TournamentTeamService;
import at.beerbrawl.backend.service.models.QualificationTeamScoreModel;
import at.beerbrawl.backend.util.BeerDateTime;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class TournamentQualificationServiceImpl implements TournamentQualificationService {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final TournamentRepository tournamentRepository;
    private final TeamRepository teamRepository;
    private final QualificationMatchRepository qualificationRepository;
    private final QualificationParticipationRepository qualificationParticipationRepository;
    private final MatchDomainService matchDomainService;
    private final BeerPongTableRepository beerPongTableRepository;
    private final TournamentTeamService teamService;

    @Override
    /*
     * @Transactional is necessary, as the qualificationMatches are not persisted in one go.
     * By default, a transaction is opened for each repository.saveAndFlush().
     */
    @Transactional
    public List<QualificationMatch> generateQualificationMatchesForTournament(
        Long tournamentId,
        String currentUserName
    ) throws PreconditionFailedException, AccessDeniedException, NotFoundException {
        LOG.debug("Create qualifying matches for tournament with id {}", tournamentId);

        var tournamentOrganizer = tournamentRepository
            .findById(tournamentId)
            .orElseThrow(NotFoundException::new)
            .getOrganizer();

        if (
            tournamentOrganizer == null ||
            !tournamentOrganizer.getUsername().equals(currentUserName)
        ) {
            LOG.debug(
                "Couldn't create qualifying matches for tournament with id {}, because the user who started the process isn't the same as the creator of the tournament.",
                tournamentId
            );
            throw new AccessDeniedException("Current user isn't organizer of tournament.");
        }

        var teams = teamRepository.findAllByTournamentId(tournamentId);
        if (teams.size() < 16) {
            LOG.debug(
                "Couldn't create qualifying matches for tournament with id {}, because there were less than 16 teams assigned to the tournament.",
                tournamentId
            );
            throw new PreconditionFailedException("Not enough teams in specified tournament.");
        }

        if (
            teams
                .stream()
                .anyMatch(t -> qualificationParticipationRepository.existsByTeamId(t.getId()))
        ) {
            LOG.debug(
                "Couldn't create qualifying matches for tournament with id {}, because there were already qualification matches assigned.",
                tournamentId
            );
            throw new PreconditionFailedException(
                "Qualification matches already created for tournament."
            );
        }

        Collections.shuffle(teams);
        var tournament = tournamentRepository.getReferenceById(tournamentId);

        var matches = new ArrayList<QualificationMatch>();
        for (int i = 0; i < teams.size(); i++) {
            final var participatingTeams = List.of(
                teams.get(i),
                teams.get(Math.floorMod(i - 1, teams.size()))
            );
            final var match = new QualificationMatch(tournament, participatingTeams);
            matches.add(match);
        }

        qualificationRepository.saveAllAndFlush(matches);

        matchDomainService.scheduleQualiMatches(tournamentId);

        return qualificationRepository.getAllByIdIn(matches.stream().map(m -> m.getId()).toList());
    }

    @Override
    public List<QualificationMatch> getQualificationMatchesForTournament(Long tournamentId) {
        LOG.debug("Get qualification matches for tournament with id {}", tournamentId);

        return qualificationRepository.findAllByTournamentId(tournamentId);
    }

    /**
     * Update a qualification match with the given data.
     * Has to be transactional, as updating the result of a match must clear the
     * match from the corresponding table.
     */
    @Override
    @Transactional
    public QualificationMatch updateQualificationMatch(
        Long tournamentId,
        Long matchId,
        TournamentUpdateQualificationMatchDto updateDto
    ) {
        LOG.debug(
            "Updating qualification match {} in tournament {} with {}",
            matchId,
            tournamentId,
            updateDto
        );

        var match =
            this.qualificationRepository.findById(matchId).orElseThrow(
                    () -> new NotFoundException("Match not found")
                );

        if (!Objects.equals(match.getTournament().getId(), tournamentId)) {
            throw new NotFoundException("Match not found");
        }

        if (!match.getParticipations().stream().allMatch(p -> p.getTeam().getCheckedIn())) {
            throw new PreconditionFailedException("Not all teams in this match are ready yet");
        }

        if (updateDto.drinksPickup() != null) {
            this.updateQualificationMatchDrinksStatus(match, updateDto.drinksPickup());
        }

        if (updateDto.scoreUpdate() != null) {
            this.updateQualificationMatchResults(match, updateDto.scoreUpdate());
        }

        // Ensure we return an up-to-date object in any case
        return this.qualificationRepository.findById(matchId).orElseThrow(
                () -> new NotFoundException("Match not found")
            );
    }

    protected void updateQualificationMatchDrinksStatus(
        QualificationMatch match,
        TournamentUpdateQualificationMatchDto.DrinksPickupDto updateDto
    ) {
        final var participation = match
            .getParticipations()
            .stream()
            .filter(p -> p.getTeam().getId() == updateDto.teamId())
            .findFirst()
            .orElseThrow(
                () ->
                    new PreconditionFailedException(
                        "Team %d is not a participant of qualification match %d".formatted(
                                updateDto.teamId(),
                                match.getId()
                            )
                    )
            );

        if (participation.isDrinksCollected()) {
            throw new TeamMatchDrinksAlreadyPickedUpException(
                match.getId(),
                participation.getTeam().getId()
            );
        }

        if (this.teamService.isTeamCurrentlyPlaying(updateDto.teamId())) {
            throw new PreconditionFailedException(
                "Cannot mark drinks for team %d as picked up, currently playing in another match".formatted(
                        updateDto.teamId()
                    )
            );
        }

        if (match.getStartTime() != null) {
            throw new PreconditionFailedException("Qualification match has already started");
        }

        if (match.getEndTime() != null) {
            throw new PreconditionFailedException("Qualification match has already ended");
        }

        participation.setDrinksCollected(true);
        this.qualificationParticipationRepository.saveAndFlush(participation);

        final var participantIds = match
            .getParticipations()
            .stream()
            .map(p -> p.getTeam().getId())
            .toList();
        if (participantIds.stream().anyMatch(this.teamService::isTeamCurrentlyPlaying)) {
            LOG.debug(
                "Cannot start qualification match {}; one or both participating teams ({}) are currently playing",
                match.getId(),
                participantIds
            );
            return;
        }

        if (!match.getParticipations().stream().allMatch(Participation::isDrinksCollected)) {
            LOG.debug(
                "Cannot start qualification match {}; one or both participating teams ({}) have not collected their drinks yet",
                match.getId(),
                participantIds
            );
            return;
        }

        // TODO: Check also whether a table is already assigned for this match
        // and only set `startTime` it if it has. See #40.

        LOG.debug("Starting qualification match {}", match.getId());
        match.setStartTime(BeerDateTime.nowUtc());
        this.qualificationRepository.saveAndFlush(match);
    }

    private void updateQualificationMatchResults(
        QualificationMatch match,
        TournamentUpdateQualificationMatchDto.ScoreUpdateDto updateDto
    ) {
        if (match.getStartTime() == null || match.getStartTime().isAfter(BeerDateTime.nowUtc())) {
            throw new PreconditionFailedException("Match has not started yet");
        }

        // Check if both teams are ready
        if (
            match.getTeams().size() != 2 || match.getTeams().stream().noneMatch(Team::getCheckedIn)
        ) {
            throw new PreconditionFailedException(
                "Both teams must be ready to update match results"
            );
        }

        if (updateDto.winnerTeamId() != null) {
            final var winnerTeam = match
                .getTeams()
                .stream()
                .filter(team -> team.getId().longValue() == updateDto.winnerTeamId().longValue())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Winner team not found"));

            match.setWinner(winnerTeam);
            match.setWinnerPoints(updateDto.winnerPoints());
        }

        match.setEndTime(BeerDateTime.nowUtc());
        this.qualificationRepository.saveAndFlush(match);
        for (var team : match.getTeams()) {
            team.markAvailable();
            teamRepository.saveAndFlush(team);
        }
        if (match.getTable() != null) {
            match.getTable().setCurrentMatch(null);
            beerPongTableRepository.saveAndFlush(match.getTable());
            final var tournamentId = match.getTournament().getId();
            matchDomainService.scheduleQualiMatches(tournamentId);
        } else {
            LOG.warn(
                """
                Finishing a match that is not bound to a table should never happen.
                Acceptable for old tests that did not consider the table scheduling logic
                or rely on repositories directly for data manipulation
                                """
            );
        }
    }

    @Override
    @Transactional
    public List<QualificationTeamScoreModel> getTournamentQualificationScoreTable(
        Long tournamentId
    ) {
        LOG.debug("Get qualification score table for tournament with id {}", tournamentId);

        // check that the tournament exists
        tournamentRepository
            .findById(tournamentId)
            .orElseThrow(() -> new NotFoundException("Tournament not found"));

        var teams = teamRepository.findAllByTournamentId(tournamentId);
        var matches = qualificationRepository.findAllByTournamentId(tournamentId);

        return calculateScores(teams, matches);
    }

    private static List<QualificationTeamScoreModel> calculateScores(
        List<Team> teams,
        List<QualificationMatch> matches
    ) {
        Map<Long, QualificationTeamScoreModel> scoreEntryMap = new java.util.HashMap<>(Map.of());
        for (var team : teams) {
            var current = QualificationTeamScoreModel.empty(team);
            scoreEntryMap.put(team.getId(), current);
        }

        var playedMatches = matches.stream().filter(m -> m.getWinner() != null).toList();

        for (var match : playedMatches) {
            var winner = match.getWinner();
            var loser = match
                .getParticipations()
                .stream()
                .filter(p -> !p.getTeam().equals(winner))
                .findFirst()
                .get();
            var winnerScore = match.getWinnerPoints();

            scoreEntryMap.get(winner.getId()).addWin(winnerScore);
            scoreEntryMap.get(loser.getTeam().getId()).addLoss();
        }

        var scoreEntries = new ArrayList<>(scoreEntryMap.values().stream().toList());

        // sort by wins, then points, and then just by their name
        scoreEntries.sort((a, b) -> {
            if (a.getWins().equals(b.getWins())) {
                if (a.getPoints().equals(b.getPoints())) {
                    return a.getName().compareTo(b.getName());
                }
                return Long.compare(b.getPoints(), a.getPoints());
            }
            return Long.compare(b.getWins(), a.getWins());
        });

        Long currentPosition = 1L;
        QualificationTeamScoreModel lastEntry = null;
        for (var entry : scoreEntries) {
            if (
                lastEntry != null &&
                lastEntry.getWins().equals(entry.getWins()) &&
                lastEntry.getPoints().equals(entry.getPoints())
            ) {
                entry.setPosition(lastEntry.getPosition());
            } else {
                entry.setPosition(currentPosition);
            }
            currentPosition++;
            lastEntry = entry;
        }

        return scoreEntries;
    }
}
