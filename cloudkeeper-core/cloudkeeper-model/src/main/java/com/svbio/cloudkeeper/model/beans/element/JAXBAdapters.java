package com.svbio.cloudkeeper.model.beans.element;

import com.svbio.cloudkeeper.model.immutable.element.Name;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.adapters.XmlAdapter;

final class JAXBAdapters {
    private JAXBAdapters() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    static final class MutableQualifiedNamableAdapter extends XmlAdapter<String, MutableQualifiedNamable> {
        @Override
        @Nullable
        public MutableQualifiedNamable unmarshal(@Nullable String original) {
            return original == null
                ? null
                : new MutableQualifiedNamable().setQualifiedName(original);
        }

        @Override
        @Nullable
        public String marshal(@Nullable MutableQualifiedNamable original) {
            if (original == null) {
                return null;
            }
            @Nullable Name qualifiedName = original.getQualifiedName();
            if (qualifiedName == null) {
                return null;
            }
            return qualifiedName.toString();
        }
    }

    static final class MutableSimpleNamableAdapter extends XmlAdapter<String, MutableSimpleNameable> {
        @Override
        @Nullable
        public MutableSimpleNameable unmarshal(@Nullable String original) {
            return original == null
                ? null
                : new MutableSimpleNameable().setSimpleName(original);
        }

        @Override
        @Nullable
        public String marshal(@Nullable MutableSimpleNameable original) {
            if (original == null) {
                return null;
            }
            @Nullable SimpleName simpleName = original.getSimpleName();
            if (simpleName == null) {
                return null;
            }
            return simpleName.toString();
        }
    }
}
