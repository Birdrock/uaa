package org.cloudfoundry.identity.uaa.impl.config;

import java.util.Properties;
import org.springframework.beans.factory.FactoryBean;

/**
 * Factory for Java Properties that reads from a YAML source. YAML is a nice human-readable format
 * for configuration, and it has some useful hierarchical properties. It's more or less a superset
 * of JSON, so it has a lot of similar features. The Properties created by this factory have nested
 * paths for hierarchical objects, so for instance this YAML
 *
 * <pre>
 * environments:
 *   dev:
 *     url: http://dev.bar.com
 *     name: Developer Setup
 *   prod:
 *     url: http://foo.bar.com
 *     name: My Cool App
 * </pre>
 *
 * is transformed into these Properties:
 *
 * <pre>
 * environments.dev.url=http://dev.bar.com
 * environments.dev.name=Developer Setup
 * environments.prod.url=http://foo.bar.com
 * environments.prod.name=My Cool App
 * </pre>
 *
 * Lists are represented as comma-separated values (useful for simple String values) and also as
 * property keys with <code>[]</code> dereferencers, for example this YAML:
 *
 * <pre>
 * servers:
 * - dev.bar.com
 * - foo.bar.com
 * </pre>
 *
 * becomes java Properties like this:
 *
 * <pre>
 * servers=dev.bar.com,foo.bar.com
 * servers[0]=dev.bar.com
 * servers[1]=foo.bar.com
 * </pre>
 *
 * @author Dave Syer
 */
public class YamlPropertiesFactoryBean extends YamlProcessor implements FactoryBean<Properties> {

  private Properties instance;

  @Override
  public Properties getObject() {
    if (instance == null) {
      instance = doGetObject();
    }
    return instance;
  }

  private Properties doGetObject() {
    final Properties result = new Properties();
    MatchCallback callback = (properties, map) -> result.putAll(properties);
    process(callback);
    return result;
  }

  @Override
  public Class<?> getObjectType() {
    return Properties.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }
}
