/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.unittests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import at.beerbrawl.backend.entity.KoStanding;
import at.beerbrawl.backend.entity.KoStanding.KoStandingValidationResult;
import at.beerbrawl.backend.entity.Team;
import at.beerbrawl.backend.util.KoStandingsTestUtil;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Testing the structural validity of the KoStanding tree.
 */
public class KoStandingTest {

    @Test
    public void constuctRound0KoStandingSucceeds() {
        new KoStanding(null, null, new Team(null, null));
    }

    @Test
    public void constuctIntermediateKoStandingSucceeds() {
        var mockTeam = new Team(null, null);
        var preceedingStandings = List.of(
            new KoStanding(null, null, mockTeam),
            new KoStanding(null, null, mockTeam)
        );
        new KoStanding(null, preceedingStandings, mockTeam);
    }

    @Test
    public void constructGivenUneligibleTeamFails() {
        var preceedingStandings = List.of(
            new KoStanding(null, null, new Team(null, null)),
            new KoStanding(null, null, new Team(null, null))
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new KoStanding(null, preceedingStandings, new Team(null, null))
        );
    }

    @Test
    public void verify_givenInvalidStructure_fails() {
        var tree = KoStandingsTestUtil.buildStandingsTree();
        assertEquals(KoStandingValidationResult.OK, tree.evaluateValidity());
    }

    @Test
    public void verifyGivenDuplicateInitialParticipantsFails() {
        var tree = KoStandingsTestUtil.buildTreeWithDuplicateParticipants();
        assertEquals(KoStandingValidationResult.DUPLICATE_PARTICIPANTS, tree.evaluateValidity());
    }
}
