package me.ialistannen.tntspawnevents.instrumentation;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.bukkit.Bukkit;

public class JvmUtils {

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

      writeClassesToJar(agentJar, manifest, agentClasses);

      VirtualMachine jvm = VirtualMachine.attach(Integer.toString(pid));
      jvm.loadAgent(agentJar.toAbsolutePath().toString(), arguments);
      jvm.detach();

//      Files.deleteIfExists(agentJar);

    } catch (IOException | AttachNotSupportedException | AgentInitializationException
        | AgentLoadException e) {
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
//    mainAttributes.put(
//        new Attributes.Name("Boot-Class-Path"),
//        codeSourceAsPath(agentClass) + " " + codeSourceAsPath(Bukkit.class)
//    );

//    System.out.println(codeSourceAsPath(agentClass) + " " + codeSourceAsPath(Bukkit.class));

    return manifest;
  }

  private static String codeSourceAsPath(Class<?> clazz) {
    return clazz.getProtectionDomain().getCodeSource().getLocation().toString()
        .replace("file:", "");
  }

  private static void writeClassesToJar(Path jarFile, Manifest jarManifest, Class<?>... classes) {
    try (OutputStream outputStream = Files.newOutputStream(jarFile);
        JarOutputStream jarOutputStream = new JarOutputStream(outputStream, jarManifest)) {

      for (Class<?> aClass : classes) {
        String resourceName = Utils.getResourceName(aClass);
        jarOutputStream.putNextEntry(new JarEntry(resourceName));
        jarOutputStream.write(Utils.getClassFileBytes(aClass));
        jarOutputStream.closeEntry();
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
