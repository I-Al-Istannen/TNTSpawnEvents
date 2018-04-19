package me.ialistannen.tntspawnevents;

import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PrimedTntSpawnEvent extends Event implements Cancellable {

  private static final HandlerList handlers = new HandlerList();

  private boolean cancelled;
  private TNTPrimed tnt;

  public PrimedTntSpawnEvent(TNTPrimed tnt) {
    this.cancelled = false;
    this.tnt = tnt;
  }

  public TNTPrimed getTnt() {
    return tnt;
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public void setCancelled(boolean cancel) {
    this.cancelled = cancel;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  @SuppressWarnings("unused") // mandated by bukkit
  public static HandlerList getHandlerList() {
    return handlers;
  }
}
