/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint.dto;

public record TeamDto(Long id, String name, Boolean checkedIn, Boolean currentlyPlaying) {}
