/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint.dto;

import at.beerbrawl.backend.enums.MediaState;

public record SharedMediaMetadataDto(
    Long id,
    String author,
    String title,
    MediaState state,
    Long tournamentId
) {}
