package org.cloudfoundry.identity.uaa.authentication.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;
import org.cloudfoundry.identity.uaa.authentication.AuthzAuthenticationRequest;
import org.cloudfoundry.identity.uaa.authentication.InvalidCodeException;
import org.cloudfoundry.identity.uaa.authentication.UaaAuthentication;
import org.cloudfoundry.identity.uaa.authentication.UaaAuthenticationDetails;
import org.cloudfoundry.identity.uaa.authentication.UaaPrincipal;
import org.cloudfoundry.identity.uaa.codestore.ExpiringCode;
import org.cloudfoundry.identity.uaa.codestore.ExpiringCodeStore;
import org.cloudfoundry.identity.uaa.codestore.ExpiringCodeType;
import org.cloudfoundry.identity.uaa.user.UaaAuthority;
import org.cloudfoundry.identity.uaa.user.UaaUser;
import org.cloudfoundry.identity.uaa.user.UaaUserDatabase;
import org.cloudfoundry.identity.uaa.util.JsonUtils;
import org.cloudfoundry.identity.uaa.zone.IdentityZoneHolder;
import org.cloudfoundry.identity.uaa.zone.MultitenantClientServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.oauth2.provider.NoSuchClientException;

/** @author Dave Syer */
public class AutologinAuthenticationManager implements AuthenticationManager {

  private Logger logger = LoggerFactory.getLogger(getClass());

  private ExpiringCodeStore codeStore;
  private MultitenantClientServices clientDetailsService;
  private UaaUserDatabase userDatabase;

  public void setExpiringCodeStore(ExpiringCodeStore expiringCodeStore) {
    this.codeStore = expiringCodeStore;
  }

  public void setClientDetailsService(MultitenantClientServices clientDetailsService) {
    this.clientDetailsService = clientDetailsService;
  }

  public void setUserDatabase(UaaUserDatabase userDatabase) {
    this.userDatabase = userDatabase;
  }

  public ExpiringCode doRetrieveCode(String code) {
    return codeStore.retrieveCode(code, IdentityZoneHolder.get().getId());
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {

    if (!(authentication instanceof AuthzAuthenticationRequest)) {
      return null;
    }

    AuthzAuthenticationRequest request = (AuthzAuthenticationRequest) authentication;
    Map<String, String> info = request.getInfo();
    String code = info.get("code");

    ExpiringCode expiringCode = doRetrieveCode(code);
    Map<String, String> codeData = null;
    try {
      if (expiringCode == null) {
        logger.debug("Autologin code has expired");
        throw new InvalidCodeException("expired_code", "Expired code", 422);
      }
      codeData =
          JsonUtils.readValue(expiringCode.getData(), new TypeReference<Map<String, String>>() {});
      if (!isAutologinCode(expiringCode.getIntent(), codeData.get("action"))) {
        logger.debug("Code is not meant for autologin");
        throw new InvalidCodeException("invalid_code", "Not an autologin code", 422);
      }
    } catch (JsonUtils.JsonUtilException x) {
      throw new BadCredentialsException("JsonConversion error", x);
    }

    String userId = codeData.get("user_id");
    String clientId = codeData.get(OAuth2Utils.CLIENT_ID);

    if (clientId == null) {
      throw new BadCredentialsException("Cannot redeem provided code for user, client id missing");
    }

    try {
      clientDetailsService.loadClientByClientId(clientId, IdentityZoneHolder.get().getId());
    } catch (NoSuchClientException x) {
      throw new BadCredentialsException("Cannot redeem provided code for user, client is missing");
    }

    UaaUser user = null;

    try {
      user = userDatabase.retrieveUserById(userId);
    } catch (UsernameNotFoundException e) {
      throw new BadCredentialsException("Cannot redeem provided code for user, user is missing");
    }

    UaaAuthenticationDetails details = (UaaAuthenticationDetails) authentication.getDetails();
    if (!clientId.equals(details.getClientId())) {
      throw new BadCredentialsException("Cannot redeem provided code for user, client mismatch");
    }

    UaaPrincipal principal = new UaaPrincipal(user);

    return new UaaAuthentication(
        principal,
        UaaAuthority.USER_AUTHORITIES,
        (UaaAuthenticationDetails) authentication.getDetails());
  }

  private boolean isAutologinCode(String intent, String action) {
    return (intent != null && intent.equals(ExpiringCodeType.AUTOLOGIN.name()))
        || (action != null && action.equals(ExpiringCodeType.AUTOLOGIN.name()));
  }
}
