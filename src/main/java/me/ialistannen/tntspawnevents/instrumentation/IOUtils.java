package me.ialistannen.tntspawnevents.instrumentation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class IOUtils {


  /**
   * Completely reads an inputstream and returns its content as a byte array.
   *
   * @param inputStream the {@link InputStream} to read
   * @return the read bytes
   */
  public static byte[] getAllBytes(InputStream inputStream) {
    Objects.requireNonNull(inputStream, "inputStream can not be null!");

    try {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      byte[] buffer = new byte[2048];
      int tmp;
      while ((tmp = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, tmp);
      }

      inputStream.close();

      return outputStream.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
