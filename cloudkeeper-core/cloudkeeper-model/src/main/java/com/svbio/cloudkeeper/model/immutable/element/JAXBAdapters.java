package com.svbio.cloudkeeper.model.immutable.element;

import com.svbio.cloudkeeper.model.util.XmlToStringAdapter;

final class JAXBAdapters {
    private JAXBAdapters() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    public static final class KeyAdapter extends XmlToStringAdapter<Key> {
        @Override
        protected Key fromString(String original) {
            return Key.valueOf(original);
        }
    }

    public static final class IndexAdapter extends XmlToStringAdapter<Index> {
        @Override
        protected Index fromString(String original) {
            return Index.index(original);
        }
    }

    public static final class NameAdapter extends XmlToStringAdapter<Name> {
        @Override
        protected Name fromString(String original) {
            return Name.qualifiedName(original);
        }
    }

    public static final class SimpleNameAdapter extends XmlToStringAdapter<SimpleName> {
        @Override
        protected SimpleName fromString(String original) {
            return SimpleName.identifier(original);
        }
    }

    public static final class VersionAdapter extends XmlToStringAdapter<Version> {
        @Override
        protected Version fromString(String original) {
            return Version.valueOf(original);
        }
    }
}
