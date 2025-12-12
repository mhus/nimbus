package de.mhus.nimbus.shared.settings;

import de.mhus.nimbus.shared.service.SSettingsService;

public class SettingPassword implements SettingValue {

  private final String defaultValue;
  private final String key;
  private final SSettingsService service;

  public SettingPassword(String key, SSettingsService service, String defaultValue) {
    this.key = key;
    this.service = service;
    this.defaultValue = defaultValue;
    get(); // touch to create
  }

  /**
   * Get the decrypted password value
   */
  public String get() {
    if (service == null || key == null) {
      return defaultValue;
    }
    String decrypted = service.getDecryptedPassword(key);
    if (decrypted == null) {
      // create with default
      service.setEncryptedPassword(key, defaultValue);
      return defaultValue;
    }
    return decrypted;
  }

  /**
   * Set the password value (will be encrypted)
   */
  public void set(String value) {
    if (service != null && key != null) {
      service.setEncryptedPassword(key, value);
    }
  }

  public String getKey() {
    return key;
  }
}
