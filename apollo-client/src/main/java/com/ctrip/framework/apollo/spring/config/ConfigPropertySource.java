package com.ctrip.framework.apollo.spring.config;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import org.springframework.core.env.EnumerablePropertySource;

import java.util.Set;

/**
 * Property source wrapper for Config
 * 把config和PropertySource关联起来
 *
 * Config的包装类，便于进行枚举配置所有key
 * @see org.springframework.core.env.EnumerablePropertySource
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public class ConfigPropertySource extends EnumerablePropertySource<Config> {
  private static final String[] EMPTY_ARRAY = new String[0];

  ConfigPropertySource(String name, Config source) {
    super(name, source);
  }

  /**
   * 实现EnumerablePropertySource的接口，便于枚举所有配置key
   */
  @Override
  public String[] getPropertyNames() {
    Set<String> propertyNames = this.source.getPropertyNames();
    if (propertyNames.isEmpty()) {
      return EMPTY_ARRAY;
    }
    return propertyNames.toArray(new String[propertyNames.size()]);
  }

  /**
   * 实现PropertySource接口
   */
  @Override
  public Object getProperty(String name) {
    return this.source.getProperty(name, null);
  }

  public void addChangeListener(ConfigChangeListener listener) {
    this.source.addChangeListener(listener);
  }
}
