/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class TeamMatchDrinksAlreadyPickedUpException extends RuntimeException {

    private final long matchId;
    private final long teamId;
}
