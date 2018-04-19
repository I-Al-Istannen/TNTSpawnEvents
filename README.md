# About

This plugin is a small POC to add an event that is triggered when primed TNT is spawned.

# How it works

This plugin does a few things:

1. Attach a javaagent to the JVM running the server
2. Use that agent to request a redefinition of the NMS World class, as that is where entities are added
3. Modify the bytecode of that class using javassist and inject some code. To understand what exactly, have
   a look at what the method in question does normally:

   ```java
   public boolean addEntity(Entity entity, SpawnReason spawnReason) {
     if (entity == null) {
       return false;
     } else {
       Cancellable event = null;
       boolean flag;
       if (entity instanceof EntityLiving) {}
       if (entity instanceof EntityItem) {}
       // and so on

       if (event == null || !event.isCancelled()) {
         // add entity
       }
    }
   ```

   It now injects a single new `if` after the `Cancellabl event` line, that checks for `EntityTNTPrimed` and,
   if it finds it, calls the custom event. This requires some trickery, as the classloader used for the Event
   and plugin *differs* from the one running the NMS code. This means that two versions of the Event class could
   be instantiated, which means the handlerlist could differ. Unfortunately that means no event listeners will
   be registered either, so it effectively useless. To circumvent that we let *Bukkit* load the class and fetch
   the plugin for us — using that we get a class from our plugin class loader back. How neat!

4. So now our event is properly injected, called, actually distributed to listeners and handled in the same
   way as any other event. Life's good.

# Caveats

* `libattach.so` needs to be preset. This is bundled with the JDK, ideally it would be extraced from the jar, but it is platform specific.
