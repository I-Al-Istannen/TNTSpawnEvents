package me.ialistannen.tntspawnevents.instrumentation;

import java.lang.management.ManagementFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

  private static final Pattern PID_PATTERN = Pattern.compile("(\\d+)@.+");

  /**
   * Returns the PID of the current process.
   *
   * @return the pid of the JVM process
   */
  public static int getPid() {
    String name = ManagementFactory.getRuntimeMXBean().getName();
    Matcher matcher = PID_PATTERN.matcher(name);

    if (!matcher.matches()) {
      return -1;
    }

    return Integer.parseInt(matcher.group(1));
  }

  /**
   * Returns the bytes for a given class.
   *
   * @param clazz the class to get the bytes for
   * @return the read bytes
   */
  public static byte[] getClassFileBytes(Class<?> clazz) {
    String resourceName = getResourceName(clazz);
    return IOUtils.getAllBytes(getClassLoader(clazz).getResourceAsStream(resourceName));
  }

  /**
   * Checks if the class can be found.
   *
   * @param clazz the class to find
   * @return true if the bytes for the class can be found
   */
  public static boolean canFindClassBytes(Class<?> clazz) {
    System.out.println(getClassLoader(clazz));
    return getClassLoader(clazz).getResource(getResourceName(clazz)) != null;
  }

  private static ClassLoader getClassLoader(Class<?> clazz) {
    if (clazz.getClassLoader() == null) {
      return ClassLoader.getSystemClassLoader();
    }
    return clazz.getClassLoader();
  }

  /**
   * Returns the resource name for a class (i.e. replace "." with "/" and append ".class").
   *
   * @param clazz the class to get it for
   * @return the resource name
   */
  public static String getResourceName(Class<?> clazz) {
    return clazz.getName().replace('.', '/') + ".class";
  }
}