package me.ialistannen.tntspawnevents;

import me.ialistannen.tntspawnevents.agent.BukkitReflectionUtils;
import me.ialistannen.tntspawnevents.agent.MyAgent;
import me.ialistannen.tntspawnevents.instrumentation.IOUtils;
import me.ialistannen.tntspawnevents.instrumentation.JvmUtils;
import me.ialistannen.tntspawnevents.instrumentation.Utils;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class TNTSpawnEvents extends JavaPlugin {

  @Override
  public void onEnable() {
    int pid = Utils.getPid();

    new BukkitRunnable() {
      @Override
      public void run() {
        JvmUtils.attachToJvm(
            pid,
            MyAgent.class,
//        "net.minecraft.server.v1_12_R1.World",
            "me.ialistannen.tntspawnevents.TNTSpawnEvents",
            MyAgent.class, BukkitReflectionUtils.class, Utils.class, IOUtils.class
        );

      }
    }.runTaskLater(this, 20);
  }
}
