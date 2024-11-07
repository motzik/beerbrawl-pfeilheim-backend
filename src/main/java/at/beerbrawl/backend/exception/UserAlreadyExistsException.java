/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.exception;

public class UserAlreadyExistsException extends Exception {

    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
