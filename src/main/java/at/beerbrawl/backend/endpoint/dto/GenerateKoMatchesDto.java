/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint.dto;

import java.util.List;

public record GenerateKoMatchesDto(List<Long> qualifiedTeamIds) {}
