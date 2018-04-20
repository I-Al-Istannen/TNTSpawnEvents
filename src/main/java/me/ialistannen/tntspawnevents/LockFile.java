package me.ialistannen.tntspawnevents;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.bukkit.configuration.ConfigurationSection;

public class LockFile {

  /**
   * Checks if the lockfile exists.
   *
   * @param configuration the configuration to read from
   * @return true if the lockfile exists.
   */
  public static boolean isLocked(ConfigurationSection configuration) {
    return configuration.contains("lock.file") && Files.exists(getLockFilePath(configuration));
  }

  /**
   * Attempts to lock, failing if {@link #isLocked(ConfigurationSection)} is true.
   *
   * @param configuration the configuration to read from
   */
  public static boolean acquireLock(ConfigurationSection configuration) {
    try {
      if (isLocked(configuration)) {
        return false;
      }

      Path tempFile = Files.createTempFile("TNTSpawnEvents", "lockfile");
      configuration.set("lock.file", tempFile.toAbsolutePath().toString());

      tempFile.toFile().deleteOnExit();

      return true;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static Path getLockFilePath(ConfigurationSection configuration) {
    return Paths.get(configuration.getString("lock.file"));
  }
}
