package me.ialistannen.tntspawnevents.agent;

public class BukkitReflectionUtils {

  /**
   * Calls {@link Class#forName(String)} but rethrows errors as an unchecked exception!
   *
   * @param name the name of the class
   * @return the class if found
   * @see Class#forName(String)
   */
  public static Class<?> getClassUnchecked(String name) {
    try {
      return Class.forName(name);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}
