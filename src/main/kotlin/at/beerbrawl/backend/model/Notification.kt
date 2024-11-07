/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.model

/**
 * @property message
 * @property tournamentId
 */
data class Notification(val message: String, val tournamentId: Long)
