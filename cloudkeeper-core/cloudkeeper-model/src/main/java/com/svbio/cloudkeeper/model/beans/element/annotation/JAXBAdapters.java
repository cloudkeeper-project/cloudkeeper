package com.svbio.cloudkeeper.model.beans.element.annotation;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.Collections;
import java.util.List;

final class JAXBAdapters {
    private JAXBAdapters() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    static final class EntriesWrapper {
        @XmlElement
        @Nullable
        private List<MutableAnnotationEntry> entry;
    }

    static final class EntriesWrapperAdapter extends XmlAdapter<EntriesWrapper, List<MutableAnnotationEntry>> {
        @Override
        public List<MutableAnnotationEntry> unmarshal(@Nullable EntriesWrapper original) {
            return original == null || original.entry == null
                ? Collections.<MutableAnnotationEntry>emptyList()
                : original.entry;
        }

        @Override
        public EntriesWrapper marshal(@Nullable List<MutableAnnotationEntry> original) {
            EntriesWrapper entriesWrapper = new EntriesWrapper();
            entriesWrapper.entry = original;
            return entriesWrapper;
        }
    }
}
