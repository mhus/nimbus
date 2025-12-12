package de.mhus.nimbus.shared.settings;

import de.mhus.nimbus.shared.service.SSettingsService;

public class SettingDouble implements SettingValue {

  private final double defaultValue;
  private final String key;
  private final SSettingsService service;

  public SettingDouble(String key, SSettingsService service, double defaultValue) {
    this.key = key;
    this.service = service;
    this.defaultValue = defaultValue;
    get(); // touch to create
  }

  public double get() {
    if (service == null || key == null) {
      return defaultValue;
    }
    return service.getOrCreateDoubleValue(key, defaultValue);
  }

  public void set(double value) {
    if (service != null && key != null) {
      service.setDoubleValue(key, value);
    }
  }

  public String getKey() {
    return key;
  }
}
