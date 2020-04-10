package org.cloudfoundry.identity.uaa.provider;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collection;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public class IdentityProviderValidationRequest {

  private final IdentityProvider provider;
  private final UsernamePasswordAuthentication credentials;

  @JsonCreator
  public IdentityProviderValidationRequest(
      @JsonProperty("provider") IdentityProvider provider,
      @JsonProperty("credentials") UsernamePasswordAuthentication credentials) {
    this.provider = provider;
    this.credentials = credentials;
  }

  public UsernamePasswordAuthentication getCredentials() {
    return credentials;
  }

  public IdentityProvider getProvider() {
    return provider;
  }

  public static class UsernamePasswordAuthentication implements Authentication {

    private final String username;
    private final String password;

    @JsonCreator
    public UsernamePasswordAuthentication(
        @JsonProperty("username") String username, @JsonProperty("password") String password) {
      this.password = password;
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public String getUsername() {
      return username;
    }

    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
      return null;
    }

    @JsonIgnore
    @Override
    public Object getCredentials() {
      return getPassword();
    }

    @JsonIgnore
    @Override
    public Object getDetails() {
      return null;
    }

    @JsonIgnore
    @Override
    public Object getPrincipal() {
      return getUsername();
    }

    @JsonIgnore
    @Override
    public boolean isAuthenticated() {
      return false;
    }

    @JsonIgnore
    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {}

    @JsonIgnore
    @Override
    public String getName() {
      return getUsername();
    }
  }
}
