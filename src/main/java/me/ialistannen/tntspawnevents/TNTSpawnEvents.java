package me.ialistannen.tntspawnevents;

import java.util.Arrays;
import me.ialistannen.tntspawnevents.agent.BukkitReflectionUtils;
import me.ialistannen.tntspawnevents.agent.WorldAddEntityModifierAgent;
import me.ialistannen.tntspawnevents.instrumentation.ClassUtils;
import me.ialistannen.tntspawnevents.instrumentation.IOUtils;
import me.ialistannen.tntspawnevents.instrumentation.JvmUtils;
import me.ialistannen.tntspawnevents.libs.ExternalLibrary;
import me.ialistannen.tntspawnevents.libs.ExternalLibraryUtils;
import net.minecraft.server.v1_12_R1.Entity;
import org.bukkit.Bukkit;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.plugin.java.JavaPlugin;

public final class TNTSpawnEvents extends JavaPlugin {

  @Override
  public void onEnable() {
    int pid = JvmUtils.getPid();

    getServer().getPluginManager().registerEvents(new ExampleListener(), TNTSpawnEvents.this);

    ExternalLibraryUtils.addLibrariesToPath(
        ExternalLibraryUtils.unpackLibraries(Arrays.asList(ExternalLibrary.values()))
    );

    JvmUtils.attachToJvm(
        pid,
        WorldAddEntityModifierAgent.class,
        "net.minecraft.server.v1_12_R1.World",
        WorldAddEntityModifierAgent.class, BukkitReflectionUtils.class, ClassUtils.class,
        IOUtils.class,
        PrimedTntSpawnEvent.class, TNTSpawnEvents.class
    );
  }

  /**
   * Calls the tnt spawn event for the given entity.
   *
   * @param entity the tnt entity
   * @return the created and called {@link PrimedTntSpawnEvent}
   */
  @SuppressWarnings("unused") // called by javassisted bytecode
  public static PrimedTntSpawnEvent callEvent(Entity entity) {
    try {
      PrimedTntSpawnEvent event = new PrimedTntSpawnEvent((TNTPrimed) entity.getBukkitEntity());
      Bukkit.getPluginManager().callEvent(event);

      return event;
    } catch (Throwable e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

}
