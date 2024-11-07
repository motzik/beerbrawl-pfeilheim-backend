/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint

import at.beerbrawl.backend.model.Notification
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.stereotype.Controller
import org.springframework.web.util.HtmlUtils

/**
 * Websocket endpoint for notifications.
 */
@Controller
class NotificationEndpoint {
    /**
     * Notify all clients about a new message.
     *
     * @param message the message to send
     * @param tournamentId the tournament id
     * @return the notification
     */
    @MessageMapping("/notify")
    @SendTo("/partypics/notifications")
    fun notify(
        message: String,
        tournamentId: Long,
    ): Notification = Notification(HtmlUtils.htmlEscape(message), tournamentId)
}
