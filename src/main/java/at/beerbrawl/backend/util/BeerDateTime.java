/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.util;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public class BeerDateTime {

    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS);
    }
}
