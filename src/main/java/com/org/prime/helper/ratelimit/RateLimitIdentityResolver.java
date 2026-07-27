package com.org.prime.helper.ratelimit;

import com.org.prime.model.ratelimit.IdentityType;
import com.org.prime.model.ratelimit.RateLimitIdentity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * Resolves a trusted rate-limit identity.
 *
 * <p>Authenticated principals take priority. An authenticated client ID set by
 * a trusted authentication filter is used next. IP address is only a fallback
 * for anonymous requests.</p>
 */
@Component
public class RateLimitIdentityResolver {

    public static final String AUTHENTICATED_CLIENT_ID = RateLimitIdentityResolver.class.getName() + ".clientId";

    public static final String AUTHENTICATED_TENANT_ID = RateLimitIdentityResolver.class.getName() + ".tenantId";

    /**
     * Resolves the identity for a request.
     */
    public RateLimitIdentity resolve(HttpServletRequest request) {
        String tenant = resolveTenant(request);
        Principal principal = request.getUserPrincipal();

        if (principal != null) {
            return new RateLimitIdentity(IdentityType.AUTHENTICATED, String.format("%s:user:%s", tenant, principal.getName()));
        }

        Object clientId = request.getAttribute(AUTHENTICATED_CLIENT_ID);

        if (clientId instanceof String value && !value.isBlank()) {
            return new RateLimitIdentity(IdentityType.CLIENT, String.format("%s:client:%s", tenant, value));
        }

        return new RateLimitIdentity(IdentityType.ANONYMOUS, String.format("%s:ip:%s", tenant, request.getRemoteAddr()));
    }

    private String resolveTenant(HttpServletRequest request) {
        Object tenantId = request.getAttribute(AUTHENTICATED_TENANT_ID);

        if (tenantId instanceof String value && !value.isBlank()) {
            return value;
        }

        return "default";
    }
}
