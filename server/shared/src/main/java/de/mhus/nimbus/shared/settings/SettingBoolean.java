package de.mhus.nimbus.shared.settings;

import de.mhus.nimbus.shared.service.SSettingsService;

public class SettingBoolean implements SettingValue {

  private final boolean defaultValue;
  private final String key;
  private final SSettingsService service;

  public SettingBoolean(String key, SSettingsService service, boolean defaultValue) {
    this.key = key;
    this.service = service;
    this.defaultValue = defaultValue;
    get(); // touch to create
  }

  public boolean get() {
    if (service == null || key == null) {
      return defaultValue;
    }
    return service.getOrCreateBooleanValue(key, defaultValue);
  }

  public void set(boolean value) {
    if (service != null && key != null) {
      service.setBooleanValue(key, value);
    }
  }

  public String getKey() {
    return key;
  }
}
