package me.ialistannen.tntspawnevents.agent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
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
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import me.ialistannen.tntspawnevents.instrumentation.Utils;

public class MyAgent implements ClassFileTransformer {

  private static Instrumentation instrumentation;
  private static MyAgent agent;
  private Set<String> classesToTransform;

  private MyAgent(Collection<String> classesToTransform) {
    this.classesToTransform = new HashSet<>(classesToTransform);
  }

  public static void agentmain(String arguments, Instrumentation ins) {
    System.out.println("Agent instantiated!");

    List<Class<?>> classes = Arrays.stream(arguments.split(","))
        .map(BukkitReflectionUtils::getNMSClass)
        .filter(Objects::nonNull)
        .filter(Utils::canFindClassBytes)
        .collect(Collectors.toList());

    agent = new MyAgent(
        classes.stream().map(Class::getName)
            .map(s -> s.replace('.', '/'))
            .collect(Collectors.toList())
    );
    instrumentation = ins;

    ins.addTransformer(agent);

    try {
      ClassDefinition[] definitions = classes.stream()
          .map(c -> new ClassDefinition(c, Utils.getClassFileBytes(c)))
          .toArray(ClassDefinition[]::new);

      System.out.println("Definitions: " + Arrays.toString(definitions));
//
      instrumentation.redefineClasses(definitions);
    } catch (UnmodifiableClassException | ClassNotFoundException e) {
      e.printStackTrace();
    } finally {
      instrumentation.removeTransformer(agent);
    }
  }

  @Override
  public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
      ProtectionDomain protectionDomain, byte[] classfileBuffer)
      throws IllegalClassFormatException {

    System.out.println("Transforming stuff: " + className);

    if (!classesToTransform.contains(className)) {
      return null;
    }

    ClassPool classPool = new ClassPool();
    try {
      CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
      CtMethod addEntity = ctClass.getDeclaredMethod("addEntity");

      System.out.println("ADDED: " + addEntity);

    } catch (IOException | NotFoundException e) {
      e.printStackTrace();
    }

    return null;
  }
}
