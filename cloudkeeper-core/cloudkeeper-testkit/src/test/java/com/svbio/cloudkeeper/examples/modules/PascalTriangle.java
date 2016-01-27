package com.svbio.cloudkeeper.examples.modules;

import com.svbio.cloudkeeper.model.bare.element.module.BareInputModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableChildOutToParentOutConnection;
import com.svbio.cloudkeeper.model.beans.element.module.MutableCompositeModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableConnection;
import com.svbio.cloudkeeper.model.beans.element.module.MutableInputModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableOutPort;
import com.svbio.cloudkeeper.model.beans.element.module.MutablePort;
import com.svbio.cloudkeeper.model.beans.element.module.MutableProxyModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableSiblingConnection;
import com.svbio.cloudkeeper.model.beans.type.MutableDeclaredType;

import java.util.ArrayList;
import java.util.List;

public final class PascalTriangle {
    private PascalTriangle() { }

    /**
     * Returns a composite module that has the form of Pascal's triangle.
     *
     * <p>The composite module has {@code numOutPorts} out-ports, named {@code coef_0}, {@code coef_1}, ...,
     * {@code coef_numOutPorts} that contain the values binomial({@code n}, 0) to
     * binomial({@code n}, {@code numOutPorts}).
     *
     * @param n Number of rows in pascal triangle - 1.
     * @param numOutPorts Number of out-ports, containing the values binomial({@code n}, 0) to
     *     binomial({@code n}, {@code numOutPorts}).
     * @return composite module
     * @throws IndexOutOfBoundsException if {@code n < 1} or {@code numOutPorts < 1} or {@code numOutPorts > n + 1}
     */
    public static MutableCompositeModule createCompositeModule(int n, int numOutPorts) {
        if (n < 1 || numOutPorts < 1 || numOutPorts > n + 1) {
            throw new IndexOutOfBoundsException("Invalid arguments for Pascal triangle.");
        }

        List<MutableModule<?>> children = new ArrayList<>();
        List<MutableConnection<?>> connections = new ArrayList<>();
        children.add(
            new MutableInputModule()
                .setSimpleName("one")
                .setValue(1)
                .setOutPortType(MutableDeclaredType.fromType(Integer.class))
        );
        for (int i = 2; i <= n; ++i) {
            for (int j = 1; j < i; ++j) {
                String firstAncestorModule = j == 1 ? "one" : "pos_" + (i - 1) + '_' + (j - 1);
                String secondAncestorModule = j == i - 1 ? "one" : "pos_" + (i - 1) + '_' + j;
                String firstOutPortName = "one".equals(firstAncestorModule) ? BareInputModule.OUT_PORT_NAME : "sum";
                String secondOutPortName = "one".equals(secondAncestorModule) ? BareInputModule.OUT_PORT_NAME : "sum";
                String newModuleName = "pos_" + i + '_' + j;
                children.add(
                    new MutableProxyModule()
                        .setSimpleName(newModuleName)
                        .setDeclaration(BinarySum.class.getName())
                );
                connections.add(
                    new MutableSiblingConnection()
                        .setFromModule(firstAncestorModule).setFromPort(firstOutPortName)
                        .setToModule(newModuleName).setToPort("num1")
                );
                connections.add(
                    new MutableSiblingConnection()
                        .setFromModule(secondAncestorModule).setFromPort(secondOutPortName)
                        .setToModule(newModuleName).setToPort("num2")
                );
            }
        }

        List<MutablePort<?>> ports = new ArrayList<>();
        ports.add(
            new MutableOutPort()
                .setSimpleName("coef_0")
                .setType(MutableDeclaredType.fromType(Integer.class))
        );
        connections.add(
            new MutableChildOutToParentOutConnection()
                .setFromModule("one").setFromPort(BareInputModule.OUT_PORT_NAME)
                .setToPort("coef_0")
        );
        for (int i = 1; i < numOutPorts && i < n; ++i) {
            ports.add(
                new MutableOutPort()
                    .setSimpleName("coef_" + i)
                    .setType(MutableDeclaredType.fromType(Integer.class))
            );
            connections.add(
                new MutableChildOutToParentOutConnection()
                    .setFromModule("pos_" + n + '_' + i).setFromPort("sum")
                    .setToPort("coef_" + i)
            );
        }
        if (numOutPorts == n + 1) {
            ports.add(
                new MutableOutPort()
                    .setSimpleName("coef_" + n)
                    .setType(MutableDeclaredType.fromType(Integer.class))
            );
            connections.add(
                new MutableChildOutToParentOutConnection()
                    .setFromModule("one").setFromPort(BareInputModule.OUT_PORT_NAME)
                    .setToPort("coef_" + n)
            );
        }

        return new MutableCompositeModule()
            .setDeclaredPorts(ports)
            .setModules(children)
            .setConnections(connections);
    }

    /**
     * Returns a composite module that has the form of Pascal's triangle.
     *
     * <p>This method is equivalent to {@link #createCompositeModule(int, int)} with arguments {@code n} and
     * {@code n + 1}.
     *
     * @param n Number of rows in pascal triangle - 1.
     * @return composite module
     * @throws IndexOutOfBoundsException if {@code n < 1}
     */
    public static MutableCompositeModule createCompositeModule(int n) {
        return createCompositeModule(n, n + 1);
    }
}
