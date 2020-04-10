package org.cloudfoundry.identity.uaa.authentication;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * Filter which processes and authenticates a client based on parameters client_id and client_secret
 * It sets the authentication to a client only Oauth2Authentication object as that is expected by
 * the LoginAuthenticationManager.
 */
public class LoginClientParametersAuthenticationFilter
    extends AbstractClientParametersAuthenticationFilter {

  @Override
  public void wrapClientCredentialLogin(
      HttpServletRequest req,
      HttpServletResponse res,
      Map<String, String> loginInfo,
      String clientId) {
    if (loginInfo.isEmpty()) {
      throw new BadCredentialsException("Request does not contain credentials.");
    } else if (clientAuthenticationManager == null || loginInfo.get(CLIENT_ID) == null) {
      logger.debug(
          "Insufficient resources to perform client authentication. AuthMgr:"
              + clientAuthenticationManager
              + "; clientId:"
              + clientId);
      throw new BadCredentialsException("Request does not contain client credentials.");
    } else {
      logger.debug("Located credentials in request, with keys: " + loginInfo.keySet());

      doClientCredentialLogin(req, loginInfo, clientId);
    }
  }
}
