/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.repository;

import at.beerbrawl.backend.entity.KoStanding;
import at.beerbrawl.backend.entity.KoStanding_;
import at.beerbrawl.backend.entity.Tournament;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KoStandingsRepository extends JpaRepository<KoStanding, Long> {
    void deleteByTournament(Tournament tournament);

    List<KoStanding> findByTournament(Tournament tournament);

    List<KoStanding> findAllByTournamentId(long tournamentId);

    List<KoStanding> findByTeamId(long teamId);

    @EntityGraph(
        attributePaths = {
            KoStanding_.PRECEEDING_STANDINGS, KoStanding_.TEAM, KoStanding_.NEXT_STANDING,
        }
    )
    List<KoStanding> getAllByTournamentId(long tournamentId);

    Optional<KoStanding> findFinaleByTournamentIdAndNextStandingIsNull(Long tournamentId);

    List<KoStanding> findByPreceedingStandingsTeamIdAndStartTimeIsNotNullAndEndTimeIsNull(
        Long teamId
    );

    @EntityGraph(
        attributePaths = {
            KoStanding_.PRECEEDING_STANDINGS, KoStanding_.TEAM, KoStanding_.NEXT_STANDING,
        }
    )
    Optional<KoStanding> findKoStandingById(Long id);
}
