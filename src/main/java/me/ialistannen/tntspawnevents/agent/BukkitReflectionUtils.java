package me.ialistannen.tntspawnevents.agent;

public class BukkitReflectionUtils {

  public static Class<?> getNMSClass(String name) {
    try {
      return Class.forName(name);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}
