package me.ialistannen.tntspawnevents.instrumentation;

import com.sun.tools.attach.VirtualMachine;
import java.util.Arrays;

/**
 * Attaches an agent, but can be invoked by another JVM.
 *
 * First parameter is the pid, second the path to the agent jar and the last may be an argument for
 * the agent.
 */
public class ExternalAgentAttacher {

  public static void main(String[] args) throws Exception {
    System.out.println("Called with '" + Arrays.toString(args) + "'");
    if (args.length < 2) {
      throw new IllegalArgumentException("Not enough arguments received.");
    }

    String pid = args[0];
    String pathToAgent = args[1];
    String argument = args.length > 2 ? args[2] : "";

    VirtualMachine machine = VirtualMachine.attach(pid);
    machine.loadAgent(pathToAgent, argument);
    machine.detach();
  }

}
