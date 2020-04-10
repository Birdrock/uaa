package org.cloudfoundry.identity.uaa.provider.saml;

import static org.cloudfoundry.identity.uaa.provider.ExternalIdentityProviderDefinition.USER_ATTRIBUTE_PREFIX;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.cloudfoundry.identity.uaa.authentication.UaaAuthentication;
import org.cloudfoundry.identity.uaa.authentication.UaaPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.providers.ExpiringUsernameAuthenticationToken;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class LoginSamlAuthenticationToken extends ExpiringUsernameAuthenticationToken {

  public static final String AUTHENTICATION_CONTEXT_CLASS_REFERENCE = "acr";

  private final UaaPrincipal uaaPrincipal;

  public LoginSamlAuthenticationToken(
      UaaPrincipal uaaPrincipal, ExpiringUsernameAuthenticationToken token) {
    super(token.getTokenExpiration(), uaaPrincipal, token.getCredentials(), token.getAuthorities());
    this.uaaPrincipal = uaaPrincipal;
  }

  public UaaPrincipal getUaaPrincipal() {
    return uaaPrincipal;
  }

  public UaaAuthentication getUaaAuthentication(
      List<? extends GrantedAuthority> uaaAuthorityList,
      Set<String> externalGroups,
      MultiValueMap<String, String> userAttributes) {
    LinkedMultiValueMap<String, String> customAttributes = new LinkedMultiValueMap<>();
    for (Map.Entry<String, List<String>> entry : userAttributes.entrySet()) {
      if (entry.getKey().startsWith(USER_ATTRIBUTE_PREFIX)) {
        customAttributes.put(
            entry.getKey().substring(USER_ATTRIBUTE_PREFIX.length()), entry.getValue());
      }
    }
    UaaAuthentication authentication =
        new UaaAuthentication(
            getUaaPrincipal(),
            getCredentials(),
            uaaAuthorityList,
            externalGroups,
            customAttributes,
            null,
            isAuthenticated(),
            System.currentTimeMillis(),
            getTokenExpiration() == null ? -1l : getTokenExpiration().getTime());
    authentication.setAuthenticationMethods(Collections.singleton("ext"));
    List<String> acrValues = userAttributes.get(AUTHENTICATION_CONTEXT_CLASS_REFERENCE);
    if (acrValues != null) {
      authentication.setAuthContextClassRef(new HashSet<>(acrValues));
    }
    return authentication;
  }
}
