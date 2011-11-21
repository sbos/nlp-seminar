package ru.ispras.nlpcourse;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Settings {
  private static Configuration configuration;
  private static Log Log = LogFactory.getLog(Settings.class);

  static {
    try {
      configuration = new XMLConfiguration("settings.xml");
      Log.debug("Settings loaded properly");
    } catch (ConfigurationException e) {
      Log.error("Could not load configuration", e);
    }
  }

  public static String getTemporaryPath() {
    return configuration.getString("path.tmp");
  }

  public static int getTimeoutMinutes() {
    return configuration.getInt("timeout");
  }

  public static String getGoogleLogin() {
    return configuration.getString("google.account[@login]");
  }

  public static String getGooglePassword() {
    return configuration.getString("google.account[@password]");
  }

  public static int getFusionTableID() {
    return configuration.getInt("google.fusiontable");
  }

  public static String getTesterPath() {
    return configuration.getString("path.tester");
  }
}
