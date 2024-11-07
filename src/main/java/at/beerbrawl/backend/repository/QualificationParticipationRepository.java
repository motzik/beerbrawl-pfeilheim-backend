/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.repository;

import at.beerbrawl.backend.entity.ParticipationKey;
import at.beerbrawl.backend.entity.QualificationMatch;
import at.beerbrawl.backend.entity.QualificationParticipation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Do not persist entities using this repostiory, QualificationParticipation entity is owned by {@link QualificationMatch}.
 * Else, consistency cannot be guaranteed.
 */
@Repository
public interface QualificationParticipationRepository
    extends JpaRepository<QualificationParticipation, ParticipationKey> {
    Boolean existsByTeamId(Long teamId);

    List<QualificationParticipation> findAllByQualificationMatchId(Long matchId);

    List<QualificationParticipation> findByTeamId(Long teamId);

    List<QualificationParticipation> findByQualificationMatchIn(List<QualificationMatch> matches);

    Optional<QualificationParticipation> findByTeamIdAndQualificationMatchId(
        Long teamId,
        Long matchId
    );
}
