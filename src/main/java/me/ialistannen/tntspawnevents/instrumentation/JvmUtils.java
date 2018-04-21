package me.ialistannen.tntspawnevents.instrumentation;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JvmUtils {

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
   * Attaches a given agent to the JVM by creating a temp agent jar for it.
   *
   * @param pid the pid of the JVM to attach to
   * @param agentMain the main class of the agent
   * @param arguments the arguments to pass to the agent
   * @param agentClasses all classes (including the main) that should be written to the agent
   */
  public static void attachToJvm(int pid, Class<?> agentMain, String arguments,
      Class<?>... agentClasses) {
    try {
      Path agentJar = createAgentJar();
      Manifest manifest = generateManifest(agentMain);

      List<Class<?>> javassistClasses = getJavassistClasses();
      Collections.addAll(javassistClasses, agentClasses);
      writeClassesToJar(agentJar, manifest, javassistClasses.toArray(new Class[0]));

      AgentRunner.run(agentJar, pid, arguments);

    } catch (IOException | AttachNotSupportedException | AgentInitializationException
        | AgentLoadException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private static Path createAgentJar() throws IOException {
    Path agentJar = Files.createTempFile("agent", ".jar");

    // Hacky shutdown hook to hopefully clean up
    agentJar.toFile().deleteOnExit();

    return agentJar;
  }

  private static Manifest generateManifest(Class<?> agentClass) {
    Manifest manifest = new Manifest();
    Attributes mainAttributes = manifest.getMainAttributes();
    mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
    mainAttributes.put(new Attributes.Name("Agent-Class"), agentClass.getName());
    mainAttributes.put(new Attributes.Name("Can-Retransform-Classes"), "true");
    mainAttributes.put(new Attributes.Name("Can-Redefine-Classes"), "true");

    return manifest;
  }

  /**
   * Writes the given classes into a jar file.
   *
   * @param jarFile the file to write to
   * @param jarManifest the manifest
   * @param classes the classes to write
   */
  public static void writeClassesToJar(Path jarFile, Manifest jarManifest, Class<?>... classes) {
    try (OutputStream outputStream = Files.newOutputStream(jarFile);
        JarOutputStream jarOutputStream = new JarOutputStream(outputStream, jarManifest)) {

      for (Class<?> aClass : classes) {
        String resourceName = ClassUtils.getResourceName(aClass);
        jarOutputStream.putNextEntry(new JarEntry(resourceName));
        jarOutputStream.write(ClassUtils.getClassFileBytes(aClass));
        jarOutputStream.closeEntry();
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static List<Class<?>> getJavassistClasses() throws URISyntaxException {
    Path path = Paths
        .get(JvmUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI());

    List<Class<?>> results = new ArrayList<>();

    try (JarFile jarFile = new JarFile(path.toFile())) {
      Enumeration<JarEntry> entries = jarFile.entries();

      while (entries.hasMoreElements()) {
        JarEntry jarEntry = entries.nextElement();

        if (jarEntry.getName().startsWith("javassist") && !jarEntry.isDirectory()) {
          results.add(
              Class.forName(
                  jarEntry.getName()
                      .replace("/", ".")
                      .replace(".class", "")
              )
          );
        }
      }

    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
    }

    return results;
  }
}
