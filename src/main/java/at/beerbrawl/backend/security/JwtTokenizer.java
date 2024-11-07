/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.security;

import at.beerbrawl.backend.config.properties.SecurityProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Component;

@SuppressWarnings("deprecation")
@Component
public class JwtTokenizer {

    private final SecurityProperties securityProperties;

    public JwtTokenizer(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public String getAuthToken(String user, List<String> roles) {
        byte[] signingKey = securityProperties.getJwtSecret().getBytes();
        String token = Jwts.builder()
            .signWith(Keys.hmacShaKeyFor(signingKey), SignatureAlgorithm.HS512)
            .setHeaderParam("typ", securityProperties.getJwtType())
            .setIssuer(securityProperties.getJwtIssuer())
            .setAudience(securityProperties.getJwtAudience())
            .setSubject(user)
            .setExpiration(
                new Date(System.currentTimeMillis() + securityProperties.getJwtExpirationTime())
            )
            .claim("rol", roles)
            .compact();
        return securityProperties.getAuthTokenPrefix() + token;
    }
}
