/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.util;

import at.beerbrawl.backend.entity.KoStanding;
import at.beerbrawl.backend.entity.Team;
import java.util.List;

public class KoStandingsTestUtil {

    /**
     * Builds a tree of Standings with a given depth and lower id bound. The
     * participant with the lower wins consistently.
     */
    public static KoStanding buildStandingsTree() {
        return _buildStandingsTreeInternal(4, -(1 << 4));
    }

    // build tree with leafes having ids in range [lowerIdBound; lowerIdBound +
    // 2^depth), and parents having the same id as their left child
    private static KoStanding _buildStandingsTreeInternal(int depth, long lowerIdBound) {
        // owned id range: [lowerboundId; lowerboundId + 2^depth)
        if (depth == 0) {
            var leafTeam = new Team(null, null);
            leafTeam.setId(lowerIdBound);
            return new KoStanding(null, null, leafTeam);
        }
        var left = _buildStandingsTreeInternal(depth - 1, lowerIdBound);
        var right = _buildStandingsTreeInternal(depth - 1, lowerIdBound + (1 << (depth - 1)));
        return new KoStanding(null, List.of(left, right), left.getTeam());
    }

    /**
     * Builds a tree of Standings with a given depth and lower id bound. The
     * participant with the lower wins consistently.
     */
    public static KoStanding buildTreeWithDuplicateParticipants() {
        var tree = buildStandingsTree();
        var eventuallyLeftmostLeaf = tree;
        while (eventuallyLeftmostLeaf.hasPrecedingMatches()) {
            eventuallyLeftmostLeaf = eventuallyLeftmostLeaf.getPreceedingStandings().get(0);
        }
        eventuallyLeftmostLeaf.getTeam().setId(-3L); // some other one, which -3 does not play against directly
        return tree;
    }

    public static KoStanding buildTreeWithUneligibleParticipant() {
        var tree = buildStandingsTree();
        var eventuallyLeftmostLeaf = tree;
        while (eventuallyLeftmostLeaf.hasPrecedingMatches()) {
            eventuallyLeftmostLeaf = eventuallyLeftmostLeaf.getPreceedingStandings().get(0);
        }
        eventuallyLeftmostLeaf.getTeam().setId(-3L); // some other one, which -3 does not play against directly
        return tree;
    }
}
