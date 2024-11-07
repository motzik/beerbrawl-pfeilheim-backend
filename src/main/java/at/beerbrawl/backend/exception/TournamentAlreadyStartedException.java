/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.exception;

public class TournamentAlreadyStartedException extends RuntimeException {

    public TournamentAlreadyStartedException() {}

    public TournamentAlreadyStartedException(String message) {
        super(message);
    }

    public TournamentAlreadyStartedException(String message, Throwable cause) {
        super(message, cause);
    }

    public TournamentAlreadyStartedException(Exception e) {
        super(e);
    }
}
