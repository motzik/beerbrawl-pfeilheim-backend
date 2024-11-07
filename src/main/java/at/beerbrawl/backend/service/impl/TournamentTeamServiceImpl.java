/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.service.impl;

import at.beerbrawl.backend.endpoint.dto.TournamentUpdateTeamDto;
import at.beerbrawl.backend.endpoint.mapper.TeamMapper;
import at.beerbrawl.backend.entity.Team;
import at.beerbrawl.backend.entity.Tournament.SignupTeamResult;
import at.beerbrawl.backend.entity.domainservice.MatchDomainService;
import at.beerbrawl.backend.exception.BadTournamentPublicAccessTokenException;
import at.beerbrawl.backend.exception.NotFoundException;
import at.beerbrawl.backend.exception.TournamentAlreadyStartedException;
import at.beerbrawl.backend.repository.KoStandingsRepository;
import at.beerbrawl.backend.repository.QualificationMatchRepository;
import at.beerbrawl.backend.repository.TeamRepository;
import at.beerbrawl.backend.repository.TournamentRepository;
import at.beerbrawl.backend.service.TournamentTeamService;
import at.beerbrawl.backend.service.models.TeamModel;
import at.beerbrawl.backend.util.BeerDateTime;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class TournamentTeamServiceImpl implements TournamentTeamService {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final TournamentRepository tournamentRepository;
    private final TeamRepository teamRepository;
    private final MatchDomainService matchDomainService;
    private final QualificationMatchRepository qualificationRepository;
    private final KoStandingsRepository koStandingsRepository;
    private final TeamMapper teamMapper;

    @Override
    public Team getById(long teamId) {
        return this.teamRepository.findById(teamId).orElseThrow(
                () -> new NotFoundException("Team %d not found".formatted(teamId))
            );
    }

    /**
     * Isolation must prevent phantom read for maxParticipants check.
     */
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public SignupTeamResult signupTeamForTournament(
        long tournamentId,
        UUID selfRegistrationToken,
        String name
    ) {
        LOG.debug("Create new team {} for tournament {}", name, tournamentId);

        final var tournament = tournamentRepository
            .findById(tournamentId)
            .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));

        if (!Objects.equals(tournament.getPublicAccessToken(), selfRegistrationToken)) {
            throw new BadTournamentPublicAccessTokenException();
        }

        if (
            tournament.getRegistrationEnd().isBefore(BeerDateTime.nowUtc()) ||
            !tournament.getQualificationMatches().isEmpty()
        ) {
            return SignupTeamResult.REGISTRATION_CLOSED;
        }

        final var teams = teamRepository.findAllByTournamentId(tournamentId);
        if (teams.size() >= tournament.getMaxParticipants()) {
            return SignupTeamResult.MAX_PARTICIPANTS_REACHED;
        }

        if (teams.stream().map(Team::getName).anyMatch(name::equals)) {
            return SignupTeamResult.TEAM_ALREADY_EXISTS;
        }

        teamRepository.save(new Team(name, tournament));
        return SignupTeamResult.SUCCESS;
    }

    @Override
    @Transactional
    public Collection<TeamModel> getTournamentTeams(Long tournamentId) {
        LOG.debug("Get teams for tournament with id {}", tournamentId);

        return teamRepository
            .findAllByTournamentId(tournamentId)
            .stream()
            .map(
                team ->
                    this.teamMapper.entityToModel(team, this.isTeamCurrentlyPlaying(team.getId()))
            )
            .toList();
    }

    @Override
    @Transactional
    public Team updateTeam(Long tournamentId, Long teamId, TournamentUpdateTeamDto updateDto)
        throws NotFoundException {
        LOG.debug("Updating team {} in tournament {} with {}", teamId, tournamentId, updateDto);

        var team = teamRepository
            .findById(teamId)
            .orElseThrow(() -> new NotFoundException("Team not found in tournament"));

        if (!team.getTournament().getId().equals(tournamentId)) {
            throw new NotFoundException("Team not found in tournament");
        }

        team.setName(updateDto.name());
        teamRepository.saveAndFlush(team);
        return team;
    }

    @Override
    @Transactional
    public void deleteTeam(Long tournamentId, Long teamId) throws NotFoundException {
        LOG.debug("Delete team with id {} from tournament with id {}", teamId, tournamentId);

        var team = teamRepository
            .findById(teamId)
            .orElseThrow(() -> new NotFoundException("Team not found in tournament"));
        var tournament = team.getTournament();

        if (!tournament.getId().equals(tournamentId)) {
            throw new NotFoundException("Team not found in tournament");
        }

        if ((long) tournament.getQualificationMatches().size() > 0) {
            throw new TournamentAlreadyStartedException();
        }

        teamRepository.delete(team);
    }

    @Override
    @Transactional
    public void markTeamAsReady(long tournamentId, long teamId) {
        LOG.debug("Mark team with id {} as ready for tournament with id {}", teamId, tournamentId);

        var team = teamRepository
            .findById(teamId)
            .orElseThrow(() -> new NotFoundException("Team not found in tournament"));
        var tournament = team.getTournament();

        if (!tournament.getId().equals(tournamentId)) {
            throw new NotFoundException("Team not found in tournament");
        }

        team.checkIn();
        teamRepository.saveAndFlush(team);

        matchDomainService.scheduleQualiMatches(tournamentId);
    }

    @Override
    public boolean isTeamCurrentlyPlaying(long teamId) {
        final var qualificationMatches =
            this.qualificationRepository.findByParticipationsTeamIdAndStartTimeIsNotNullAndEndTimeIsNull(
                    teamId
                );
        if (!qualificationMatches.isEmpty()) {
            return true;
        }

        final var koStandings =
            this.koStandingsRepository.findByPreceedingStandingsTeamIdAndStartTimeIsNotNullAndEndTimeIsNull(
                    teamId
                );
        return !koStandings.isEmpty();
    }
}
