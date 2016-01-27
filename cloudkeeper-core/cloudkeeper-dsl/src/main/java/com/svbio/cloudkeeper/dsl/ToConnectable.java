package com.svbio.cloudkeeper.dsl;

interface ToConnectable<T> extends Connectable {
    Module from(FromConnectable<? extends T> fromPort);
}
