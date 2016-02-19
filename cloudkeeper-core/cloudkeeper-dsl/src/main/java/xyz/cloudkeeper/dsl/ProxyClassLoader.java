package xyz.cloudkeeper.dsl;

import xyz.cloudkeeper.dsl.exception.AbstractMethodsException;
import xyz.cloudkeeper.dsl.exception.InvalidClassException;
import xyz.cloudkeeper.relocated.org.objectweb.asm.ClassReader;
import xyz.cloudkeeper.relocated.org.objectweb.asm.ClassVisitor;
import xyz.cloudkeeper.relocated.org.objectweb.asm.ClassWriter;
import xyz.cloudkeeper.relocated.org.objectweb.asm.MethodVisitor;
import xyz.cloudkeeper.relocated.org.objectweb.asm.Opcodes;
import xyz.cloudkeeper.relocated.org.objectweb.asm.Type;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class ProxyClassLoader extends ClassLoader {
    static final String PROXY_PREFIX = "$ModuleProxy.";

    private final Method proxyMethod;
    private final Map<String, Class<?>> dynamicClasses = new HashMap<>();

    /**
     * Creates a new proxy class loader using the specified parent class loader for delegation.
     *
     * @param parentClassLoader parent class loader
     * @param proxyMethod The {@link Method} that the dynamically generated implementation of any public abstract
     *     object-returning method will call. This {@link Method} must be a instance method taking a single
     *     {@link String} as argument and returning an {@link Object}. The argument when invoking the method will be the
     *     name of the method.
     * @throws NullPointerException if any argument is null
     */
    ProxyClassLoader(ClassLoader parentClassLoader, Method proxyMethod) {
        super(Objects.requireNonNull(parentClassLoader));
        this.proxyMethod = Objects.requireNonNull(proxyMethod);
    }

    static final class MethodIdentifier {
        private final String classInternalName;
        private final String methodName;
        private final String descriptor;

        MethodIdentifier(String classInternalName, String methodName, String descriptor) {
            this.classInternalName = classInternalName;
            this.methodName = methodName;
            this.descriptor = descriptor;
        }

        @Override
        public boolean equals(@Nullable Object otherObject) {
            if (this == otherObject) {
                return true;
            } else if (otherObject == null || getClass() != otherObject.getClass()) {
                return false;
            }

            MethodIdentifier other = (MethodIdentifier) otherObject;
            return classInternalName.equals(other.classInternalName)
                && methodName.equals(other.methodName)
                && descriptor.equals(other.descriptor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(classInternalName, methodName, descriptor);
        }
    }

    static String binaryToInternalName(String name) {
        return name.replace('.', '/');
    }

    final class ModuleClassVisitor extends ClassVisitor {
        private final ClassWriter classWriter;
        private final String subclassInternalName;
        private final String abstractClassInternalName;
        private final String visitedClassInternalName;
        private final Set<MethodIdentifier> relevantMethods;

        /**
         * See {@link Type#getInternalName(Class)} for an example of how to convert from binary name to internal name.
         *
         * @param classWriter the ASM class writer that this visitor will write to
         * @param subclassName binary name of the dynamically defined class
         * @param abstractClassName binary name of abstract module class
         * @param visitedClassName binary name of the super class (or interface) that contains relevant abstract
         *     methods
         * @param relevantMethods methods that need to be overridden in the dynamic class.
         */
        ModuleClassVisitor(ClassWriter classWriter, String subclassName, String abstractClassName,
            String visitedClassName, Set<MethodIdentifier> relevantMethods) {

            super(Opcodes.ASM4);
            this.classWriter = classWriter;
            this.subclassInternalName = binaryToInternalName(subclassName);
            this.abstractClassInternalName = binaryToInternalName(abstractClassName);
            this.visitedClassInternalName = binaryToInternalName(visitedClassName);
            this.relevantMethods = relevantMethods;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName,
            String[] interfaces) {

            // We must write the header information only once; when visiting the abstract module class.
            if (abstractClassInternalName.equals(name)) {
                // Define subclass as public and final. We simply reuse the signature (generic type information).
                classWriter.visit(
                    Opcodes.V1_7,
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                    subclassInternalName,
                    signature,
                    visitedClassInternalName,
                    null
                );

                // Create no-argument constructor
                MethodVisitor methodVisitor = classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
                methodVisitor.visitCode();
                // Push reference to self ("this") onto the operand stack
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                // Invoke super constructor
                methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, visitedClassInternalName, "<init>", "()V", false);
                methodVisitor.visitInsn(Opcodes.RETURN);
                // Set size of operand stack and number of local variables. Each argument for a method invocation needs
                // a local variable.
                methodVisitor.visitMaxs(1, 1);
                methodVisitor.visitEnd();
            }
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
            String[] exceptions) {

            if (
                name != null && descriptor != null
                && relevantMethods.contains(new MethodIdentifier(visitedClassInternalName, name, descriptor))
            ) {
                // Create method. Since we are overriding, we can reuse almost all arguments.
                MethodVisitor methodVisitor = classWriter.visitMethod(
                    Opcodes.ACC_PUBLIC, name, descriptor, signature, exceptions
                );
                methodVisitor.visitCode();
                // Push reference to self ("this") onto operand stack
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                // Push String containing the method name onto operand stack
                methodVisitor.visitLdcInsn(name);
                // Invoke proxy method. It is protected, so it may be called by the dynamic subclass.
                methodVisitor.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    subclassInternalName,
                    proxyMethod.getName(),
                    Type.getMethodDescriptor(proxyMethod),
                    false
                );
                // The previous method call push the result onto the operand stack.
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getReturnType(descriptor).getInternalName());
                // Now it is possible to return the
                // result. The opcode indicates we are returning an object (and not some primitive type).
                methodVisitor.visitInsn(Opcodes.ARETURN);
                // Set size of operand stack and number of local variables. Each argument for a method invocation
                // needs a local variable.
                methodVisitor.visitMaxs(2, 1);
                methodVisitor.visitEnd();
            }
            return null;
        }
    }

    private static InputStream getClassByteCodeStream(Class<?> clazz) throws IOException {
        String internalName = clazz.getName().replace('.', '/') + ".class";
        @Nullable InputStream inputStream = clazz.getClassLoader().getResourceAsStream(internalName);
        if (inputStream == null) {
            throw new IllegalStateException(String.format("Cannot find class file with name '%s'.", internalName));
        }
        return inputStream;
    }

    /**
     * Defines a proxy implementation for the given abstract class.
     *
     * This method has to be called before {@link #loadClass(String)} may be used. This method is idempotent in that
     * repeated calls to this method will not have an effect.
     *
     * @param abstractClass name of the abstract class
     * @throws IOException if instantiating a {@link ClassReader} for {@code abstractClass} fails
     */
    synchronized void defineProxyForClass(Class<?> abstractClass) throws IOException {
        final String abstractClassName = abstractClass.getName();
        final String proxyName = getProxyNameFromName(abstractClass.getName());

        // This method is idempotent: If a dynamically generated class already exists for the given abstract class, we
        // do not need to do anything.
        if (dynamicClasses.containsKey(proxyName)) {
            return;
        }

        int classModifiers = abstractClass.getModifiers();
        if (!Modifier.isPublic(classModifiers)
            || !Modifier.isAbstract(classModifiers)
            || (abstractClass.isMemberClass() && !Modifier.isStatic(abstractClass.getModifiers()))
            || abstractClass.isAnonymousClass()
            || abstractClass.isLocalClass()) {

            throw new InvalidClassException(String.format(
                "Expected class with modifiers public and abstract. If class is a member class, also expected "
                    + "modifier static. However, got %s.", abstractClass
            ));
        }

        // Determine all public abstract methods that return a subclass of the proxy method. Group these methods by
        // the declaring class (or interface).
        Map<Class<?>, Set<MethodIdentifier>> relevantMethods = new LinkedHashMap<>();
        // Make sure that the abstract class comes first in the iteration order of relevantMethods. From the
        // LinkedHashMap documentation: "insertion order is not affected if a key is re-inserted into the map"
        relevantMethods.put(abstractClass, new HashSet<MethodIdentifier>());
        Method[] methods = abstractClass.getMethods();
        for (Method method: methods) {
            // getMethods() only return public methods.
            int modifiers = method.getModifiers();
            if (
                proxyMethod.getReturnType().isAssignableFrom(method.getReturnType())
                && Modifier.isAbstract(modifiers)
            ) {
                Class<?> declaringClass = method.getDeclaringClass();
                Set<MethodIdentifier> relevantMethodsForClass = relevantMethods.get(declaringClass);
                if (relevantMethodsForClass == null) {
                    relevantMethodsForClass = new HashSet<>();
                    relevantMethods.put(declaringClass, relevantMethodsForClass);
                }
                relevantMethodsForClass.add(new MethodIdentifier(
                    binaryToInternalName(declaringClass.getName()),
                    method.getName(),
                    Type.getMethodDescriptor(method)
                ));
            } else if (Modifier.isAbstract(modifiers)) {
                // Note that this does not catch non-public abstract methods!
                // TODO: Write utility function to determine source-code line of class
                throw new AbstractMethodsException(abstractClassName, method.getName(), null);
            }
        }

        ClassWriter classWriter = new ClassWriter(0);
        for (Map.Entry<Class<?>, Set<MethodIdentifier>> entry: relevantMethods.entrySet()) {
            Class<?> clazz = entry.getKey();
            final ClassReader classReader;
            try (InputStream classByteCodeStream = getClassByteCodeStream(clazz)) {
                classReader = new ClassReader(classByteCodeStream);
            }
            String superclassName = clazz.getName();
            ModuleClassVisitor classVisitor
                = new ModuleClassVisitor(classWriter, proxyName, abstractClassName, superclassName, entry.getValue());
            classReader.accept(classVisitor, ClassReader.SKIP_CODE);
        }

        classWriter.visitEnd();
        byte[] bytes = classWriter.toByteArray();

        dynamicClasses.put(proxyName, defineClass(proxyName, bytes, 0, bytes.length));
    }

    @Override
    protected synchronized Class<?> findClass(String name) throws ClassNotFoundException {
        if (name.startsWith(PROXY_PREFIX)) {
            Class<?> clazz = dynamicClasses.get(name);
            if (clazz == null) {
                // defineProxyForClass() needs to be called first before this method can succeed!
                throw new ClassNotFoundException(String.format("Could not find dynamically generated class %s "
                    + "(for abstract class %s).", name, getNameFromProxyName(name)));
            }
            return clazz;
        }
        return super.findClass(name);
    }

    static String getProxyNameFromName(String className) {
        return PROXY_PREFIX + className;
    }

    static String getNameFromProxyName(String proxyName) {
        if (!proxyName.startsWith(PROXY_PREFIX)) {
            throw new IllegalArgumentException(String.format("Proxy name does not start with \"%s\".", PROXY_PREFIX));
        }
        return proxyName.substring(PROXY_PREFIX.length());
    }
}
