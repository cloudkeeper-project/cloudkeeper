package com.svbio.cloudkeeper.linker;

import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.LinkerTraceElement;
import com.svbio.cloudkeeper.model.MissingPropertyException;
import com.svbio.cloudkeeper.model.bare.BareLocatable;
import com.svbio.cloudkeeper.model.immutable.Location;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

abstract class CopyContext {
    @Nullable private final CopyContext parentContext;

    private CopyContext() {
        parentContext = null;
    }

    private CopyContext(@Nullable CopyContext parentContext) {
        assert parentContext != null;
        this.parentContext = parentContext;
    }

    static CopyContext rootContext() {
        return RootCopyContext.INSTANCE;
    }

    final CopyContext newContextForProperty(String property) {
        Objects.requireNonNull(property);
        return new PropertyCopyContext(this, SimpleName.identifier(property));
    }

    final ListPropertyCopyContext newContextForListProperty(String property) {
        Objects.requireNonNull(property);
        return new ListPropertyCopyContext(this, SimpleName.identifier(property));
    }

    final CopyContext newContextForChild(Object child) {
        Objects.requireNonNull(child);
        return new ObjectCopyContext(this, child);
    }

    final CopyContext newSystemContext(String description) {
        Objects.requireNonNull(description);
        return new SystemCopyContext(this, description);
    }

    @Override
    public abstract String toString();

    @Nullable
    abstract Location getLocation();

    abstract LinkerException newMissingException();

    final List<LinkerTraceElement> toLinkerTrace() {
        List<LinkerTraceElement> referenceList = new LinkedList<>();

        @Nullable CopyContext current = this;
        do {
            referenceList.add(
                new LinkerTraceElement(current.toString(), current.getLocation())
            );
            current = current.parentContext;
        } while (current != null);
        return referenceList;
    }

    private static final class RootCopyContext extends CopyContext {
        private static final RootCopyContext INSTANCE = new RootCopyContext();

        @Override
        public String toString() {
            return "top-level copy context";
        }

        @Override
        Location getLocation() {
            return null;
        }

        @Override
        MissingPropertyException newMissingException() {
            return new MissingPropertyException("Root-level object is null.", toLinkerTrace());
        }
    }

    static final class CopyContextSupplier {
        private final ListPropertyCopyContext parentContext;
        private int index = 0;

        private CopyContextSupplier(ListPropertyCopyContext parentContext) {
            this.parentContext = parentContext;
        }

        public CopyContext get() {
            IndexCopyContext newCopyContext = new IndexCopyContext(parentContext, index);
            ++index;
            return newCopyContext;
        }
    }

    private abstract static class AbstractPropertyCopyContext extends CopyContext {
        private final SimpleName property;

        AbstractPropertyCopyContext(CopyContext parentContext, SimpleName property) {
            super(parentContext);
            this.property = property;
        }

        @Override
        public final String toString() {
            return "property '" + property + '\'';
        }

        @Override
        final Location getLocation() {
            return null;
        }

        @Override
        MissingPropertyException newMissingException() {
            return new MissingPropertyException(String.format(
                "Required property '%s' is null.", property
            ), toLinkerTrace());
        }
    }

    static final class PropertyCopyContext extends AbstractPropertyCopyContext {
        private PropertyCopyContext(CopyContext parentContext, SimpleName property) {
            super(parentContext, property);
        }
    }

    static final class ListPropertyCopyContext extends AbstractPropertyCopyContext {
        private ListPropertyCopyContext(CopyContext parentContext, SimpleName property) {
            super(parentContext, property);
        }

        CopyContextSupplier supplier() {
            return new CopyContextSupplier(this);
        }
    }

    private static final class IndexCopyContext extends CopyContext {
        private final int index;

        private IndexCopyContext(CopyContext parentContext, int index) {
            super(parentContext);
            assert index >= 0;
            this.index = index;
        }

        @Override
        public String toString() {
            return "index " + index;
        }

        @Override
        Location getLocation() {
            return null;
        }

        @Override
        MissingPropertyException newMissingException() {
            return new MissingPropertyException(String.format(
                "Required element in list property (%d) is null.", index
            ), toLinkerTrace());
        }
    }

    private static final class ObjectCopyContext extends CopyContext {
        private final Object object;

        private ObjectCopyContext(CopyContext parentContext, Object object) {
            super(parentContext);
            this.object = object;
        }

        @Override
        public String toString() {
            return object.toString();
        }

        @Override
        Location getLocation() {
            return object instanceof BareLocatable
                ? ((BareLocatable) object).getLocation()
                : null;
        }

        @Override
        MissingPropertyException newMissingException() {
            // This should not happen
            throw new IllegalStateException("Missing property in object copy context.");
        }
    }

    private static final class SystemCopyContext extends CopyContext {
        private final String description;

        private SystemCopyContext(CopyContext parentContext, String description) {
            super(parentContext);
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }

        @Override
        Location getLocation() {
            return null;
        }

        @Override
        MissingPropertyException newMissingException() {
            return new MissingPropertyException("Root-level object in system context is null.", toLinkerTrace());
        }
    }
}
