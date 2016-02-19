package xyz.cloudkeeper.model.beans.element.type;

import xyz.cloudkeeper.model.bare.element.type.BareTypeDeclaration;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.Locale;

final class JAXBAdapters {
    private JAXBAdapters() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    static class TypeDeclarationKindAdapter extends XmlAdapter<String, BareTypeDeclaration.Kind> {
        @Override
        @Nullable
        public BareTypeDeclaration.Kind unmarshal(@Nullable String original) {
            return original != null
                ? BareTypeDeclaration.Kind.valueOf(original.toUpperCase(Locale.ENGLISH))
                : null;
        }

        @Override
        @Nullable
        public String marshal(@Nullable BareTypeDeclaration.Kind original) {
            return original != null
                ? original.toString().toLowerCase(Locale.ENGLISH)
                : null;
        }
    }
}
