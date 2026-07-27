package com.org.prime.helper.rateLimit;

import com.org.prime.helper.ratelimit.RateLimitIdentityResolver;
import com.org.prime.model.ratelimit.IdentityType;
import com.org.prime.model.ratelimit.RateLimitIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.security.Principal;

import static com.org.prime.helper.ratelimit.RateLimitIdentityResolver.AUTHENTICATED_CLIENT_ID;
import static com.org.prime.helper.ratelimit.RateLimitIdentityResolver.AUTHENTICATED_TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RateLimitIdentityResolverTest {

    private RateLimitIdentityResolver identityResolver;

    @BeforeEach
    void setUp() {
        identityResolver = new RateLimitIdentityResolver();
    }

    @Test
    void shouldResolveAuthenticatedPrincipalWithDefaultTenant() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setUserPrincipal(principal("akash"));

        RateLimitIdentity identity = identityResolver.resolve(request);

        assertEquals(IdentityType.AUTHENTICATED, identity.type());
        assertEquals("default:user:akash", identity.value());
    }

    @Test
    void shouldResolveAuthenticatedPrincipalWithTenant() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setUserPrincipal(principal("akash"));
        request.setAttribute(AUTHENTICATED_TENANT_ID, "tenant-a");

        RateLimitIdentity identity = identityResolver.resolve(request);

        assertEquals(IdentityType.AUTHENTICATED, identity.type());
        assertEquals("tenant-a:user:akash", identity.value());
    }

    @Test
    void shouldPreferPrincipalOverClientId() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setUserPrincipal(principal("akash"));
        request.setAttribute(AUTHENTICATED_CLIENT_ID, "client-123");
        request.setAttribute(AUTHENTICATED_TENANT_ID, "tenant-a");

        RateLimitIdentity identity = identityResolver.resolve(request);

        assertEquals(IdentityType.AUTHENTICATED, identity.type());
        assertEquals("tenant-a:user:akash", identity.value());
    }

    @Test
    void shouldResolveAuthenticatedClientId() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setAttribute(AUTHENTICATED_CLIENT_ID, "client-123");
        request.setAttribute(AUTHENTICATED_TENANT_ID, "tenant-a");

        RateLimitIdentity identity = identityResolver.resolve(request);

        assertEquals(IdentityType.CLIENT, identity.type());
        assertEquals("tenant-a:client:client-123", identity.value());
    }

    @Test
    void shouldUseIpWhenClientIdIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setRemoteAddr("192.0.2.10");

        RateLimitIdentity identity = identityResolver.resolve(request);

        assertEquals(IdentityType.ANONYMOUS, identity.type());
        assertEquals("default:ip:192.0.2.10", identity.value());
    }

    @Test
    void shouldUseIpWhenClientIdIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setAttribute(AUTHENTICATED_CLIENT_ID, " ");
        request.setRemoteAddr("192.0.2.11");

        RateLimitIdentity identity = identityResolver.resolve(request);

        assertEquals(IdentityType.ANONYMOUS, identity.type());
        assertEquals("default:ip:192.0.2.11", identity.value());
    }

    @Test
    void shouldUseDefaultTenantWhenTenantIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setAttribute(AUTHENTICATED_CLIENT_ID, "client-456");
        request.setAttribute(AUTHENTICATED_TENANT_ID, " ");

        RateLimitIdentity identity = identityResolver.resolve(request);

        assertEquals(IdentityType.CLIENT, identity.type());
        assertEquals("default:client:client-456", identity.value());
    }

    @Test
    void shouldUseDefaultTenantWhenTenantIsNotString() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setAttribute(AUTHENTICATED_CLIENT_ID, "client-789");
        request.setAttribute(AUTHENTICATED_TENANT_ID, 123);

        RateLimitIdentity identity = identityResolver.resolve(request);

        assertEquals(IdentityType.CLIENT, identity.type());
        assertEquals("default:client:client-789", identity.value());
    }

    @Test
    void shouldIgnoreNonStringClientId() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setAttribute(AUTHENTICATED_CLIENT_ID, 123);
        request.setRemoteAddr("192.0.2.12");

        RateLimitIdentity identity = identityResolver.resolve(request);

        assertEquals(IdentityType.ANONYMOUS, identity.type());
        assertEquals("default:ip:192.0.2.12", identity.value());
    }

    private Principal principal(String name) {
        return () -> name;
    }
}
