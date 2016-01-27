package com.svbio.cloudkeeper.dsl;

import com.svbio.cloudkeeper.dsl.exception.CannotInstantiateException;
import com.svbio.cloudkeeper.dsl.exception.DSLException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

final class Instantiator {
    private Instantiator() {
        throw new AssertionError(String.format("No %s instances for you!", getClass().getName()));
    }

    static <T> T instantiate(Class<T> clazz) throws DSLException {
        if (
            (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers())) ||
            clazz.isAnonymousClass() ||
            clazz.isLocalClass()
        ) {
            throw new CannotInstantiateException(String.format("Cannot instantiate %s, because it is an inner, " +
                "nested, or anonymous class.", clazz), null);
        }

        final Constructor<T> constructor;
        try {
            constructor = clazz.getConstructor();
        } catch (NoSuchMethodException exception) {
            throw new CannotInstantiateException(
                String.format("Cannot instantiate %s due to missing public no-argument constructor.", clazz),
                exception,
                null
            );
        }

        try {
            return constructor.newInstance();
        } catch (InstantiationException exception) {
            throw new CannotInstantiateException(
                String.format("Cannot instantiate abstract %s.", clazz),
                exception,
                null
            );
        } catch (IllegalAccessException exception) {
            throw new CannotInstantiateException(
                String.format("Cannot instantiate %s because the no-argument constructor is inaccessible.", clazz),
                exception,
                null
            );
        } catch (InvocationTargetException exception) {
            if (exception.getCause() instanceof DSLException) {
                throw (DSLException) exception.getCause();
            }

            throw new CannotInstantiateException(
                String.format("Cannot instantiate %s because the public no-argument constructor threw an " +
                    "exception.", clazz),
                exception.getCause(),
                null
            );
        }
    }
}
