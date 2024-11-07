/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.repository;

import at.beerbrawl.backend.entity.QualificationMatch;
import at.beerbrawl.backend.entity.QualificationMatch_;
import at.beerbrawl.backend.entity.QualificationParticipation_;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QualificationMatchRepository extends JpaRepository<QualificationMatch, Long> {
    @EntityGraph(
        attributePaths = {
            QualificationMatch_.PARTICIPATIONS,
            QualificationMatch_.PARTICIPATIONS + '.' + QualificationParticipation_.TEAM,
        }
    )
    List<QualificationMatch> getAllByIdIn(Iterable<Long> ids);

    List<QualificationMatch> findAllByTournamentId(Long tournamentId);

    boolean existsByTournamentId(long tournamentId);

    List<QualificationMatch> findByParticipationsTeamIdAndStartTimeIsNotNullAndEndTimeIsNull(
        Long teamId
    );
}
