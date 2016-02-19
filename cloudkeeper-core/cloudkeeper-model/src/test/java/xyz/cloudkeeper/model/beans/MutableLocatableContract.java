package xyz.cloudkeeper.model.beans;

import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.testng.Assert;
import org.testng.ITest;
import org.testng.annotations.Test;
import xyz.cloudkeeper.model.Immutable;
import xyz.cloudkeeper.model.bare.BareLocatable;
import xyz.cloudkeeper.model.beans.element.MutableAnnotatedConstruct;
import xyz.cloudkeeper.model.beans.element.MutableQualifiedNamable;
import xyz.cloudkeeper.model.beans.element.MutableSimpleNameable;
import xyz.cloudkeeper.model.immutable.AnnotationValue;
import xyz.cloudkeeper.model.immutable.Location;
import xyz.cloudkeeper.model.immutable.element.Key;
import xyz.cloudkeeper.model.immutable.element.Name;
import xyz.cloudkeeper.model.immutable.element.NoKey;
import xyz.cloudkeeper.model.immutable.element.SimpleName;
import xyz.cloudkeeper.model.immutable.element.Version;
import xyz.cloudkeeper.model.immutable.execution.ExecutionTrace;
import xyz.cloudkeeper.model.util.ImmutableList;

import javax.annotation.Nullable;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.regex.Pattern;

public class MutableLocatableContract implements ITest {
    private static final CopyOption[] NO_COPY_OPTIONS = new CopyOption[0];

    private final Class<?> clazz;
    private final ImmutableList<GetterSetterPair> getterSetterPairs;
    private MutableLocatable<?> instance;
    private MutableLocatable<?> copy;
    private Method staticCopyMethod;

    @SafeVarargs
    public static MutableLocatableContract[] contractsFor(Class<? extends MutableLocatable<?>>... classes) {
        MutableLocatableContract[] array = new MutableLocatableContract[classes.length];
        for (int i = 0; i < classes.length; ++i) {
            array[i] = new MutableLocatableContract(classes[i]);
        }
        return array;
    }

    public MutableLocatableContract(Class<? extends MutableLocatable<?>> clazz) {
        this.clazz = clazz;

        List<GetterSetterPair> newGetterSetterPairs = new ArrayList<>();
        for (Method method: clazz.getMethods()) {
            try {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (method.getName().startsWith("set") && parameterTypes.length == 1) {
                    Class<?> expectedReturnType = parameterTypes[0];
                    Method getter = Boolean.class.equals(expectedReturnType) || boolean.class.equals(expectedReturnType)
                        ? clazz.getMethod("is" + method.getName().substring(3))
                        : clazz.getMethod("get" + method.getName().substring(3));

                    Class<?> getterReturnType = getter.getReturnType();
                    if (getterReturnType.equals(parameterTypes[0])) {
                        newGetterSetterPairs.add(new GetterSetterPair(getter, method));
                    }
                }
            } catch (NoSuchMethodException exception) {
                Assert.fail(String.format(
                    "Could not find getter corresponding to setter method %s.", method
                ), exception);
            }
        }
        getterSetterPairs = ImmutableList.copyOf(newGetterSetterPairs);
    }

    @Override
    public String getTestName() {
        return clazz.getSimpleName();
    }

    @Test
    public void noArgsConstructor() throws ReflectiveOperationException {
        instance = (MutableLocatable<?>) clazz.getConstructor().newInstance();
    }

    private static MutableLocatable<?> valueForMutableLocatable(Class<?> clazz) throws Exception {
        Class<?> currentClass = clazz;
        while (true) {
            if (Modifier.isAbstract(currentClass.getModifiers())) {
                XmlSeeAlso annotation = currentClass.getAnnotation(XmlSeeAlso.class);
                Assert.assertNotNull(annotation, String.format("Missing @%s annotation on %s.",
                    XmlSeeAlso.class.getSimpleName(), currentClass));
                currentClass = (Class<?>) annotation.value()[0];
            } else {
                return (MutableLocatable<?>) currentClass.getConstructor().newInstance();
            }
        }
    }

    private static final class ClassAndValue {
        private final Class<?> clazz;
        private final Object value;
        private final Object equalsValue;

        private ClassAndValue(Class<?> clazz, Object value, Object equalsValue) {
            this.clazz = clazz;
            this.value = value;
            this.equalsValue = equalsValue;
        }

        private ClassAndValue(Class<?> clazz, Object value) {
            this(clazz, value, value);
        }
    }

    private static ClassAndValue valueForType(Class<?> rawClass, Type generic) throws Exception {
        if (List.class.equals(rawClass)) {
            Type listPropertyElementType = ((ParameterizedType) generic).getActualTypeArguments()[0];
            Class<?> listPropertyElementRaw;
            if (listPropertyElementType instanceof Class<?>) {
                listPropertyElementRaw = (Class<?>) listPropertyElementType;
            } else if (listPropertyElementType instanceof ParameterizedType) {
                listPropertyElementRaw = (Class<?>) ((ParameterizedType) listPropertyElementType).getRawType();
            } else {
                Assert.fail(String.format("Expected generic type that is %s or %s, but got %s.",
                    Class.class.getSimpleName(), ParameterizedType.class.getSimpleName(), listPropertyElementType));
                throw new AssertionError();
            }
            return new ClassAndValue(
                List.class,
                Collections.singletonList(valueForType(listPropertyElementRaw, listPropertyElementType).value)
            );
        } else if (rawClass.isEnum()) {
            return new ClassAndValue(rawClass, rawClass.getEnumConstants()[0]);
        } else if (Location.class.equals(rawClass)) {
            return new ClassAndValue(rawClass, new Location("foo", 3, 24));
        } else if (SimpleName.class.equals(rawClass)) {
            return new ClassAndValue(String.class, "Bar", SimpleName.identifier("Bar"));
        } else if (Name.class.equals(rawClass)) {
            return new ClassAndValue(String.class, "com.foo.Bar", Name.qualifiedName("com.foo.Bar"));
        } else if (Key.class.equals(rawClass)) {
            return new ClassAndValue(Key.class, NoKey.instance());
        } else if (Version.class.equals(rawClass)) {
            return new ClassAndValue(rawClass, Version.valueOf("1.2.3"));
        } else if (AnnotationValue.class.equals(rawClass)) {
            return new ClassAndValue(Serializable.class, 34, AnnotationValue.of(34));
        } else if (ExecutionTrace.class.equals(rawClass)) {
            return new ClassAndValue(rawClass, ExecutionTrace.valueOf("/loop/value:in:num"));
        } else if (MutableQualifiedNamable.class.equals(rawClass)) {
            return new ClassAndValue(String.class, "com.foo.Bar",
                new MutableQualifiedNamable().setQualifiedName("com.foo.Bar"));
        } else if (MutableSimpleNameable.class.equals(rawClass)) {
            return new ClassAndValue(String.class, "Bar", new MutableSimpleNameable().setSimpleName("Bar"));
        } else if (MutableLocatable.class.isAssignableFrom(rawClass)) {
            return new ClassAndValue(rawClass, valueForMutableLocatable(rawClass));
        } else if (URI.class.equals(rawClass)) {
            return new ClassAndValue(rawClass, URI.create("test:some.module"));
        } else if (Pattern.class.equals(rawClass)) {
            return new ClassAndValue(rawClass, Pattern.compile(".*"));
        } else if (Date.class.equals(rawClass)) {
            return new ClassAndValue(rawClass, new Date());
        } else if (String.class.equals(rawClass)) {
            return new ClassAndValue(rawClass, "baz");
        } else if (byte[].class.equals(rawClass)) {
            return new ClassAndValue(rawClass, new byte[] { (byte) 1, (byte) 2 });
        } else if (Object.class.equals(rawClass)) {
            return new ClassAndValue(rawClass, new Object());
        } else {
            // No other type is currently supported.
            Assert.fail(String.format("Unexpected property type %s.", generic));
            throw new AssertionError();
        }
    }

    @Test(dependsOnMethods = "noArgsConstructor")
    public void setters() throws Exception {
        for (GetterSetterPair getterSetterPair: getterSetterPairs) {
            Method getter = getterSetterPair.getter;
            Method setter = getterSetterPair.setter;
            Class<?> propertyClass = setter.getParameterTypes()[0];
            Type propertyType = setter.getGenericParameterTypes()[0];

            // Verify that null is a valid argument for list-property setters if and only if a property is initialized
            // as null by the default constructor
            if (List.class.isAssignableFrom(propertyClass)) {
                if (getter.invoke(instance) == null) {
                    setter.invoke(instance, new Object[] { null });
                    Assert.assertNull(getter.invoke(instance));
                } else {
                    try {
                        setter.invoke(instance, new Object[] { null });
                        Assert.fail("Expected exception.");
                    } catch (InvocationTargetException exception) {
                        Assert.assertTrue(exception.getCause() instanceof NullPointerException);
                    }
                }
            }

            // Verify that calling setter works
            ClassAndValue classAndValue = valueForType(propertyClass, propertyType);
            clazz.getMethod(setter.getName(), classAndValue.clazz).invoke(instance, classAndValue.value);

            // Verify that getter now returns the previously updated property
            Object newValue = getter.invoke(instance);
            Assert.assertEquals(newValue, classAndValue.equalsValue);
        }
    }

    private static Class<?> getBareInterface(Class<?> clazz) {
        for (Constructor<?> constructor: clazz.getDeclaredConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length == 2 && BareLocatable.class.isAssignableFrom(parameterTypes[0])) {
                Class<?> bareInterface = parameterTypes[0];
                Assert.assertTrue(bareInterface.getSimpleName().startsWith("Bare"));
                return parameterTypes[0];
            }
        }
        Assert.fail(String.format("Could not find copy constructor for %s.", clazz));
        throw new AssertionError();
    }

    @Test(dependsOnMethods = "setters")
    public void copyOf() throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class<?> bareInterface = getBareInterface(clazz);
        String simpleName = bareInterface.getSimpleName();

        Class<?> superclass = clazz.getSuperclass();
        String copyOfName = superclass.equals(MutableLocatable.class)
                || superclass.equals(MutableAnnotatedConstruct.class)
            ? "copyOf"
            : "copyOf" + simpleName.substring("Bare".length());
        staticCopyMethod = clazz.getMethod(copyOfName, bareInterface, CopyOption[].class);

        // Verify that calling copyOf method with null returns null
        Assert.assertNull(staticCopyMethod.invoke(null, null, NO_COPY_OPTIONS));

        copy = (MutableLocatable<?>) staticCopyMethod.invoke(null, instance, NO_COPY_OPTIONS);
    }

    @Test(dependsOnMethods = "copyOf")
    public void testEquals() {
        Assert.assertEquals(instance, instance);
        Assert.assertEquals(copy, instance);
        Assert.assertEquals(instance, copy);
        Assert.assertFalse(instance.equals(null));
        Assert.assertFalse(instance.equals(new Object()));
        Assert.assertEquals(copy.toString(), instance.toString());
    }

    private static boolean hasVisitorMethod(Class<?> clazz) {
        for (Method method: clazz.getDeclaredMethods()) {
            if (method.getName().startsWith("accept")) {
                return true;
            }
        }
        return false;
    }

    @Test(dependsOnMethods = "testEquals")
    public void inheritedCopyOf()
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class<?> currentClass = clazz.getSuperclass();
        while (MutableLocatable.class.isAssignableFrom(currentClass) && !currentClass.equals(MutableLocatable.class)
                && !currentClass.equals(MutableAnnotatedConstruct.class)) {
            Class<?> bareInterface = getBareInterface(currentClass);
            if (hasVisitorMethod(bareInterface)) {
                String abstractCopyOfName = "copyOf" + currentClass.getSimpleName().substring("Mutable".length());
                Method abstractCopyMethod
                    = currentClass.getMethod(abstractCopyOfName, bareInterface, CopyOption[].class);
                MutableLocatable<?> abstractCopy
                    = (MutableLocatable<?>) abstractCopyMethod.invoke(null, instance, NO_COPY_OPTIONS);
                Assert.assertEquals(abstractCopy, instance);
            }
            currentClass = currentClass.getSuperclass();
        }
    }

    /**
     * Verifies that the copy method produces an object that does not share any transitive references to mutable
     * objects.
     */
    @Test(dependsOnMethods = "copyOf")
    public void emptyObjectGraphIntersection() {
        List<Object> duplicates = computeObjectGraphIntersection(instance, copy);
        Assert.assertTrue(duplicates.isEmpty(), "Shared object references found.");
    }

    private static final class GetterSetterPair {
        private final Method getter;
        private final Method setter;

        private GetterSetterPair(Method getter, Method setter) {
            this.getter = getter;
            this.setter = setter;
        }
    }

    private void restoreNonSerializableProperties(MutableLocatable<?> deserialized, MutableLocatable<?> original)
            throws IllegalAccessException, InvocationTargetException {
        // Verify that properties of type Object have not been restored. Instead, restore manually.
        for (GetterSetterPair getterSetterPair: getterSetterPairs) {
            Method getter = getterSetterPair.getter;
            Method setter = getterSetterPair.setter;

            if (getter.getReturnType().equals(Object.class)) {
                Assert.assertNull(getter.invoke(deserialized));
                setter.invoke(deserialized, getter.invoke(original));
            }
        }
    }

    /**
     * Verifies that Java serialization and deserialization works and gives equal results.
     */
    @Test(dependsOnMethods = "setters")
    public void serialization() throws Exception {
        byte[] bytes;
        try (
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)
        ) {
            objectOutputStream.writeObject(instance);
            objectOutputStream.close();
            bytes = byteArrayOutputStream.toByteArray();
        }

        MutableLocatable<?> reconstructed;
        try (
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)
        ) {
            reconstructed = (MutableLocatable<?>) objectInputStream.readObject();
        }

        restoreNonSerializableProperties(reconstructed, instance);
        Assert.assertEquals(reconstructed, instance);
        Assert.assertEquals(reconstructed.hashCode(), instance.hashCode());
    }

    @SuppressWarnings("unchecked")
    private static <T> Object uncheckedMarshal(MutableLocatable<?> object, XmlAdapter<?, T> xmlAdapter)
            throws Exception {
        return xmlAdapter.marshal((T) object);
    }

    @SuppressWarnings("unchecked")
    private static <T> JAXBElement<T> uncheckedNewJAXBElement(Class<T> clazz, Object value) {
        return new JAXBElement<>(new QName("xml-test"), clazz, (T) value);
    }

    @SuppressWarnings("unchecked")
    private static <T> MutableLocatable<?> uncheckedUnmarshal(Object object, XmlAdapter<T, ?> xmlAdapter)
            throws Exception {
        return (MutableLocatable<?>) xmlAdapter.unmarshal((T) object);
    }

    @Test(dependsOnMethods = "setters")
    public void xmlSerialization() throws Exception {
        MutableLocatable<?> copyWithoutLocations = (MutableLocatable<?>) staticCopyMethod.invoke(
            null, instance, new CopyOption[] { StandardCopyOption.STRIP_LOCATION });

        Object objectToSerialize = copyWithoutLocations;
        Class<?> classToBind = copyWithoutLocations.getClass();
        @Nullable XmlAdapter<?, ?> xmlAdapter = null;
        if (!clazz.isAnnotationPresent(XmlRootElement.class)) {
            Class<?> currentClass = clazz;
            while (MutableLocatable.class.isAssignableFrom(currentClass)
                    && !MutableLocatable.class.equals(currentClass)
                    && !MutableAnnotatedConstruct.class.equals(currentClass)) {
                @Nullable XmlJavaTypeAdapter typeAdapter = currentClass.getAnnotation(XmlJavaTypeAdapter.class);
                if (typeAdapter != null) {
                    Class<?> typeAdapterClass = typeAdapter.value();
                    Constructor<?> typeAdapterConstructor = typeAdapterClass.getDeclaredConstructor();
                    typeAdapterConstructor.setAccessible(true);
                    xmlAdapter = (XmlAdapter<?, ?>) typeAdapterConstructor.newInstance();
                    Object xmlAdaptedObject = uncheckedMarshal(copyWithoutLocations, xmlAdapter);
                    classToBind = xmlAdaptedObject.getClass();
                    objectToSerialize = uncheckedNewJAXBElement(classToBind, xmlAdaptedObject);
                    break;
                }
                currentClass = currentClass.getSuperclass();
            }
            if (xmlAdapter == null) {
                objectToSerialize = uncheckedNewJAXBElement(classToBind, copyWithoutLocations);
            }
        }

        // We use EclipseLink MOXy for marshalling, and the JAXB Reference Implementation for unmarshalling
        JAXBContext moxyContext = JAXBContextFactory.createContext(new Class<?>[]{ classToBind }, null);
        Marshaller marshaller = moxyContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        JAXBContext internalJaxbContext = JAXBContext.newInstance(classToBind);
        Unmarshaller unmarshaller = internalJaxbContext.createUnmarshaller();

        StringWriter stringWriter = new StringWriter();
        marshaller.marshal(objectToSerialize, stringWriter);
        Object deserializedObject;
        try (StringReader stringReader = new StringReader(stringWriter.toString())) {
            deserializedObject = unmarshaller.unmarshal(new StreamSource(stringReader), classToBind).getValue();
        }
        MutableLocatable<?> deserialized = xmlAdapter == null
            ? (MutableLocatable<?>) deserializedObject
            : uncheckedUnmarshal(deserializedObject, xmlAdapter);

        restoreNonSerializableProperties(deserialized, instance);
        Assert.assertEquals(deserialized, copyWithoutLocations);
        Assert.assertEquals(deserialized.hashCode(), copyWithoutLocations.hashCode());
    }

    @Test(dependsOnMethods = "setters")
    public void visitors() throws IllegalAccessException, InvocationTargetException {
        final Object argument = new Object();
        for (Method method: clazz.getMethods()) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (method.getName().startsWith("accept") && parameterTypes.length == 2
                    && parameterTypes[0].getSimpleName().endsWith("Visitor")) {
                Class<?> visitorClass = parameterTypes[0];
                InvocationHandler invocationHandler = (proxy, method1, args) -> {
                    if (method1.getName().startsWith("visit")) {
                        Assert.assertSame(args[0], instance);
                        Assert.assertSame(args[1], argument);
                        return method1.getParameterTypes()[0];
                    } else {
                        throw new UnsupportedOperationException(String.format(
                            "Unexpected call to method %s.", method1
                        ));
                    }
                };
                Object visitor = Proxy.newProxyInstance(
                    visitorClass.getClassLoader(), new Class<?>[]{ visitorClass }, invocationHandler);
                Class<?> result = (Class<?>) method.invoke(instance, visitor, argument);
                Assert.assertTrue(result.isInstance(instance));
            }
        }
    }


    private static final class NullOutputStream extends OutputStream {
        @Override
        public void write(int theByte) { }

        @Override
        public void write(byte[] bytes, int off, int len) { }
    }

    private static final class CheckingObjectOutputStream extends ObjectOutputStream {
        private enum Dummy {
            INSTANCE
        }

        private final boolean firstRun;
        private final IdentityHashMap<Object, Boolean> identityHashMap;
        @Nullable private final IdentityHashMap<Object, Boolean> duplicates;

        CheckingObjectOutputStream(@Nullable IdentityHashMap<Object, Boolean> identityHashMap) throws IOException {
            super(new NullOutputStream());
            if (identityHashMap == null) {
                firstRun = true;
                this.identityHashMap = new IdentityHashMap<>();
                duplicates = null;
            } else {
                firstRun = false;
                this.identityHashMap = identityHashMap;
                duplicates = new IdentityHashMap<>();
            }
            enableReplaceObject(true);
        }

        private static boolean isImmutable(Object object) {
            for (Class<?> clazz: Arrays.asList(Immutable.class, Enum.class, Pattern.class, String.class, Integer.class,
                    URI.class)) {
                if (clazz.isAssignableFrom(object.getClass())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        protected Object replaceObject(Object obj) throws IOException {
            if (isImmutable(obj)) {
                // Break recursion: No need to descend into immutable objects
                return Dummy.INSTANCE;
            }

            if (firstRun) {
                identityHashMap.put(obj, Boolean.TRUE);
            } else {
                assert duplicates != null;
                if (identityHashMap.containsKey(obj)) {
                    duplicates.put(obj, Boolean.TRUE);
                }
            }
            return super.replaceObject(obj);
        }
    }

    private static IdentityHashMap<Object, Boolean> collectObjects(
            Serializable object, @Nullable IdentityHashMap<Object, Boolean> identityHashMap) {
        try (CheckingObjectOutputStream checkingObjectOutputStream = new CheckingObjectOutputStream(identityHashMap)) {
            checkingObjectOutputStream.writeObject(object);
            @Nullable IdentityHashMap<Object, Boolean> returnMap = checkingObjectOutputStream.firstRun
                ? checkingObjectOutputStream.identityHashMap
                : checkingObjectOutputStream.duplicates;
            assert returnMap != null;
            return returnMap;
        } catch (IOException exception) {
            throw new IllegalStateException("Unexpected exception.", exception);
        }
    }

    private static List<Object> computeObjectGraphIntersection(Serializable first, Serializable second) {
        IdentityHashMap<Object, Boolean> firstObjectMap = collectObjects(first, null);
        IdentityHashMap<Object, Boolean> duplicates = collectObjects(second, firstObjectMap);
        return new ArrayList<>(duplicates.keySet());
    }
}
