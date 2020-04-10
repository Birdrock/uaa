package org.cloudfoundry.identity.uaa.authentication.manager;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.error.OAuth2AuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;

public class ScopeAuthenticationFilter implements Filter {

  private AuthenticationManager authenticationManager;
  private AuthenticationEntryPoint authenticationEntryPoint = new OAuth2AuthenticationEntryPoint();

  public AuthenticationEntryPoint getAuthenticationEntryPoint() {
    return authenticationEntryPoint;
  }

  public void setAuthenticationEntryPoint(AuthenticationEntryPoint authenticationEntryPoint) {
    this.authenticationEntryPoint = authenticationEntryPoint;
  }

  public AuthenticationManager getAuthenticationManager() {
    return authenticationManager;
  }

  public void setAuthenticationManager(AuthenticationManager authenticationManager) {
    this.authenticationManager = authenticationManager;
  }

  @Override
  public void init(FilterConfig filterConfig) {}

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    try {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication == null || (!(authentication instanceof OAuth2Authentication))) {
        throw new InvalidTokenException("Missing oauth token.");
      }
      authenticationManager.authenticate(authentication);
      chain.doFilter(request, response);
    } catch (OAuth2Exception e) {
      authenticationEntryPoint.commence(
          (HttpServletRequest) request,
          (HttpServletResponse) response,
          new InsufficientAuthenticationException("Insufficient authentication", e));
      SecurityContextHolder.clearContext();
    } catch (AuthenticationException e) {
      authenticationEntryPoint.commence(
          (HttpServletRequest) request, (HttpServletResponse) response, e);
      SecurityContextHolder.clearContext();
    }
  }

  @Override
  public void destroy() {}
}
