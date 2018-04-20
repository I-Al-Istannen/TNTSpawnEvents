package me.ialistannen.tntspawnevents.libs;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import me.ialistannen.tntspawnevents.instrumentation.IOUtils;
import me.ialistannen.tntspawnevents.libs.OsUtils.OsType;

public class ExternalLibraryUtils {

  /**
   * Loads all libraries in the folder by adding it to the java path.
   *
   * @param directory the directory containing the libraries.
   */
  public static void addLibrariesToPath(Path directory) {
    if (System.getProperty("java.library.path") == null) {
      System.setProperty("java.library.path", "");
    }

    System.setProperty(
        "java.library.path",
        System.getProperty("java.library.path") + File.pathSeparator + directory.toAbsolutePath()
    );
    try {

      Field sysPaths = ClassLoader.class.getDeclaredField("sys_paths");
      sysPaths.setAccessible(true);
      sysPaths.set(null, null);

    } catch (IllegalAccessException | NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Unpacks a collection of {@link ExternalLibrary}s to the system.
   *
   * @param libraries the libraries to unpack
   * @return the directory they were extracted to
   */
  public static Path unpackLibraries(Collection<ExternalLibrary> libraries) {
    try {
      OsType osType = OsUtils.getOsType();
      String osResourcePrefix = osType.getResourcePrefix();

      Path tempDirectory = Files.createTempDirectory("external-libs");

      // this needs to be before the call deleting the individual files, otherwise the dir is not
      // empty
      tempDirectory.toFile().deleteOnExit();

      for (ExternalLibrary library : libraries) {
        String folderPath =
            library.getBasePath() + "/" + osResourcePrefix + "/" + library.getExecutableName();

        Path resultPath = tempDirectory
            .resolve(library.getExecutableName() + osType.getExecutableSuffix());

        unpack(folderPath, resultPath);

        resultPath.toFile().deleteOnExit();
      }

      return tempDirectory;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Unpacks a given resource and writes it to the given file.
   *
   * @param resourcePath the resource
   * @param target the path to write to
   */
  private static void unpack(String resourcePath, Path target) {
    byte[] bytes = IOUtils.getAllBytes(ExternalLibrary.class.getResourceAsStream(resourcePath));
    try {
      Files.write(
          target,
          bytes,
          StandardOpenOption.WRITE,
          StandardOpenOption.TRUNCATE_EXISTING,
          StandardOpenOption.CREATE
      );
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
