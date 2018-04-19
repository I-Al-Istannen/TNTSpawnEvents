package me.ialistannen.tntspawnevents;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ExampleListener implements Listener {


  @EventHandler
  public void onCoolEvent(PrimedTntSpawnEvent e) {
    for (Player player : Bukkit.getOnlinePlayers()) {
      player.sendMessage(ChatColor.RED + "Not on my watch!");
    }

    e.setCancelled(true);
  }
}