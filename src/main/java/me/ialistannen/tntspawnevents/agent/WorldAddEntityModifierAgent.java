package me.ialistannen.tntspawnevents.agent;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.LineNumberAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;
import me.ialistannen.tntspawnevents.instrumentation.ClassUtils;

public class WorldAddEntityModifierAgent implements ClassFileTransformer {

  private Set<String> classesToTransform;
  private ClassPool classPool;

  private WorldAddEntityModifierAgent(Collection<String> classesToTransform) {
    this.classesToTransform = new HashSet<>(classesToTransform);
    this.classPool = ClassPool.getDefault();
  }

  public static void agentmain(String arguments, Instrumentation instrumentation) {
    System.out.println("Agent instantiated! with arguments: '" + arguments + "'");

    List<Class<?>> classes = Arrays.stream(arguments.split(","))
        .map(BukkitReflectionUtils::getClassUnchecked)
        .filter(Objects::nonNull)
        .filter(ClassUtils::canFindClassBytes)
        .collect(Collectors.toList());

    WorldAddEntityModifierAgent agent = new WorldAddEntityModifierAgent(
        classes.stream().map(Class::getName)
            .map(s -> s.replace('.', '/'))
            .collect(Collectors.toList())
    );

    instrumentation.addTransformer(agent);

    try {
      for (Class<?> aClass : classes) {
        instrumentation.redefineClasses(new ClassDefinition(
            aClass, ClassUtils.getClassFileBytes(aClass)
        ));
      }
    } catch (ClassNotFoundException | UnmodifiableClassException e) {
      e.printStackTrace();
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

      return ctClass.toBytecode();
    } catch (Throwable e) {
      e.printStackTrace();
    }

    return null;
  }
}
