/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.service.impl;

import at.beerbrawl.backend.endpoint.dto.TournamentUpdateKoStandingDto;
import at.beerbrawl.backend.entity.KoStanding;
import at.beerbrawl.backend.entity.Team;
import at.beerbrawl.backend.entity.Tournament;
import at.beerbrawl.backend.entity.domainservice.MatchDomainService;
import at.beerbrawl.backend.exception.NotFoundException;
import at.beerbrawl.backend.exception.PreconditionFailedException;
import at.beerbrawl.backend.exception.TeamMatchDrinksAlreadyPickedUpException;
import at.beerbrawl.backend.repository.BeerPongTableRepository;
import at.beerbrawl.backend.repository.KoStandingsRepository;
import at.beerbrawl.backend.repository.QualificationMatchRepository;
import at.beerbrawl.backend.repository.TeamRepository;
import at.beerbrawl.backend.repository.TournamentRepository;
import at.beerbrawl.backend.service.TournamentKoPhaseService;
import at.beerbrawl.backend.service.TournamentQualificationService;
import at.beerbrawl.backend.service.TournamentTeamService;
import at.beerbrawl.backend.service.models.QualificationTeamScoreModel;
import at.beerbrawl.backend.util.BeerDateTime;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class TournamentKoPhaseServiceImpl implements TournamentKoPhaseService {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final TournamentRepository tournamentRepository;
    private final TeamRepository teamRepository;
    private final QualificationMatchRepository qualificationRepository;
    private final KoStandingsRepository koStandingsRepository;
    private final TournamentTeamService teamService;
    private final TournamentQualificationService qualificationService;
    private final MatchDomainService matchDomainService;
    private final BeerPongTableRepository beerPongTableRepository;

    @Override
    public KoStanding getStandingById(long standingId) {
        return this.koStandingsRepository.findById(standingId).orElseThrow(
                () -> new NotFoundException("KO standing %d not found".formatted(standingId))
            );
    }

    @Override
    @Transactional
    public void generateKoMatchesForTournament(
        Long tournamentId,
        List<Long> teamIds,
        String subjectName
    ) throws NotFoundException, AccessDeniedException {
        LOG.debug("Create knockout matches for tournament with id {}", tournamentId);

        // authorization
        final Tournament tournament = tournamentRepository.getReferenceById(tournamentId);
        if (!tournament.getOrganizer().getUsername().equals(subjectName)) {
            LOG.debug(
                "Subject {} illegally tried to generate KO phase of non-owned tournament {}",
                subjectName,
                tournamentId
            );
            throw new AccessDeniedException("Current user isn't organizer of tournament.");
        }

        // ensure quali phase finished
        final var matches = qualificationRepository.findAllByTournamentId(tournamentId);
        boolean qualiIncomplete = matches.stream().anyMatch(m -> m.getWinner() == null);
        if (qualiIncomplete) {
            LOG.debug(
                "Couldn't create knockout matches for tournament with id {}, because there were still qualification matches running.",
                tournamentId
            );
            throw new PreconditionFailedException("Qualification matches still running.");
        }

        // delete previous KO matches present
        koStandingsRepository.deleteByTournament(tournament);

        if (teamIds.size() != 16L) {
            throw new PreconditionFailedException("16 teams are required for knockout phase");
        }

        var scoreTable = qualificationService.getTournamentQualificationScoreTable(tournamentId);
        var edgePosition = scoreTable.get(15).getPosition();

        var guaranteedTeams = scoreTable
            .stream()
            .filter(s -> s.getPosition() < edgePosition)
            .map(QualificationTeamScoreModel::getId)
            .toList();

        var disqualifiedTeams = scoreTable
            .stream()
            .filter(s -> s.getPosition() > edgePosition)
            .map(QualificationTeamScoreModel::getId)
            .toList();

        //check qualified teams
        for (int i = 0; i < guaranteedTeams.size(); i++) {
            if (!Objects.equals(teamIds.get(i), guaranteedTeams.get(i))) {
                throw new PreconditionFailedException(
                    "The qualified teams are not in the correct order"
                );
            }
        }

        //check disqualified teams
        for (var disqualifiedTeam : disqualifiedTeams) {
            if (teamIds.contains(disqualifiedTeam)) {
                throw new PreconditionFailedException(
                    "There are already disqualified teams in selection for the the knockout phase."
                );
            }
        }

        var placesLeft = 16 - guaranteedTeams.size();
        var pickedEdgeTeams = teamIds
            .stream()
            .filter(
                teamId -> !guaranteedTeams.contains(teamId) && !disqualifiedTeams.contains(teamId)
            )
            .toList();

        if (pickedEdgeTeams.size() != placesLeft) {
            throw new PreconditionFailedException(
                "Not the correct amount of teams picked for knockout phase. Expected 16 teams."
            );
        }

        var edgeTeams = scoreTable
            .stream()
            .filter(s -> Objects.equals(s.getPosition(), edgePosition))
            .map(QualificationTeamScoreModel::getId)
            .toList();

        for (var pickedTeamId : pickedEdgeTeams) {
            if (!edgeTeams.contains(pickedTeamId)) {
                throw new PreconditionFailedException(
                    "There is a team picked for knockout phase that is not on the edge for qualification."
                );
            }
        }

        var teams = new HashMap<Long, Team>();
        for (var team : teamRepository.findAllById(teamIds)) {
            teams.put(team.getId(), team);
        }

        var koMatches = teamIds
            .stream()
            .map(teamId -> new KoStanding(tournament, null, teams.get(teamId)))
            .toArray(KoStanding[]::new);

        // map the teams to the leaf layer of the KO tree
        // undefined ranking logic, so i just cross-match the teams like in wendy's
        // tournaments
        var leafLayer = new KoStanding[teamIds.size()];
        for (int i = 0; i < leafLayer.length / 2; i++) {
            leafLayer[i * 2] = koMatches[i];
            leafLayer[i * 2 + 1] = koMatches[koMatches.length - 1 - i];
        }

        // for each r in Rounds, n in N: (r)[2n] and (r)[2n+1] fight for the spot of
        // next (r+1)[n]
        var layer = leafLayer.clone();
        while (layer.length > 1) {
            final var prevLayer = layer;
            layer = new KoStanding[prevLayer.length / 2];

            for (int i = 0; i < layer.length; i++) {
                final var preceeding = List.of(prevLayer[i * 2], prevLayer[i * 2 + 1]);
                layer[i] = new KoStanding(tournament, preceeding, null);
                // passing the preceeding standings to the parent does not help us, as the child
                // owns the relationship
                for (var p : preceeding) {
                    p.setNextStanding(layer[i]);
                }
            }
        }

        var validifyResult = layer[0].evaluateValidity();
        if (validifyResult != KoStanding.KoStandingValidationResult.OK) {
            LOG.error("Generated KO tree is invalid {}", validifyResult);
            throw new IllegalStateException("Generated KO tree is invalid");
        }

        // requires cascade persist on the many side
        koStandingsRepository.saveAndFlush(layer[0]);

        matchDomainService.scheduleKoMatches(tournamentId);
    }

    @Override
    public KoStanding getKoStandingsTree(Long tournamentId) {
        LOG.debug("Get ko standings for tournament {}.", tournamentId);

        var standings = koStandingsRepository.getAllByTournamentId(tournamentId);
        if (standings.isEmpty()) {
            LOG.debug("No ko standings for tournament {} found.", tournamentId);
            throw new NotFoundException("No standings for tournament found");
        }

        var rootNode = standings
            .stream()
            .filter(n -> n.getNextStanding() == null)
            .findFirst()
            .orElseThrow(
                () -> new PreconditionFailedException("No root node for ko matches tree found.")
            );

        var stack = new Stack<KoStanding>();
        stack.addAll(rootNode.getPreceedingStandings());
        while (!stack.isEmpty()) {
            var current = stack.pop();

            var newPrecedingStandings = standings
                .stream()
                .filter(
                    n ->
                        n.getNextStanding() != null &&
                        Objects.equals(n.getNextStanding().getId(), current.getId())
                )
                .toList();

            current.getPreceedingStandings().clear();
            current.getPreceedingStandings().addAll(newPrecedingStandings);

            stack.addAll(newPrecedingStandings);
        }

        return rootNode;
    }

    @Override
    @Transactional
    public void updateKoStanding(
        String userName,
        Long tournamentId,
        Long standingId,
        TournamentUpdateKoStandingDto updateDto
    ) {
        var tournament = tournamentRepository
            .findById(tournamentId)
            .orElseThrow(() -> new NotFoundException("No tournament found."));

        if (!tournament.getOrganizer().getUsername().equals(userName)) {
            throw new AccessDeniedException("Current user isn't organizer of tournament.");
        }

        var koStanding = koStandingsRepository
            .findKoStandingById(standingId)
            .orElseThrow(() -> new NotFoundException("KO standing not found"));

        if (updateDto.teamSet() != null) {
            this.updateKoStandingTeam(koStanding, updateDto.teamSet());
        }

        if (updateDto.drinksPickup() != null) {
            this.updateKoStandingDrinksStatus(koStanding, updateDto.drinksPickup());
        }
    }

    private void updateKoStandingTeam(
        KoStanding koStanding,
        TournamentUpdateKoStandingDto.SetWinnerTeamDto teamSet
    ) {
        if (!koStanding.hasPrecedingMatches()) {
            throw new PreconditionFailedException("Team of first round can't be changed.");
        }

        if (
            koStanding.getNextStanding() != null && koStanding.getNextStanding().getTeam() != null
        ) {
            throw new PreconditionFailedException("Team of next standing isn't empty");
        }

        if (teamSet.teamId() == null) {
            koStanding.setTeam(null);
        } else {
            var team = this.teamService.getById(teamSet.teamId());
            if (
                koStanding
                    .getPreceedingStandings()
                    .stream()
                    .filter(s -> s.getTeam() != null && s.getTeam().getId().equals(team.getId()))
                    .count() !=
                1
            ) {
                throw new PreconditionFailedException("Team isn't assigned to a previous standing");
            }

            koStanding.setTeam(team);
            koStanding.setEndTime(BeerDateTime.nowUtc());
        }

        this.koStandingsRepository.saveAndFlush(koStanding);
        if (koStanding.getTable() != null) {
            koStanding.getTable().setCurrentMatch(null);
            beerPongTableRepository.saveAndFlush(koStanding.getTable());
            matchDomainService.scheduleKoMatches(koStanding.getTournament().getId());
        }
    }

    private void updateKoStandingDrinksStatus(
        KoStanding standing,
        TournamentUpdateKoStandingDto.DrinksPickupDto updateDto
    ) {
        if (standing.getTeam() == null || standing.getTeam().getId() != updateDto.teamId()) {
            throw new PreconditionFailedException(
                "Team %d is not a participant of KO match %d".formatted(
                        updateDto.teamId(),
                        standing.getId()
                    )
            );
        }

        if (standing.isDrinksCollected()) {
            throw new TeamMatchDrinksAlreadyPickedUpException(standing.getId(), updateDto.teamId());
        }

        if (this.teamService.isTeamCurrentlyPlaying(updateDto.teamId())) {
            throw new PreconditionFailedException(
                "Cannot mark drinks for team %d as picked up, currently playing in another match".formatted(
                        updateDto.teamId()
                    )
            );
        }

        final var next = standing.getNextStanding();
        if (next != null) {
            if (next.getStartTime() != null) {
                throw new PreconditionFailedException("KO match has already started");
            }

            if (next.getEndTime() != null) {
                throw new PreconditionFailedException("KO match has already ended");
            }

            if (!next.getPreceedingStandings().stream().allMatch(s -> s.getTeam() != null)) {
                throw new PreconditionFailedException("Both teams for the next match must be set");
            }
        }

        standing.setDrinksCollected(true);
        this.koStandingsRepository.saveAndFlush(standing);

        tryStartKoPhaseMatch(standing.getNextStanding());
    }

    private void tryStartKoPhaseMatch(KoStanding standing) {
        final var participantIds = standing
            .getPreceedingStandings()
            .stream()
            .map(p -> p.getTeam() != null ? p.getTeam().getId() : null)
            .filter(Objects::nonNull)
            .toList();

        if (participantIds.size() != 2) {
            LOG.debug(
                "Cannot start KO match {}; not all participants ({} of 2) have been set",
                standing.getId(),
                participantIds.size()
            );
            return;
        }

        if (participantIds.stream().anyMatch(this.teamService::isTeamCurrentlyPlaying)) {
            LOG.debug(
                "Cannot start KO match {}; one or both participating teams ({}) are currently playing",
                standing.getId(),
                participantIds
            );
            return;
        }

        if (!standing.getPreceedingStandings().stream().allMatch(KoStanding::isDrinksCollected)) {
            LOG.debug(
                "Cannot start KO match {}; one or both participating teams ({}) have not collected their drinks yet",
                standing.getId(),
                participantIds
            );
            return;
        }

        // TODO: Check also whether a table is already assigned for this match
        // and only set `startTime` it if it has. See #40.

        LOG.debug("Starting KO match {}", standing.getId());
        standing.setStartTime(BeerDateTime.nowUtc());
        this.koStandingsRepository.saveAndFlush(standing);
    }
}
