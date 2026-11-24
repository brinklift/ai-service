package com.blift.aiservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.StringUtils;

/**
 * Accepts only access tokens Keycloak issued to the Blift client ({@code azp}).
 *
 * <p>Blift users live in Keycloak's {@code master} realm alongside clients such as
 * {@code admin-cli}, which allow a plain password grant. A token minted that way skips the
 * gateway's login and MFA checks, yet carries a valid signature and issuer. The gateway rejects
 * it (gateway JwtDecoderConfig); this repeats the check here so the service does not depend on
 * every request having passed through the gateway.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.jwt.issuer-uri")
public class BliftClientJwtDecoderConfig {

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}") String jwkSetUri,
            @Value("${keycloak.client-id:blift_auth}") String clientId) {
        String jwks = StringUtils.hasText(jwkSetUri) ? jwkSetUri : issuerUri + "/protocol/openid-connect/certs";
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwks).build();
        OAuth2TokenValidator<Jwt> issuedToBlift = jwt -> {
            if (clientId.equals(jwt.getClaimAsString("azp"))) {
                return OAuth2TokenValidatorResult.success();
            }
            log.warn("Rejected token issued to client '{}'", jwt.getClaimAsString("azp"));
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Token was not issued to the Blift client", null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuerUri), issuedToBlift));
        return decoder;
    }
}
