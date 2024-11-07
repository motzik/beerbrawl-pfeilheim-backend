/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.entity;

public interface SharedMediaMetadataProjection {
    Long getId();

    String getAuthor();

    String getTitle();

    Long getTournamentId();
}
