package de.mhus.nimbus.shared.settings;

import de.mhus.nimbus.shared.service.SSettingsService;

public class SettingInteger implements SettingValue {

  private final int defaultValue;
  private final String key;
  private final SSettingsService service;

  public SettingInteger(String key, SSettingsService service, int defaultValue) {
    this.key = key;
    this.service = service;
    this.defaultValue = defaultValue;
    get(); // touch to create
  }

  public int get() {
    if (service == null || key == null) {
      return defaultValue;
    }
    return service.getOrCreateIntValue(key, defaultValue);
  }

  public void set(int value) {
    if (service != null && key != null) {
      service.setIntValue(key, value);
    }
  }

  public String getKey() {
    return key;
  }
}
