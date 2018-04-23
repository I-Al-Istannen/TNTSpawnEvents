package me.ialistannen.tntspawnevents.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.bukkit.Bukkit;

public class BukkitReflectionUtils {

  private static final String SERVER_VERSION;


  static {
    String name = Bukkit.getServer().getClass().getPackage().getName();

    SERVER_VERSION = name.substring(name.lastIndexOf(".") + 1);
  }

  /**
   * Returns the NMS class with the given name.
   * <p>
   * <br>Format: {@code "net.minecraft.server.<version>.<name>"}
   *
   * @param name the name of the class
   * @return the class
   */
  public static Class<?> getNmsClass(String name) {
    return getClassUnchecked("net.minecraft.server." + SERVER_VERSION + "." + name);
  }

  /**
   * Calls {@link Class#forName(String)} but rethrows errors as an unchecked exception!
   *
   * @param name the name of the class
   * @return the class if found
   * @see Class#forName(String)
   */
  private static Class<?> getClassUnchecked(String name) {
    try {
      return Class.forName(name);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Invokes a given method without parameters.
   *
   * @param target the target class
   * @param handle the handle to invoke it on
   * @param name the name of the method
   * @param <T> the return type
   * @return the returned value
   */
  public static <T> T invokeMethod(Class<?> target, Object handle, String name) {
    try {
      Method method = target.getDeclaredMethod(name);
      method.setAccessible(true);

      @SuppressWarnings("unchecked")
      T result = (T) method.invoke(handle);
      return result;
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }
}
