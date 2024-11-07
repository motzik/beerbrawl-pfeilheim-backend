/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.exception;

public class PreconditionFailedException extends RuntimeException {

    public PreconditionFailedException(String message) {
        super(message);
    }
}
