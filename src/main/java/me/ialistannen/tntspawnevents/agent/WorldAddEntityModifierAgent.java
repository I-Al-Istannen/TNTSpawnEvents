package me.ialistannen.tntspawnevents.agent;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.ref.WeakReference;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.bytecode.LineNumberAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;
import me.ialistannen.tntspawnevents.instrumentation.ClassUtils;

public class WorldAddEntityModifierAgent implements ClassFileTransformer {

  private static final Logger LOGGER = Logger.getLogger("TNTSpawnAgent");

  private Set<String> classesToTransform;
  private ClassPool classPool;
  private List<WeakReference<ClassLoader>> attachedClassLoaders;

  private WorldAddEntityModifierAgent(Collection<String> classesToTransform) {
    this.classesToTransform = new HashSet<>(classesToTransform);
    this.classPool = ClassPool.getDefault();
    this.attachedClassLoaders = new ArrayList<>();
  }

  public static void agentmain(String arguments, Instrumentation instrumentation) {
    LOGGER.info(addPrefix("Agent instantiated! Arguments: '" + arguments + "'"));

    Class<?> clazz = BukkitReflectionUtils.getClassUnchecked(arguments);

    if (!ClassUtils.canFindClassBytes(clazz)) {
      LOGGER.severe(addPrefix("Couldn't find class bytes for '%s', aborting.", clazz));
      return;
    }

    WorldAddEntityModifierAgent agent = new WorldAddEntityModifierAgent(
        Collections.singletonList(arguments.replace('.', '/'))
    );

    instrumentation.addTransformer(agent);

    try {
      instrumentation.redefineClasses(new ClassDefinition(
          clazz, ClassUtils.getClassFileBytes(clazz))
      );
    } catch (ClassNotFoundException | UnmodifiableClassException e) {
      LOGGER.log(Level.WARNING, addPrefix("An error occurred redefining the target class"), e);
    } finally {
      instrumentation.removeTransformer(agent);
    }
  }

  @Override
  public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
      ProtectionDomain protectionDomain, byte[] classfileBuffer) {

    if (!classesToTransform.contains(className)) {
      return null;
    }

    addClassloaderToPathIfNotPresent(loader);

    try {
      CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
      for (CtMethod ctMethod : ctClass.getMethods()) {
        if (!ctMethod.getName().equals("addEntity")) {
          continue;
        }
        if (ctMethod.getParameterTypes().length != 2) {
          continue;
        }

        MethodInfo methodInfo = ctMethod.getMethodInfo();
        LocalVariableAttribute table = (LocalVariableAttribute) methodInfo.getCodeAttribute()
            .getAttribute(LocalVariableAttribute.tag);

        LineNumberAttribute lineNumberAttribute = (LineNumberAttribute) methodInfo
            .getCodeAttribute()
            .getAttribute(LineNumberAttribute.tag);

        // this is some idiot code. The reason for it is that this code will run with the
        // application classloader, whereas our plugin is loaded with a JavaPluginClassLoader.
        // This means that if we directly reference *any* of our classes here, we well get a
        // different instance than what is used by the server, so we kindly need to ask Bukkit
        // to deal with that crap for us. This works because the "Bukkit" class is also loaded
        // by the application classloader and therefore we get served the same instance. Yay.
        String code = "if ($1 instanceof net.minecraft.server.v1_12_R1.EntityTNTPrimed) {"
            + "event = org.bukkit.Bukkit.getPluginManager().getPlugin(\"TNTSpawnEvents\")"
            + "            .getClass()"
            + "            .getMethod(\"callEvent\", new Class[]{net.minecraft.server.v1_12_R1.Entity.class})"
            + "            .invoke(null, new Object[]{$1});"
            + "}";

        int position = 0;
        for (int i = 0; i < table.tableLength(); i++) {
          String descriptor = table.descriptor(i);
          if (descriptor.contains("org/bukkit/event/Cancellable")) {
            position = table.startPc(i);
            break;
          }
        }
        ctMethod.insertAt(lineNumberAttribute.toLineNumber(position), code);

        break;
      }

      LOGGER.info(addPrefix("Redefined '%s'", className));

      return ctClass.toBytecode();
    } catch (Throwable e) {
      LOGGER.log(Level.WARNING, addPrefix("An error occurred transforming the target class"), e);
    }

    return null;
  }

  private void addClassloaderToPathIfNotPresent(ClassLoader classLoader) {
    for (WeakReference<ClassLoader> attachedClassLoader : attachedClassLoaders) {
      if (attachedClassLoader.get() == classLoader) {
        return;
      }
    }
    attachedClassLoaders.add(new WeakReference<>(classLoader));
    classPool.appendClassPath(new LoaderClassPath(classLoader));

    LOGGER.info(addPrefix(classLoader + " was added to the ClassPool"));

    cleanCollectedLoaders();
  }

  private void cleanCollectedLoaders() {
    attachedClassLoaders.removeIf(ref -> ref.get() == null);
  }

  private static String addPrefix(String message, Object... formatParameters) {
    return String.format("[TNTSpawnAgent] " + message, formatParameters);
  }
}
