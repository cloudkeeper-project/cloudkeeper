package com.svbio.cloudkeeper.dsl;

import com.svbio.cloudkeeper.dsl.exception.IncompleteException;
import com.svbio.cloudkeeper.dsl.exception.InvalidTypeException;
import com.svbio.cloudkeeper.model.bare.element.module.BareInputModule;
import com.svbio.cloudkeeper.model.bare.element.module.BareModuleVisitor;
import com.svbio.cloudkeeper.model.bare.element.module.BareOutPort;
import com.svbio.cloudkeeper.model.bare.element.module.BarePortVisitor;
import com.svbio.cloudkeeper.model.bare.element.serialization.BareSerializationRoot;
import com.svbio.cloudkeeper.model.bare.type.BareTypeMirror;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;

import javax.annotation.Nullable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

public final class InputModule<T> extends Module<InputModule<T>> implements BareInputModule, FromConnectable<T> {
    private final T value;
    private final OutPort outPort;

    public final class OutPort extends Port implements FromConnectable<T>, DSLOutPort<T> {
        OutPort(Type type) {
            super(SimpleName.identifier(BareInputModule.OUT_PORT_NAME), type, true, null);
        }

        @Override
        public String toString() {
            return BareOutPort.Default.toString(this);
        }

        @Override
        public <U, P> U accept(BarePortVisitor<U, P> visitor, P parameter) {
            return visitor.visitOutPort(this, parameter);
        }
    }

    InputModule(Type staticType, @Nullable T value) {
        if (value == null) {
            throw new IncompleteException("value", getLocation());
        }

        outPort = new OutPort(staticType);
        this.value = value;
    }

    @Override
    public String toString() {
        return BareInputModule.Default.toString(this);
    }

    @Override
    public <U, P> U accept(BareModuleVisitor<U, P> visitor, P parameter) {
        return visitor.visitInputModule(this, parameter);
    }

    @Override
    public OutPort getPort() {
        return outPort;
    }

    @Override
    public BareTypeMirror getOutPortType() {
        return outPort.getType();
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public BareSerializationRoot getRaw() {
        return null;
    }

    @Override
    void finishConstruction(String simpleName, @Nullable Type declaredType, List<DSLAnnotation> annotations) {
        super.finishConstruction(simpleName, declaredType, annotations);

        if (outPort.getJavaType() == null) {
            Type javaType;
            // We need to determine the port type of this input module.
            if (
                declaredType instanceof ParameterizedType &&
                ((ParameterizedType) declaredType).getRawType().equals(InputModule.class)
            ) {
                // If this input module is not anonymous, we take the declared type as type (that is, the type parameter
                // given to InputModule).
                javaType = ((ParameterizedType) declaredType).getActualTypeArguments()[0];
            } else if (value.getClass().getTypeParameters().length == 0) {
                // If the module is anonymous, we test if the dynamic type is a class without type parameters. If yes,
                // we take that.
                javaType = value.getClass();
            } else {
                throw new InvalidTypeException(String.format("Cannot determine static type of input module %s. " +
                    "Expected %s but got %s.", simpleName, InputModule.class, declaredType), getLocation());
            }
            outPort.setJavaType(javaType);
        }
        // Implicit ports are not handled by super.finishConstruction()
        outPort.finishConstruction();
    }
}
