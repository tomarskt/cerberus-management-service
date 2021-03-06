/*
 * Copyright (c) 2016 Nike, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.security;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

/**
 * Custom security context that will host the auth principal object that can be utilized to check user details
 * during a request.
 */
public class CerberusSecurityContext implements SecurityContext {

    public static final String AUTH_SCHEME = "CERBERUS_TOKEN";

    private final CerberusPrincipal authPrincipal;

    private final String scheme;

    public CerberusSecurityContext(CerberusPrincipal authPrincipal, String scheme) {
        this.authPrincipal = authPrincipal;
        this.scheme = scheme;
    }

    @Override
    public Principal getUserPrincipal() {
        return authPrincipal;
    }

    @Override
    public boolean isUserInRole(String role) {
        return authPrincipal.hasRole(role);
    }

    @Override
    public boolean isSecure() {
        return "https".equals(scheme);
    }

    @Override
    public String getAuthenticationScheme() {
        return AUTH_SCHEME;
    }
}
