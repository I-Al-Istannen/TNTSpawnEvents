package me.ialistannen.tntspawnevents.libs;


import java.util.Locale;

public class OsUtils {

  /**
   * Returns the os type.
   *
   * @return the os type
   */
  public static OsType getOsType() {
    String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
    if (osName.contains("windows")) {
      return OsType.WINDOWS;
    }

    if (osName.contains("linux")) {
      return OsType.LINUX;
    }

    if (osName.contains("os x") || osName.contains("mac") || osName.contains("darwin")) {
      return OsType.MAC;
    }

    return OsType.UNKNOWN;
  }

  /**
   * Returns the current java version (approx)
   *
   * @return the current java version
   */
  public static String getCurrentJavaVersion() {
    String value = System.getProperty("java.version");
    if (value.startsWith("1.")) {
      return value.substring(0, 3);
    } else {
      return value.substring(0, value.indexOf('.'));
    }
  }

  public enum OsType {
    LINUX(".so"), WINDOWS(".dll"), MAC(".dylib"), UNKNOWN(".nop");

    private String executableSuffix;

    OsType(String executableSuffix) {
      this.executableSuffix = executableSuffix;
    }

    /**
     * The prefix of the resources for this os.
     *
     * @return the resource prefix (folder name)
     */
    public String getResourcePrefix() {
      return name().toLowerCase();
    }

    public String getExecutableSuffix() {
      return executableSuffix;
    }
  }
}
