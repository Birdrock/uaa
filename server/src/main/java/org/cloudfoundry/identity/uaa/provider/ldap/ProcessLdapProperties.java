package org.cloudfoundry.identity.uaa.provider.ldap;

import static org.cloudfoundry.identity.uaa.provider.LdapIdentityProviderDefinition.LDAP_TLS_EXTERNAL;
import static org.cloudfoundry.identity.uaa.provider.LdapIdentityProviderDefinition.LDAP_TLS_NONE;
import static org.cloudfoundry.identity.uaa.provider.LdapIdentityProviderDefinition.LDAP_TLS_SIMPLE;
import static org.springframework.util.StringUtils.hasText;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.net.ssl.SSLSocketFactory;
import org.apache.directory.api.util.DummySSLSocketFactory;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.cloudfoundry.identity.uaa.provider.ldap.extension.DefaultTlsDirContextAuthenticationStrategy;
import org.cloudfoundry.identity.uaa.provider.ldap.extension.ExternalTlsDirContextAuthenticationStrategy;
import org.cloudfoundry.identity.uaa.security.LdapSocketFactory;
import org.springframework.ldap.core.support.AbstractTlsDirContextAuthenticationStrategy;
import org.springframework.ldap.core.support.DirContextAuthenticationStrategy;
import org.springframework.ldap.core.support.SimpleDirContextAuthenticationStrategy;

public class ProcessLdapProperties {

  public static final String LDAP_SOCKET_FACTORY = "java.naming.ldap.factory.socket";
  public static final String LDAP_SSL_SOCKET_FACTORY =
      "org.cloudfoundry.identity.ldap.ssl.factory.socket";
  public static final String SKIP_SSL_VERIFICATION_SOCKET_FACTORY =
      DummySSLSocketFactory.class.getName();
  public static final String EXPIRY_CHECKING_SOCKET_FACTORY = LdapSocketFactory.class.getName();

  private boolean disableSslVerification;
  private String baseUrl;
  private String tlsConfig;

  public ProcessLdapProperties(String baseUrl, boolean disableSslVerification, String tlsConfig) {
    this.baseUrl = baseUrl;
    this.disableSslVerification = disableSslVerification;
    this.tlsConfig = tlsConfig;
  }

  public Map process(Map map) {
    Map result = new LinkedHashMap(map);
    if (isDisableSslVerification()) {
      result.put(LDAP_SSL_SOCKET_FACTORY, SKIP_SSL_VERIFICATION_SOCKET_FACTORY);
    } else {
      result.put(LDAP_SSL_SOCKET_FACTORY, EXPIRY_CHECKING_SOCKET_FACTORY);
    }

    if (isLdapsUrl()) {
      result.put(LDAP_SOCKET_FACTORY, result.get(LDAP_SSL_SOCKET_FACTORY));
    }
    return result;
  }

  public boolean isLdapsUrl() {
    return baseUrl != null && baseUrl.startsWith("ldaps");
  }

  public boolean isDisableSslVerification() {
    return disableSslVerification;
  }

  public void setDisableSslVerification(boolean disableSslVerification) {
    this.disableSslVerification = disableSslVerification;
  }

  public SSLSocketFactory getSSLSocketFactory()
      throws IllegalAccessException, InstantiationException, ClassNotFoundException {
    Class<?> clazz =
        Class.forName(
            (String) (process(new HashMap()).get(LDAP_SSL_SOCKET_FACTORY)),
            true,
            ProcessLdapProperties.class.getClassLoader());
    return (SSLSocketFactory) clazz.newInstance();
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public DirContextAuthenticationStrategy getAuthenticationStrategy()
      throws ClassNotFoundException, NoSuchAlgorithmException, IllegalAccessException,
          InstantiationException, KeyManagementException {
    if (!hasText(tlsConfig)) {
      tlsConfig = LDAP_TLS_NONE;
    }
    AbstractTlsDirContextAuthenticationStrategy tlsStrategy;
    switch (tlsConfig) {
      case LDAP_TLS_NONE:
        return new SimpleDirContextAuthenticationStrategy();
      case LDAP_TLS_SIMPLE:
        tlsStrategy = new DefaultTlsDirContextAuthenticationStrategy();
        break;
      case LDAP_TLS_EXTERNAL:
        tlsStrategy = new ExternalTlsDirContextAuthenticationStrategy();
        break;
      default:
        throw new IllegalArgumentException(tlsConfig);
    }
    tlsStrategy.setHostnameVerifier(new AllowAllHostnameVerifier());
    tlsStrategy.setSslSocketFactory(getSSLSocketFactory());
    return tlsStrategy;
  }
}
