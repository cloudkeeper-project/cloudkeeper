package com.svbio.cloudkeeper.model.immutable;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

final class JAXBAdapters {
    private JAXBAdapters() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    private interface JAXBInterface {
        Object toNativeValue();
    }

    private static Object jaxbToArray(Class<?> componentType, List<?> list) {
        Object array = Array.newInstance(componentType, list.size());
        int i = 0;
        for (Object element: list) {
            Array.set(array, i, element);
            ++i;
        }
        return array;
    }

    @XmlRootElement(name = "boolean-array")
    static final class BooleanArray implements JAXBInterface {
        @XmlValue
        private List<Boolean> list;

        @Override
        public Object toNativeValue() {
            return jaxbToArray(boolean.class, list);
        }
    }

    @XmlRootElement(name = "boolean")
    static final class BooleanValue implements JAXBInterface {
        @XmlValue
        private boolean value;

        @Override
        public Object toNativeValue() {
            return value;
        }
    }

    @XmlRootElement(name = "byte-array")
    static final class ByteArray implements JAXBInterface {
        @XmlValue
        private List<Byte> list;

        @Override
        public Object toNativeValue() {
            return jaxbToArray(byte.class, list);
        }
    }

    @XmlRootElement(name = "byte")
    static final class ByteValue implements JAXBInterface {
        @XmlValue
        private byte value;

        @Override
        public Object toNativeValue() {
            return value;
        }
    }

    @XmlRootElement(name = "char-array")
    static final class CharArray implements JAXBInterface {
        @XmlValue
        private List<Character> list;

        @Override
        public Object toNativeValue() {
            return jaxbToArray(char.class, list);
        }
    }

    @XmlRootElement(name = "char")
    static final class CharValue implements JAXBInterface {
        @XmlValue
        private char value;

        @Override
        public Object toNativeValue() {
            return value;
        }
    }

    @XmlRootElement(name = "double-array")
    static final class DoubleArray implements JAXBInterface {
        @XmlValue
        private List<Double> list;

        @Override
        public Object toNativeValue() {
            return jaxbToArray(double.class, list);
        }
    }

    @XmlRootElement(name = "double")
    static final class DoubleValue implements JAXBInterface {
        @XmlValue
        private double value;

        @Override
        public Object toNativeValue() {
            return value;
        }
    }

    @XmlRootElement(name = "float-array")
    static final class FloatArray implements JAXBInterface {
        @XmlValue
        private List<Float> list;

        @Override
        public Object toNativeValue() {
            return jaxbToArray(float.class, list);
        }
    }

    @XmlRootElement(name = "float")
    static final class FloatValue implements JAXBInterface {
        @XmlValue
        private float value;

        @Override
        public Object toNativeValue() {
            return value;
        }
    }

    @XmlRootElement(name = "int-array")
    static final class IntArray implements JAXBInterface {
        @XmlValue
        private List<Integer> list;

        @Override
        public Object toNativeValue() {
            return jaxbToArray(int.class, list);
        }
    }

    @XmlRootElement(name = "int")
    static final class IntValue implements JAXBInterface {
        @XmlValue
        private int value;

        @Override
        public Object toNativeValue() {
            return value;
        }
    }

    @XmlRootElement(name = "long-array")
    static final class LongArray implements JAXBInterface {
        @XmlValue
        private List<Long> list;

        @Override
        public Object toNativeValue() {
            return jaxbToArray(long.class, list);
        }
    }

    @XmlRootElement(name = "long")
    static final class LongValue implements JAXBInterface {
        @XmlValue
        private long value;

        @Override
        public Object toNativeValue() {
            return value;
        }
    }

    @XmlRootElement(name = "short-array")
    static final class ShortArray implements JAXBInterface {
        @XmlValue
        private List<Short> list;

        @Override
        public Object toNativeValue() {
            return jaxbToArray(short.class, list);
        }
    }

    @XmlRootElement(name = "short")
    static final class ShortValue implements JAXBInterface {
        @XmlValue
        private short value;

        @Override
        public Object toNativeValue() {
            return value;
        }
    }

    @XmlRootElement(name = "string-array")
    static final class StringArray implements JAXBInterface {
        @XmlValue
        private List<String> list;

        @Override
        public Object toNativeValue() {
            return jaxbToArray(String.class, list);
        }
    }

    @XmlRootElement(name = "string")
    static final class StringValue implements JAXBInterface {
        @XmlValue
        private String value;

        @Override
        public Object toNativeValue() {
            return value;
        }
    }

    @XmlRootElement(name = "annotation-value")
    static final class JAXBAnnotationValue {
        @XmlElementRefs({
            @XmlElementRef(type = BooleanArray.class),
            @XmlElementRef(type = BooleanValue.class),
            @XmlElementRef(type = ByteArray.class),
            @XmlElementRef(type = ByteValue.class),
            @XmlElementRef(type = CharArray.class),
            @XmlElementRef(type = CharValue.class),
            @XmlElementRef(type = DoubleArray.class),
            @XmlElementRef(type = DoubleValue.class),
            @XmlElementRef(type = FloatArray.class),
            @XmlElementRef(type = FloatValue.class),
            @XmlElementRef(type = IntArray.class),
            @XmlElementRef(type = IntValue.class),
            @XmlElementRef(type = LongArray.class),
            @XmlElementRef(type = LongValue.class),
            @XmlElementRef(type = ShortArray.class),
            @XmlElementRef(type = ShortValue.class),
            @XmlElementRef(type = StringArray.class),
            @XmlElementRef(type = StringValue.class)
        })
        private Object object;
    }

    static final class AnnotationValueAdapter extends XmlAdapter<JAXBAnnotationValue, AnnotationValue> {
        @Override
        public AnnotationValue unmarshal(JAXBAnnotationValue original) {
            if (original == null) {
                return null;
            }
            Object object = original.object;
            return object == null
                ? null
                : AnnotationValue.of(((JAXBInterface) object).toNativeValue());
        }

        @SuppressWarnings("unchecked")
        private static <T> List<T> cast(List<?> list) {
            return (List<T>) list;
        }

        private static Object arrayToJAXB(Object array) {
            int size = Array.getLength(array);
            List<Object> list = new ArrayList<>(size);
            for (int i = 0; i < size; ++i) {
                list.add(Array.get(array, i));
            }

            if (array instanceof boolean[]) {
                BooleanArray jaxbObject = new BooleanArray();
                jaxbObject.list = cast(list);
                return jaxbObject;
            } else if (array instanceof byte[]) {
                ByteArray jaxbObject = new ByteArray();
                jaxbObject.list = cast(list);
                return jaxbObject;
            } else if (array instanceof char[]) {
                CharArray jaxbObject = new CharArray();
                jaxbObject.list = cast(list);
                return jaxbObject;
            } else if (array instanceof double[]) {
                DoubleArray jaxbObject = new DoubleArray();
                jaxbObject.list = cast(list);
                return jaxbObject;
            } else if (array instanceof float[]) {
                FloatArray jaxbObject = new FloatArray();
                jaxbObject.list = cast(list);
                return jaxbObject;
            } else if (array instanceof int[]) {
                IntArray jaxbObject = new IntArray();
                jaxbObject.list = cast(list);
                return jaxbObject;
            } else if (array instanceof long[]) {
                LongArray jaxbObject = new LongArray();
                jaxbObject.list = cast(list);
                return jaxbObject;
            } else if (array instanceof short[]) {
                ShortArray jaxbObject = new ShortArray();
                jaxbObject.list = cast(list);
                return jaxbObject;
            } else {
                StringArray jaxbObject = new StringArray();
                jaxbObject.list = cast(list);
                return jaxbObject;
            }
        }

        private static Object nonArrayToJAXB(Object value) {
            if (value instanceof Boolean) {
                BooleanValue jaxbObject = new BooleanValue();
                jaxbObject.value = (Boolean) value;
                return jaxbObject;
            } else if (value instanceof Byte) {
                ByteValue jaxbObject = new ByteValue();
                jaxbObject.value = (Byte) value;
                return jaxbObject;
            } else if (value instanceof Character) {
                CharValue jaxbObject = new CharValue();
                jaxbObject.value = (Character) value;
                return jaxbObject;
            } else if (value instanceof Double) {
                DoubleValue jaxbObject = new DoubleValue();
                jaxbObject.value = (Double) value;
                return jaxbObject;
            } else if (value instanceof Float) {
                FloatValue jaxbObject = new FloatValue();
                jaxbObject.value = (Float) value;
                return jaxbObject;
            } else if (value instanceof Integer) {
                IntValue jaxbObject = new IntValue();
                jaxbObject.value = (Integer) value;
                return jaxbObject;
            } else if (value instanceof Long) {
                LongValue jaxbObject = new LongValue();
                jaxbObject.value = (Long) value;
                return jaxbObject;
            } else if (value instanceof Short) {
                ShortValue jaxbObject = new ShortValue();
                jaxbObject.value = (Short) value;
                return jaxbObject;
            } else {
                StringValue jaxbObject = new StringValue();
                jaxbObject.value = (String) value;
                return jaxbObject;
            }
        }

        @Override
        public JAXBAnnotationValue marshal(AnnotationValue original) {
            if (original == null) {
                return null;
            }
            Object nativeValue = original.toNativeValue();
            JAXBAnnotationValue jaxbContainer = new JAXBAnnotationValue();
            if (nativeValue.getClass().isArray()) {
                jaxbContainer.object = arrayToJAXB(nativeValue);
            } else {
                jaxbContainer.object = nonArrayToJAXB(nativeValue);
            }
            return jaxbContainer;
        }
    }
}
