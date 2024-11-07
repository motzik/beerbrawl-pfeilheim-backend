/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.entity

import at.beerbrawl.backend.enums.MediaState
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.validation.constraints.Size

/**
 * @property id
 * @property author
 * @property title
 * @property image
 * @property tournament
 * @property state
 */
@Entity
class SharedMedia(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,
    var author:
    @Size(max = 30, message = "Author can't be more than 30 characters long.")
    String,
    var title:
    @Size(max = 50, message = "Title can't be more than 50 characters long.")
    String,
    @Lob
    @Column(columnDefinition = "BLOB")
    var image: ByteArray,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    var tournament: Tournament,
    @Enumerated(EnumType.STRING)
    var state: MediaState,
) {
    companion object {
        const val MAX_IMAGE_SIZE: Int = 5 * 1_024 * 1_024 // 2MB
        const val MAX_IMAGE_WIDTH: Int = 1_920 * 4
        const val MAX_IMAGE_HEIGHT: Int = 1_080 * 4
        val allowedTypes: Array<String> =
            arrayOf(
                "image/jpeg",
                "image/jpg",
                "image/png",
                "image/heic",
                "image/heif",
            )
    }
}
