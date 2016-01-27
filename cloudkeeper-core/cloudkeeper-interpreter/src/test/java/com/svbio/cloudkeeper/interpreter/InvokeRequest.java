package com.svbio.cloudkeeper.interpreter;

import java.util.concurrent.Callable;

final class InvokeRequest {
    private final Callable<Void> callable;

    InvokeRequest(Callable<Void> callable) {
        this.callable = callable;
    }

    void invoke() throws Exception {
        callable.call();
    }
}
