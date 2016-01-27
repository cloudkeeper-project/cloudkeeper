package com.svbio.cloudkeeper.model.beans.type;

import com.svbio.cloudkeeper.model.bare.type.BarePrimitiveType;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class JAXBAdapters {
    private JAXBAdapters() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }


    @XmlAccessorType(XmlAccessType.FIELD)
    static class JAXBWrappedMutableTypeMirror {
        @XmlElementRef
        private MutableTypeMirror<?> type;
    }

    static class TypeMirrorWrapperAdapter extends XmlAdapter<JAXBWrappedMutableTypeMirror, MutableTypeMirror<?>> {
        @Override
        @Nullable
        public MutableTypeMirror<?> unmarshal(@Nullable JAXBWrappedMutableTypeMirror original) {
            return original != null
                ? original.type
                : null;
        }

        @Override
        @Nullable
        public JAXBWrappedMutableTypeMirror marshal(@Nullable MutableTypeMirror<?> original) {
            if (original == null) {
                return null;
            }

            JAXBWrappedMutableTypeMirror wrappedMutableTypeMirror = new JAXBWrappedMutableTypeMirror();
            wrappedMutableTypeMirror.type = original;
            return wrappedMutableTypeMirror;
        }
    }

    static class PrimitiveKindAdapter extends XmlAdapter<String, BarePrimitiveType.Kind> {
        private static final Map<String, BarePrimitiveType.Kind> JAVA_NAME_MAP;
        static {
            Map<String, BarePrimitiveType.Kind> newMap = new HashMap<>();
            for (BarePrimitiveType.Kind primitiveType: BarePrimitiveType.Kind.values()) {
                newMap.put(primitiveType.getPrimitiveClass().getName(), primitiveType);
            }
            JAVA_NAME_MAP = Collections.unmodifiableMap(newMap);
        }


        @Override
        @Nullable
        public BarePrimitiveType.Kind unmarshal(@Nullable String original) {
            return original != null
                ? JAVA_NAME_MAP.get(original)
                : null;
        }

        @Override
        @Nullable
        public String marshal(@Nullable BarePrimitiveType.Kind original) {
            return original != null
                ? original.getPrimitiveClass().getName()
                : null;
        }
    }
}
