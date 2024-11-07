/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TournamentUpdateTeamDto(@NotBlank @Size(min = 3, max = 20) String name) {}
