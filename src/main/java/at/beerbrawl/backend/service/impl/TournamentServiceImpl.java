/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.service.impl;

import at.beerbrawl.backend.endpoint.dto.TournamentUpdateDto;
import at.beerbrawl.backend.entity.KoStanding;
import at.beerbrawl.backend.entity.Team;
import at.beerbrawl.backend.entity.Tournament;
import at.beerbrawl.backend.entity.domainservice.MatchDomainService;
import at.beerbrawl.backend.exception.BadTournamentPublicAccessTokenException;
import at.beerbrawl.backend.exception.NotFoundException;
import at.beerbrawl.backend.repository.BeerPongTableRepository;
import at.beerbrawl.backend.repository.KoStandingsRepository;
import at.beerbrawl.backend.repository.QualificationMatchRepository;
import at.beerbrawl.backend.repository.TeamRepository;
import at.beerbrawl.backend.repository.TournamentRepository;
import at.beerbrawl.backend.repository.UserRepository;
import at.beerbrawl.backend.service.TournamentService;
import at.beerbrawl.backend.service.models.TournamentOverviewModel;
import jakarta.validation.ValidationException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class TournamentServiceImpl implements TournamentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        MethodHandles.lookup().lookupClass()
    );
    private final TournamentRepository tournamentRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final QualificationMatchRepository qualificationRepository;
    private final BeerPongTableRepository beerPongTableRepository;
    private final KoStandingsRepository koStandingsRepository;
    private final MatchDomainService matchDomainService;

    @Override
    public List<Tournament> findAllByOrganizer(String organizerName) {
        LOGGER.debug("Find all tournaments of Organizer");
        var organizerId = userRepository.findByUsername(organizerName).getId();
        return tournamentRepository.findAllByOrganizerIdOrderByNameAsc(organizerId);
    }

    @Override
    public Tournament create(Tournament tournament, String currentUserName) {
        LOGGER.debug("Create new tournament {}", tournament);

        tournament.setOrganizer(userRepository.findByUsername(currentUserName));

        return tournamentRepository.saveAndFlush(tournament);
    }

    public Tournament findOne(long tournamentId) {
        LOGGER.debug("Get basic information about tournament {}", tournamentId);

        return tournamentRepository
            .findById(tournamentId)
            .orElseThrow(() -> new IllegalArgumentException("Tournament not found"));
    }

    @Override
    public boolean isOrganizer(String username, Long tournamentId) {
        LOGGER.debug(
            "Check if user {} has permission to access tournament with id {}",
            username,
            tournamentId
        );
        var tournament = tournamentRepository
            .findById(tournamentId)
            .orElseThrow(NotFoundException::new);
        return tournament.getOrganizer().getUsername().equals(username);
    }

    @Transactional
    public void deleteTournament(long tournamentId, String currentUserName)
        throws NotFoundException, AccessDeniedException {
        LOGGER.debug("Deleting tournament with id {}", tournamentId);

        Tournament tournament = tournamentRepository
            .findById(tournamentId)
            .orElseThrow(() -> new NotFoundException("Tournament not found"));

        if (!tournament.getOrganizer().getUsername().equals(currentUserName)) {
            throw new AccessDeniedException("You do not have permission to delete this tournament");
        }

        tournamentRepository.deleteById(tournamentId);
        LOGGER.debug("Tournament with id {} deleted successfully", tournamentId);
    }

    public Tournament updateTournament(long tournamentId, TournamentUpdateDto updates)
        throws NotFoundException, ValidationException {
        LOGGER.debug("Update tournament with id {} to {}", tournamentId, updates);

        var tournament = tournamentRepository
            .findById(tournamentId)
            .orElseThrow(() -> new NotFoundException("Tournament not found"));

        if (updates.name() != null && !updates.name().isBlank()) {
            tournament.setName(updates.name());
        }

        if (updates.description() != null && !updates.description().isBlank()) {
            tournament.setDescription(updates.description());
        }

        // All Team related information can only be set before the tournament has started.
        var qualiMatches = qualificationRepository.findAllByTournamentId(tournamentId);
        if (qualiMatches.isEmpty()) {
            LOGGER.debug("Update tournament ok before its started");

            if (updates.maxParticipants() != null) {
                if (
                    teamRepository.findAllByTournamentId(tournamentId).size() >
                    updates.maxParticipants()
                ) {
                    throw new ValidationException(
                        "New max participating teams is lower than already registered teams."
                    );
                }
                tournament.setMaxParticipants(updates.maxParticipants());
            }

            if (updates.registrationEnd() != null) {
                tournament.setRegistrationEnd(updates.registrationEnd());
            }
        } else if (updates.maxParticipants() != null) {
            throw new ValidationException(
                "maxParticipants cannot be updated for running tournament"
            );
        } else if (updates.registrationEnd() != null) {
            throw new ValidationException(
                "registrationEnd cannot be updated for running tournament"
            );
        }

        return tournamentRepository.saveAndFlush(tournament);
    }

    @Override
    public TournamentOverviewModel getTournamentOverview(long tournamentId)
        throws NotFoundException {
        LOGGER.debug("Obtain data for tournament {} overview", tournamentId);

        var tournament = tournamentRepository.findById(tournamentId);
        if (tournament.isEmpty()) {
            throw new NotFoundException("Tournament was not found.");
        }
        var modelBuilder = TournamentOverviewModel.builder();

        modelBuilder
            .description(tournament.get().getDescription())
            .name(tournament.get().getName())
            .registrationEnd(tournament.get().getRegistrationEnd())
            .maxParticipants(tournament.get().getMaxParticipants())
            .publicAccessToken(tournament.get().getPublicAccessToken());

        var qualificationMatches = qualificationRepository.findAllByTournamentId(tournamentId);
        if (!qualificationMatches.isEmpty()) {
            modelBuilder
                .allQualificationMatches(qualificationMatches.size())
                .playedQualificationMatches(
                    Math.toIntExact(
                        qualificationMatches.stream().filter(q -> q.getEndTime() != null).count()
                    )
                );
        }
        var koMatchNodes = koStandingsRepository.findAllByTournamentId(tournamentId);
        if (!koMatchNodes.isEmpty()) {
            var koMatches = koMatchNodes
                .stream()
                .filter(KoStanding::hasPrecedingMatches)
                .toList()
                .size();
            modelBuilder
                .allKoMatches(koMatches)
                .playedKoMatches(
                    Math.toIntExact(
                        koMatchNodes.stream().filter(q -> q.getEndTime() != null).count()
                    )
                );
        }

        var tables = beerPongTableRepository.findAllByTournamentId(tournamentId);
        if (!tables.isEmpty()) {
            modelBuilder.tables(tables.size());
            modelBuilder.tablesInUse(
                (int) tables.stream().filter(t -> t.getCurrentMatch() != null).count()
            );
        }

        var teams = teamRepository.findAllByTournamentId(tournamentId);
        if (!teams.isEmpty()) {
            modelBuilder
                .teams(teams.size())
                .checkedInTeams(Math.toIntExact(teams.stream().filter(Team::getCheckedIn).count()));
        }

        return modelBuilder.build();
    }

    @Override
    public long countStartedTournaments(String username) {
        var organizerId = userRepository.findByUsername(username).getId();
        List<Tournament> tournaments =
            tournamentRepository.findAllWithQualificationMatchesByOrganizerIdOrderByNameAsc(
                organizerId
            );
        return tournaments.stream().filter(t -> !t.getQualificationMatches().isEmpty()).count();
    }

    @Override
    public long countNotStartedTournaments(String username) {
        var organizerId = userRepository.findByUsername(username).getId();
        List<Tournament> tournaments =
            tournamentRepository.findAllWithQualificationMatchesByOrganizerIdOrderByNameAsc(
                organizerId
            );
        return tournaments.stream().filter(t -> t.getQualificationMatches().isEmpty()).count();
    }

    @Override
    public void assertAccessTokenIsCorrect(Long tournamentId, UUID uuid) {
        var tournament = tournamentRepository
            .findById(tournamentId)
            .orElseThrow(() -> new NotFoundException("Tournament not found"));

        if (!Objects.equals(tournament.getPublicAccessToken(), uuid)) {
            throw new BadTournamentPublicAccessTokenException();
        }
    }
}
