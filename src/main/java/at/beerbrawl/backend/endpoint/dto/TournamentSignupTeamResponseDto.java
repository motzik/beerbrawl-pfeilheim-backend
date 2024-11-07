/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint.dto;

import at.beerbrawl.backend.entity.Tournament;

/**
 * Response for team signup. Compared to enum, has openapi-generator support.
 */
public record TournamentSignupTeamResponseDto(Tournament.SignupTeamResult signupTeamResult) {}
