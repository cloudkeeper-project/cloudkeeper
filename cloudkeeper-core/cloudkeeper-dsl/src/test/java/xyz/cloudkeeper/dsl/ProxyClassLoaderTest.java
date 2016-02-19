package xyz.cloudkeeper.dsl;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

public class ProxyClassLoaderTest {
    private ProxyClassLoader classLoader;

    @BeforeClass
    public void setup() throws NoSuchMethodException {
        classLoader = new ProxyClassLoader(
            ClassLoader.getSystemClassLoader(),
            AbstractClass.class.getDeclaredMethod("foo", String.class)
        );
    }

    @Test
    public void testBase() throws Exception {
        classLoader.defineProxyForClass(AbstractClass.class);
        Class<?> clazz = classLoader.loadClass(ProxyClassLoader.getProxyNameFromName(AbstractClass.class.getName()));
        AbstractClass instance = (AbstractClass) clazz.newInstance();

        // Verify that implementing the abstract methods works
        Assert.assertEquals(instance.list(), Arrays.asList(1, 2));
        Assert.assertEquals(instance.arrayList(), Arrays.asList(3, 4));

        // defineProxyForClass must be idempotent!
        classLoader.defineProxyForClass(AbstractClass.class);
        Assert.assertSame(
            clazz,
            classLoader.loadClass(ProxyClassLoader.getProxyNameFromName(AbstractClass.class.getName()))
        );
    }

    @Test(dependsOnMethods = "testBase")
    public void testDerived() throws Exception {
        classLoader.defineProxyForClass(DerivedClass.class);
        Class<?> clazz = classLoader.loadClass(ProxyClassLoader.getProxyNameFromName(DerivedClass.class.getName()));
        DerivedClass instance = (DerivedClass) clazz.newInstance();

        // Verify that implementing the abstract methods works
        Assert.assertEquals(instance.list(), Arrays.asList(1, 2));
        Assert.assertEquals(instance.arrayList(), Arrays.asList(3, 4));
        Assert.assertEquals(instance.interfaceList(), Arrays.asList(5, 6));
        Assert.assertEquals(instance.derivedList(), Arrays.asList(7, 8));
    }

    public abstract static class AbstractClass {
        public abstract List<Integer> list();
        public abstract List<? extends Number> arrayList();

        public final Object foo(String method) {
            switch (method) {
                case "list": return Arrays.asList(1, 2);
                case "arrayList": return Arrays.asList(3, 4);
                case "interfaceList": return Arrays.asList(5, 6);
                case "derivedList": return Arrays.asList(7, 8);
                default: return null;
            }
        }
    }

    interface Foo {
        List<? extends Number> interfaceList();
    }

    public abstract static class DerivedClass extends AbstractClass implements Foo {
        public abstract List<? extends Integer> derivedList();

        @Override
        public abstract List<Integer> arrayList();
    }
}
