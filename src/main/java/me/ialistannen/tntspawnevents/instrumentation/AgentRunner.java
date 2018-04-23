package me.ialistannen.tntspawnevents.instrumentation;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.ialistannen.tntspawnevents.libs.ExternalLibraryUtils;
import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.SystemUtils;

class AgentRunner {

  private static final Logger LOGGER = Logger.getLogger("AgentRunner");

  /**
   * Runs an agent and tried to also work around the Java 9 "allowAttachSelf" limitation.
   *
   * @param agentJar the path to the agent jar
   * @param pid the pid of the JVM to attach to
   * @param libsDir the path to the directly containing the external libs (e.g. libattach.1.8)
   * @param arguments the arguments for the agent
   */
  static void run(Path agentJar, int pid, Path libsDir, String arguments)
      throws IOException, AgentLoadException, AgentInitializationException,
      AttachNotSupportedException {

    if (Boolean.getBoolean("jdk.attach.allowAttachSelf")) {
      LOGGER.info(attachPrefix(
          "Picking self-attachment even if I am running on a newer JVM, as it was specified.."
      ));
      attachJavaToSameJVM(agentJar, pid, arguments);
      return;
    }

    if (!SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_9)) {
      LOGGER.info(attachPrefix("Attaching to same JVM..."));
      attachJavaToSameJVM(agentJar, pid, arguments);
      return;
    }

    LOGGER.info(attachPrefix("Attaching via an external runner..."));
    startNewJVMAndAttach(agentJar, pid, libsDir, arguments);
  }

  private static void attachJavaToSameJVM(Path agentJar, int pid, String arguments)
      throws AttachNotSupportedException, IOException, AgentLoadException,
      AgentInitializationException {

    VirtualMachine jvm = VirtualMachine.attach(Integer.toString(pid));
    jvm.loadAgent(agentJar.toAbsolutePath().toString(), arguments);
    jvm.detach();
  }

  private static void startNewJVMAndAttach(Path agentJar, int pid, Path libDir, String arguments)
      throws IOException {
    Path tempFile = Files.createTempFile("TNT-Spawn_external_attacher", ".jar");
    tempFile.toFile().deleteOnExit();

    JvmUtils.writeClassesToJar(
        tempFile,
        generateExternalAttacherManifest(),
        ExternalAgentAttacher.class, VirtualMachine.class, ExternalLibraryUtils.class
    );

    ProcessBuilder processBuilder = new ProcessBuilder(
        System.getProperty("java.home") + File.separator + "bin" + File.separator + "java",
        "-jar",
        tempFile.toAbsolutePath().toString(),
        Integer.toString(pid),
        agentJar.toAbsolutePath().toString(),
        libDir.toAbsolutePath().toString(),
        arguments
    );

    LOGGER.fine(attachPrefix("Running command '%s'", processBuilder.command()));

    Process process = processBuilder.start();

    new Thread(() -> {
      try {
        String error = new String(
            IOUtils.getAllBytes(process.getErrorStream()), StandardCharsets.UTF_8
        );
        String output = new String(
            IOUtils.getAllBytes(process.getInputStream()), StandardCharsets.UTF_8
        );

        process.waitFor();

        if (process.exitValue() != 0) {
          LOGGER.warning(attachPrefix("Attach process ended with a non-zero exit code."));
          LOGGER.warning(attachPrefix("The output stream was '%s'", output));
          LOGGER.warning(attachPrefix("The error stream was '%s'", error));
        }

      } catch (RuntimeException | InterruptedException e) {
        LOGGER.log(Level.WARNING, attachPrefix("Could not start the external agent"), e);
      }
    }).start();
  }

  private static Manifest generateExternalAttacherManifest() {
    Manifest manifest = new Manifest();
    Attributes mainAttributes = manifest.getMainAttributes();
    mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
    mainAttributes.put(
        Name.MAIN_CLASS,
        "me.ialistannen.tntspawnevents.instrumentation.ExternalAgentAttacher"
    );

    return manifest;
  }

  private static String attachPrefix(String message, Object... formatArgs) {
    return "[AgentRunner] " + String.format(message, formatArgs);
  }
}
