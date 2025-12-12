package de.mhus.nimbus.shared.settings;

import de.mhus.nimbus.shared.service.SSettingsService;

public class SettingString implements SettingValue {

  private final String defaultValue;
  private final String key;
  private final SSettingsService service;

  public SettingString(String key, SSettingsService service, String defaultValue) {
    this.key = key;
    this.service = service;
    this.defaultValue = defaultValue;
    get(); // touch to create
  }

  public String get() {
    if (service == null || key == null) {
      return defaultValue;
    }
    return service.getOrCreateStringValue(key, defaultValue);
  }

  public void set(String value) {
    if (service != null && key != null) {
      service.setStringValue(key, value);
    }
  }

  public String getKey() {
    return key;
  }
}
