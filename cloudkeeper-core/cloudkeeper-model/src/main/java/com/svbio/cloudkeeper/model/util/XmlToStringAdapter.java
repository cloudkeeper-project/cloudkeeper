package com.svbio.cloudkeeper.model.util;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public abstract class XmlToStringAdapter<T> extends XmlAdapter<String, T> {
    protected abstract T fromString(String original);

    @Override
    public final T unmarshal(String original) {
        return original != null
            ? fromString(original)
            : null;
    }

    @Override
    public final String marshal(T original) {
        return original != null
            ? original.toString()
            : null;
    }
}
