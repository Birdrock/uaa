package org.cloudfoundry.identity.uaa.login;

import static org.cloudfoundry.identity.uaa.login.ForcePasswordChangeController.FORCE_PASSWORD_EXPIRED_USER;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.cloudfoundry.identity.uaa.authentication.PasswordChangeRequiredException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.ExceptionMappingAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;

public class UaaAuthenticationFailureHandler
    implements AuthenticationFailureHandler, LogoutHandler {

  private ExceptionMappingAuthenticationFailureHandler delegate;
  private CurrentUserCookieFactory currentUserCookieFactory;

  public UaaAuthenticationFailureHandler(
      ExceptionMappingAuthenticationFailureHandler delegate,
      CurrentUserCookieFactory currentUserCookieFactory) {
    this.delegate = delegate;
    this.currentUserCookieFactory = currentUserCookieFactory;
  }

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException, ServletException {
    addCookie(response);
    if (exception != null) {
      if (exception instanceof PasswordChangeRequiredException) {
        request
            .getSession()
            .setAttribute(
                FORCE_PASSWORD_EXPIRED_USER,
                ((PasswordChangeRequiredException) exception).getAuthentication());
      }
    }
    if (delegate != null) {
      delegate.onAuthenticationFailure(request, response, exception);
    }
  }

  @Override
  public void logout(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
    addCookie(response);
  }

  private void addCookie(HttpServletResponse response) {
    Cookie clearCurrentUserCookie = currentUserCookieFactory.getNullCookie();
    response.addCookie(clearCurrentUserCookie);
  }
}
