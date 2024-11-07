/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.endpoint.dto;

import java.util.List;

public record ValidationErrorDto(List<String> errors) {}
