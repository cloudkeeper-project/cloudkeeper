package com.svbio.cloudkeeper.interpreter;

import com.svbio.cloudkeeper.linker.Linker;
import com.svbio.cloudkeeper.linker.LinkerOptions;
import com.svbio.cloudkeeper.model.Immutable;
import com.svbio.cloudkeeper.model.LinkerException;
import com.svbio.cloudkeeper.model.bare.element.module.BareInPort;
import com.svbio.cloudkeeper.model.bare.element.module.BareLoopModule;
import com.svbio.cloudkeeper.model.bare.element.module.BareOutPort;
import com.svbio.cloudkeeper.model.beans.element.MutableBundle;
import com.svbio.cloudkeeper.model.beans.element.MutablePackage;
import com.svbio.cloudkeeper.model.beans.element.MutablePluginDeclaration;
import com.svbio.cloudkeeper.model.beans.element.MutableQualifiedNamable;
import com.svbio.cloudkeeper.model.beans.element.MutableSimpleNameable;
import com.svbio.cloudkeeper.model.beans.element.module.MutableChildOutToParentOutConnection;
import com.svbio.cloudkeeper.model.beans.element.module.MutableCompositeModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableCompositeModuleDeclaration;
import com.svbio.cloudkeeper.model.beans.element.module.MutableConnection;
import com.svbio.cloudkeeper.model.beans.element.module.MutableIOPort;
import com.svbio.cloudkeeper.model.beans.element.module.MutableInPort;
import com.svbio.cloudkeeper.model.beans.element.module.MutableLoopModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableOutPort;
import com.svbio.cloudkeeper.model.beans.element.module.MutableParentInToChildInConnection;
import com.svbio.cloudkeeper.model.beans.element.module.MutableParentModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutablePort;
import com.svbio.cloudkeeper.model.beans.element.module.MutableProxyModule;
import com.svbio.cloudkeeper.model.beans.element.module.MutableShortCircuitConnection;
import com.svbio.cloudkeeper.model.beans.element.module.MutableSiblingConnection;
import com.svbio.cloudkeeper.model.beans.element.module.MutableSimpleModuleDeclaration;
import com.svbio.cloudkeeper.model.beans.type.MutableDeclaredType;
import com.svbio.cloudkeeper.model.immutable.element.Name;
import com.svbio.cloudkeeper.model.immutable.element.SimpleName;
import com.svbio.cloudkeeper.model.immutable.execution.ExecutionTrace;
import com.svbio.cloudkeeper.model.runtime.element.RuntimeRepository;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeCompositeModuleDeclaration;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeModule;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeParentModule;
import com.svbio.cloudkeeper.model.runtime.element.module.RuntimeProxyModule;
import com.svbio.cloudkeeper.model.runtime.execution.RuntimeAnnotatedExecutionTrace;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class that encapsulates a {@link RuntimeRepository} and a {@link RuntimeAnnotatedExecutionTrace} representing a
 * {@link com.svbio.cloudkeeper.model.runtime.element.module.RuntimeCompositeModule}.
 */
public final class CompositeModuleContext implements Immutable {
    /**
     * The default simple name of the composite-module declaration (if any).
     */
    public static final SimpleName DECLARATION_NAME = SimpleName.identifier("TestModule");

    /**
     * The simple name of the module under test (which is either a {@link RuntimeProxyModule} or a
     * {@link com.svbio.cloudkeeper.model.runtime.element.module.RuntimeLoopModule}).
     */
    public static final SimpleName MODULE_NAME = SimpleName.identifier("testModule");

    public static final ExecutionTrace TEST_MODULE_TRACE
        = ExecutionTrace.empty().resolveContent().resolveModule(MODULE_NAME);

    /**
     * The default bundle identifier.
     */
    public static final URI DEFAULT_BUNDLE_IDENTIFIER = URI.create("x-test:" + CompositeModuleContext.class.getName());

    private final RuntimeRepository repository;
    private final RuntimeAnnotatedExecutionTrace executionTrace;


    private CompositeModuleContext(RuntimeRepository repository, RuntimeAnnotatedExecutionTrace executionTrace) {
        this.repository = repository;
        this.executionTrace = executionTrace;
    }

    public RuntimeRepository getRepository() {
        return repository;
    }

    public RuntimeAnnotatedExecutionTrace getExecutionTrace() {
        return executionTrace;
    }

    public RuntimeParentModule getModule() {
        RuntimeModule module = executionTrace.getModule();
        return module instanceof RuntimeProxyModule
            ? ((RuntimeCompositeModuleDeclaration) ((RuntimeProxyModule) module).getDeclaration()).getTemplate()
            : (RuntimeParentModule) module;
    }

    /**
     * Returns a new composite-module context from the given connections.
     *
     * <p>This is a convenience method. The result is equivalent to creating a new {@link Builder} instance, calling
     * {@link CompositeModuleContext.Builder#addConnection(String)} for each given connection, and returning
     * the instance built by {@link Builder#build()}. The bundle identifier is always
     * {@link #DEFAULT_BUNDLE_IDENTIFIER}.
     *
     * @return the composite-module context
     */
    public static CompositeModuleContext fromConnections(List<String> connections) {
        Builder builder = new Builder(DEFAULT_BUNDLE_IDENTIFIER);
        connections.forEach(builder::addConnection);
        return builder.build();
    }

    public enum PortType {
        IN {
            @Override
            Supplier<? extends MutablePort<?>> supplier() {
                return MutableInPort::new;
            }
        },

        OUT {
            @Override
            Supplier<? extends MutablePort<?>> supplier() {
                return MutableOutPort::new;
            }
        },

        IO {
            @Override
            Supplier<? extends MutablePort<?>> supplier() {
                return MutableIOPort::new;
            }
        };

        PortType merge(@Nullable PortType other) {
            return other == null || other == this
                ? this
                : IO;
        }

        abstract Supplier<? extends MutablePort<?>> supplier();
    }

    /**
     * This class is used to build {@link RuntimeAnnotatedExecutionTrace} instances (that represents a
     * {@link com.svbio.cloudkeeper.model.runtime.element.module.RuntimeCompositeModule} and is linked against a fresh
     * {@link RuntimeRepository}).
     *
     * <p>Most users of this class only need to call {@link #addConnection(String)}.
     */
    public static final class Builder {
        private static final Name PACKAGE = Name.qualifiedName(Builder.class.getPackage().getName());
        private static final Pattern CONNECTION_PATTERN = Pattern.compile(
            "(?<source>\\p{Lower}+)(?:\\.(?<sourceport>\\p{Lower}+))? -> " +
                "(?<target>\\p{Lower}+)(?:\\.(?<targetport>\\p{Lower}+))?"
        );

        private final URI bundleIdentifier;
        private final Map<String, Map<String, PortType>> childrenPortMaps = new HashMap<>();
        private final Map<String, MutableSimpleModuleDeclaration> moduleDeclarationMap = new LinkedHashMap<>();
        private final List<MutableModule<?>> childModules = new ArrayList<>();
        private final Map<String, PortType> portMap = new LinkedHashMap<>();
        private final List<MutableConnection<?>> connections = new ArrayList<>();
        private boolean inlineTestModule = false;

        public Builder(URI bundleIdentifier) {
            Objects.requireNonNull(bundleIdentifier);
            this.bundleIdentifier = bundleIdentifier;
        }

        /**
         * Sets whether the test module should be inlined, that is, no composite-module declaration should be created.
         *
         * <p>If the test module is inlined, it is either a
         * {@link com.svbio.cloudkeeper.model.runtime.element.module.RuntimeCompositeModule} or
         * {@link com.svbio.cloudkeeper.model.runtime.element.module.RuntimeLoopModule}. Otherwise, the test module is
         * a {@link RuntimeProxyModule} referencing a {@link RuntimeCompositeModuleDeclaration}.
         */
        public Builder setInlineTestModule(boolean inlineTestModule) {
            this.inlineTestModule = inlineTestModule;
            return this;
        }

        private static PortType addPort(Map<String, PortType> portMap, String portName, PortType portType) {
            @Nullable PortType previousPortType = portMap.get(portName);
            PortType mergedPortType = portType.merge(previousPortType);
            portMap.put(portName, mergedPortType);
            return mergedPortType;
        }

        public void addPort(String portName, PortType portType) {
            Objects.requireNonNull(portName);
            Objects.requireNonNull(portType);

            addPort(portMap, portName, portType);
        }

        public void addChildModule(String moduleName) {
            Objects.requireNonNull(moduleName);
            if (moduleName.length() < 1) {
                throw new IllegalArgumentException("Expected non-empty module name.");
            }

            assert moduleName.length() >= 1;
            if (!childrenPortMaps.containsKey(moduleName)) {
                assert !moduleDeclarationMap.containsKey(moduleName);
                SimpleName simpleName
                    = SimpleName.identifier(Character.toUpperCase(moduleName.charAt(0)) + moduleName.substring(1));
                Name qualifiedName = PACKAGE.join(simpleName);
                childrenPortMaps.put(moduleName, new LinkedHashMap<>());
                moduleDeclarationMap.put(
                    moduleName,
                    new MutableSimpleModuleDeclaration()
                        .setSimpleName(simpleName)
                );
                childModules.add(
                    new MutableProxyModule()
                        .setSimpleName(moduleName)
                        .setDeclaration(new MutableQualifiedNamable().setQualifiedName(qualifiedName))
                );
            }
        }

        public void addChildPort(String module, String port, PortType portType) {
            Objects.requireNonNull(module);
            Objects.requireNonNull(port);
            Objects.requireNonNull(portType);

            addChildModule(module);
            @Nullable Map<String, PortType> childPortMap = childrenPortMaps.get(module);
            @Nullable MutableSimpleModuleDeclaration moduleDeclaration = moduleDeclarationMap.get(module);
            assert childPortMap != null && moduleDeclaration != null;
            addPort(childPortMap, port, portType);
        }

        /**
         * Adds a new connection to this builder.
         *
         * <p>This method expects connections to be of form {@code source[:sourceport] -> target[:targetport]} where
         * <ul><li>
         *     {@code source} is either the name of an in-port or the name of a child module
         * </li><li>
         *     {@code sourceport} is the name of a child module's in-port (only if {@code source} is the name of a child
         *     module})
         * </li><li>
         *     {@code target} is either the name of an out-port or the name of a child module
         * </li><li>
         *     {@code targetport} is the name of a child module's out-port (only if {@code target} is the name of a
         *     child module})
         * </li></ul>
         *
         * @param connectionString string of form {@code source[:sourceport] -> target[:targetport]}
         */
        public void addConnection(String connectionString) {
            Objects.requireNonNull(connectionString);

            Matcher matcher = CONNECTION_PATTERN.matcher(connectionString);
            if (!matcher.matches()) {
                throw new IllegalArgumentException(String.format(
                    "Expected connection string of form \"source[:sourceport] -> target[:targetport]\", "
                        + "but got \"%s\".",
                    connectionString
                ));
            }
            @Nullable String source = matcher.group("source");
            @Nullable String sourcePort = matcher.group("sourceport");
            @Nullable String target = matcher.group("target");
            @Nullable String targetPort = matcher.group("targetport");
            assert source != null && target != null : "post-condition of Matcher#matches()";
            if (targetPort == null) {
                addPort(target, PortType.OUT);
            } else {
                addChildPort(target, targetPort, PortType.IN);
            }
            if (sourcePort == null) {
                addPort(source, PortType.IN);
                if (targetPort == null) {
                    connections.add(
                        new MutableShortCircuitConnection()
                            .setFromPort(source)
                            .setToPort(target)
                    );
                } else {
                    connections.add(
                        new MutableParentInToChildInConnection()
                            .setFromPort(source)
                            .setToModule(target)
                            .setToPort(targetPort)
                    );
                }
            } else {
                addChildPort(source, sourcePort, PortType.OUT);
                if (targetPort == null) {
                    connections.add(
                        new MutableChildOutToParentOutConnection()
                            .setFromModule(source)
                            .setFromPort(sourcePort)
                            .setToPort(target)
                    );
                } else {
                    connections.add(
                        new MutableSiblingConnection()
                            .setFromModule(source)
                            .setFromPort(sourcePort)
                            .setToModule(target)
                            .setToPort(targetPort)
                    );
                }
            }
        }

        private static List<MutablePort<?>> ports(Map<String, PortType> portMap) {
            return portMap.entrySet().stream()
                .filter(entry -> !BareLoopModule.CONTINUE_PORT_NAME.equals(entry.getKey()))
                .map(
                    entry
                        -> entry.getValue().supplier().get()
                        .setSimpleName(entry.getKey())
                        .setType(
                            new MutableDeclaredType()
                                .setDeclaration(Integer.class.getName())
                        )
                )
                .collect(Collectors.toList());
        }

        private static Stream<MutablePort<?>> expandIfIOPort(MutablePort<?> port) {
            if (port instanceof MutableIOPort) {
                return Stream.of(
                    new MutableInPort().setSimpleName(enclosingInPortName(port)).setType(port.getType()),
                    new MutableOutPort().setSimpleName(enclosingOutPortName(port)).setType(port.getType())
                );
            } else {
                return Stream.of(port);
            }
        }

        private static String enclosingInPortName(MutablePort<?> port) {
            assert port instanceof BareInPort;
            String portName = Objects.requireNonNull(port.getSimpleName()).toString();
            return port instanceof MutableIOPort
                ? portName + "$in"
                : portName;
        }

        private static String enclosingOutPortName(MutablePort<?> port) {
            assert port instanceof BareOutPort;
            String portName = Objects.requireNonNull(port.getSimpleName()).toString();
            return port instanceof MutableIOPort
                ? portName + "$out"
                : portName;
        }

        private static List<MutableConnection<?>> connectionsToTestModule(List<MutablePort<?>> testModulePorts) {
            List<MutableConnection<?>> connections = new ArrayList<>(testModulePorts.size());
            for (MutablePort<?> testModulePort: testModulePorts) {
                if (testModulePort instanceof BareInPort) {
                    connections.add(
                        new MutableParentInToChildInConnection()
                            .setFromPort(enclosingInPortName(testModulePort))
                            .setToModule(new MutableSimpleNameable().setSimpleName(MODULE_NAME))
                            .setToPort(new MutableSimpleNameable().setSimpleName(testModulePort.getSimpleName()))
                    );
                }
                if (testModulePort instanceof BareOutPort) {
                    connections.add(
                        new MutableChildOutToParentOutConnection()
                            .setFromModule(new MutableSimpleNameable().setSimpleName(MODULE_NAME))
                            .setFromPort(new MutableSimpleNameable().setSimpleName(testModulePort.getSimpleName()))
                            .setToPort(enclosingOutPortName(testModulePort))
                    );
                }
            }
            return connections;
        }

        private MutableCompositeModule setUpModules(MutableParentModule<?> parentModule, MutableModule<?> testModule) {
            testModule.setSimpleName(MODULE_NAME);
            parentModule
                .setDeclaredPorts(ports(portMap))
                .setModules(childModules)
                .setConnections(connections);
            List<MutablePort<?>> enclosingModulePorts = parentModule.getDeclaredPorts().stream()
                .flatMap(Builder::expandIfIOPort)
                .collect(Collectors.toList());
            return new MutableCompositeModule()
                .setDeclaredPorts(enclosingModulePorts)
                .setModules(Collections.singletonList(testModule))
                .setConnections(connectionsToTestModule(parentModule.getDeclaredPorts()));
        }

        public CompositeModuleContext build() {
            List<MutablePluginDeclaration<?>> declarations = new ArrayList<>(moduleDeclarationMap.size() + 1);
            moduleDeclarationMap.entrySet().stream()
                .map(
                    entry -> {
                        MutableSimpleModuleDeclaration copiedDeclaration
                            = MutableSimpleModuleDeclaration.copyOfSimpleModuleDeclaration(entry.getValue());
                        copiedDeclaration.getPorts().addAll(ports(childrenPortMaps.get(entry.getKey())));
                        return copiedDeclaration;
                    }
                )
                .forEach(declarations::add);

            boolean isLoopModule = portMap.entrySet().stream()
                .filter(entry
                    -> BareLoopModule.CONTINUE_PORT_NAME.equals(entry.getKey()) || PortType.IO == entry.getValue())
                .findAny()
                .isPresent();
            MutableParentModule<?> parentModule;
            MutableModule<?> testModule;
            if (inlineTestModule) {
                parentModule = isLoopModule
                    ? new MutableLoopModule()
                    : new MutableCompositeModule();
                testModule = parentModule;
            } else {
                assert !isLoopModule;
                MutableCompositeModule typedParentModule = new MutableCompositeModule();
                parentModule = typedParentModule;
                testModule = new MutableProxyModule()
                    .setDeclaration(new MutableQualifiedNamable().setQualifiedName(PACKAGE.join(DECLARATION_NAME)));
                MutableCompositeModuleDeclaration declaration = new MutableCompositeModuleDeclaration()
                    .setSimpleName(DECLARATION_NAME)
                    .setTemplate(typedParentModule);
                declarations.add(declaration);
            }

            MutableBundle bundle = new MutableBundle()
                .setBundleIdentifier(bundleIdentifier)
                .setPackages(Collections.singletonList(
                    new MutablePackage()
                        .setQualifiedName(PACKAGE)
                        .setDeclarations(declarations)
                ));

            MutableCompositeModule enclosingModule = setUpModules(parentModule, testModule);

            LinkerOptions linkerOptions = LinkerOptions.nonExecutable();
            try {
                RuntimeRepository repository
                    = Linker.createRepository(Collections.singletonList(bundle), linkerOptions);
                RuntimeAnnotatedExecutionTrace enclosingExecutionTrace = Linker.createAnnotatedExecutionTrace(
                    ExecutionTrace.empty(), enclosingModule, Collections.emptyList(), repository, linkerOptions);
                return new CompositeModuleContext(
                    repository,
                    enclosingExecutionTrace.resolveContent().resolveModule(MODULE_NAME)
                );
            } catch (LinkerException exception) {
                throw new AssertionError("Invalid test case.", exception);
            }
        }
    }
}
