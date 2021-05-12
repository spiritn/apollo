package com.ctrip.framework.apollo.internals;

import com.ctrip.framework.apollo.ConfigFileChangeListener;
import com.ctrip.framework.apollo.PropertiesCompatibleConfigFile;
import com.ctrip.framework.apollo.enums.ConfigSourceType;
import com.ctrip.framework.apollo.model.ConfigFileChangeEvent;
import com.google.common.base.Preconditions;

import java.util.Properties;

/**
 * 这应该是保存在缓存中的配置ConfigRepository
 */
public class PropertiesCompatibleFileConfigRepository extends AbstractConfigRepository implements
    ConfigFileChangeListener {
  private final PropertiesCompatibleConfigFile configFile;
  // volatile修饰，保证可见性。因为可能会有多个线程读写
  private volatile Properties cachedProperties;

  public PropertiesCompatibleFileConfigRepository(PropertiesCompatibleConfigFile configFile) {
    this.configFile = configFile;
    this.configFile.addChangeListener(this);
    this.trySync();
  }

  @Override
  protected synchronized void sync() {
    Properties current = configFile.asProperties();

    Preconditions.checkState(current != null, "PropertiesCompatibleConfigFile.asProperties should never return null");

    // 有更新就替换掉当前缓存的配置属性
    if (cachedProperties != current) {
      cachedProperties = current;
      // 并发出通知
      this.fireRepositoryChange(configFile.getNamespace(), cachedProperties);
    }
  }

  @Override
  public Properties getConfig() {
    if (cachedProperties == null) {
      sync();
    }
    return cachedProperties;
  }

  @Override
  public void setUpstreamRepository(ConfigRepository upstreamConfigRepository) {
    //config file is the upstream, so no need to set up extra upstream
  }

  @Override
  public ConfigSourceType getSourceType() {
    return configFile.getSourceType();
  }

  /**
   * 在接收到其他Repository发出更新的通知后，此方法会被调用更新配置
   * @param changeEvent the event for this change
   */
  @Override
  public void onChange(ConfigFileChangeEvent changeEvent) {
    this.trySync();
  }
}
