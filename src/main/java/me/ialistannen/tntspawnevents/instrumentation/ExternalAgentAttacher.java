package me.ialistannen.tntspawnevents.instrumentation;

import com.sun.tools.attach.VirtualMachine;
import java.nio.file.Paths;
import java.util.Arrays;
import me.ialistannen.tntspawnevents.libs.ExternalLibraryUtils;

/**
 * Attaches an agent, but can be invoked by another JVM.
 * <p>
 * <br><strong>Arguments: </strong>
 * <ol>
 *   <li>PID</li>
 *   <li>Path to agent</li>
 *   <li>Path to libs</li>
 *   <li>[argument]</li>
 * </ol>
 */
public class ExternalAgentAttacher {

  public static void main(String[] args) throws Exception {
    System.out.println("Called with '" + Arrays.toString(args) + "'");
    if (args.length < 3) {
      throw new IllegalArgumentException("Not enough arguments received.");
    }

    String pid = args[0];
    String pathToAgent = args[1];
    String pathToLibs = args[2];
    String argument = args.length > 3 ? args[3] : "";

    ExternalLibraryUtils.addLibrariesToPath(Paths.get(pathToLibs));

    VirtualMachine machine = VirtualMachine.attach(pid);
    machine.loadAgent(pathToAgent, argument);
    machine.detach();
  }

}
