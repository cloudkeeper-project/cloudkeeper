package com.svbio.cloudkeeper.maven;

import com.svbio.cloudkeeper.model.api.util.RecursiveDeleteVisitor;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.SyncContext;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ITFileLockSyncContextFactory {
    private static final int REGISTRY_PORT = 1099;
    private static final String CONTROLLER = "controller";
    private static final long MAX_AWAIT_MS = 10000;
    private static final long AWAIT_POLLING_INTERVAL_MS = 100;

    private Path tempDir;
    private Path lockFile;
    private FileLockSyncContextFactory syncContextFactory;
    private final RepositorySystemSession repositorySystemSession = Mockito.mock(RepositorySystemSession.class);

    @BeforeClass
    public void setup() throws IOException {
        tempDir = Files.createTempDirectory(getClass().getName());
        lockFile = tempDir.resolve("lockFile");
        syncContextFactory = new FileLockSyncContextFactory(lockFile);
    }

    @AfterClass
    public void tearDown() throws IOException {
        syncContextFactory.close();
        Files.walkFileTree(tempDir, RecursiveDeleteVisitor.getInstance());
    }

    private interface Controller extends Remote {
        void acquire() throws RemoteException;
        void release() throws RemoteException;
        void shutdown() throws RemoteException;
        State getState() throws RemoteException;
    }

    private static final class LocalController implements Controller {
        private final CountDownLatch acquireSignal = new CountDownLatch(1);
        private final CountDownLatch releaseSignal = new CountDownLatch(1);
        private final CountDownLatch shutdownSignal = new CountDownLatch(1);
        private volatile State state = State.STARTED;

        private void awaitSignalToAcquire() throws InterruptedException {
            state = State.AWAITING_ACQUIRE_SIGNAL;
            acquireSignal.await();
            state = State.AWAITING_ACQUIRE;
        }

        private void awaitSignalToRelease() throws InterruptedException {
            state = State.AWAITING_RELEASE_SIGNAL;
            releaseSignal.await();
        }

        private void awaitSignalToShutdown() throws InterruptedException {
            shutdownSignal.await();
        }

        private void released() {
            state = State.RELEASED;
        }

        @Override
        public void acquire() {
            acquireSignal.countDown();
        }

        @Override
        public void release() {
            releaseSignal.countDown();
        }

        @Override
        public void shutdown() {
            shutdownSignal.countDown();
        }

        @Override
        public State getState() {
            return state;
        }
    }

    enum State {
        STARTED,
        AWAITING_ACQUIRE_SIGNAL,
        AWAITING_ACQUIRE,
        AWAITING_RELEASE_SIGNAL,
        RELEASED
    }

    private static final class SyncTask implements Runnable {
        private final FileLockSyncContextFactory syncContextFactory;
        private final LocalController controller;
        private final RepositorySystemSession repositorySystemSession;

        private SyncTask(FileLockSyncContextFactory syncContextFactory, LocalController controller,
                RepositorySystemSession repositorySystemSession) {
            this.syncContextFactory = syncContextFactory;
            this.controller = controller;
            this.repositorySystemSession = repositorySystemSession;
        }

        @Override
        public void run() {
            try (SyncContext syncContext = syncContextFactory.newInstance(repositorySystemSession, false)) {
                controller.awaitSignalToAcquire();
                syncContext.acquire(Collections.emptyList(), Collections.emptyList());
                controller.awaitSignalToRelease();
            } catch (InterruptedException exception) {
                Assert.fail("Unexpected InterruptedException.", exception);
            }

            controller.released();

            try {
                controller.awaitSignalToShutdown();
            } catch (InterruptedException exception) {
                Assert.fail("Unexpected InterruptedException.", exception);
            }
        }
    }

    private static void awaitState(State state, Controller controller) throws InterruptedException, RemoteException {
        long waited = 0;
        do {
            Thread.sleep(AWAIT_POLLING_INTERVAL_MS);
            waited += AWAIT_POLLING_INTERVAL_MS;
            State controllerState = controller.getState();
            if (state == controllerState) {
                return;
            } else if (state.compareTo(controllerState) < 0) {
                Assert.fail(String.format(
                    "Synchronization did not work. Controller state is %s, but should only be %s.",
                    controllerState, state
                ));
            }
        } while (waited < MAX_AWAIT_MS);
    }

    private static Controller awaitController() throws RemoteException, InterruptedException {
        Registry registry = LocateRegistry.getRegistry(REGISTRY_PORT);
        long waited = 0;
        do {
            Thread.sleep(AWAIT_POLLING_INTERVAL_MS);
            waited += AWAIT_POLLING_INTERVAL_MS;
            try {
                return (Controller) registry.lookup(CONTROLLER);
            } catch (RemoteException | NotBoundException ignored) { }
        } while (waited < MAX_AWAIT_MS);
        throw new AssertionError("awaitController() failed");
    }

    private void test(Controller controller) throws IOException, InterruptedException {
        SyncContext syncContext = syncContextFactory.newInstance(repositorySystemSession, false);
        awaitState(State.AWAITING_ACQUIRE_SIGNAL, controller);
        syncContext.acquire(Collections.emptyList(), Collections.emptyList());
        controller.acquire();
        awaitState(State.AWAITING_ACQUIRE, controller);
        syncContext.close();
        awaitState(State.AWAITING_RELEASE_SIGNAL, controller);
        controller.release();
        awaitState(State.RELEASED, controller);
        controller.shutdown();
    }

    @Test
    public void testLocally() throws IOException, InterruptedException {
        LocalController localController = new LocalController();
        SyncTask syncTask = new SyncTask(syncContextFactory, localController, repositorySystemSession);
        Thread thread = new Thread(syncTask, syncTask.getClass().getSimpleName());
        thread.start();
        test(localController);
        thread.join();
    }

    @Test
    public void testSeparateJVMs() throws IOException, InterruptedException, NotBoundException {
        @Nullable Process process = new ProcessBuilder(forkedJVMCommandLine()).inheritIO().start();
        try {
            test(awaitController());
            Assert.assertEquals(process.waitFor(MAX_AWAIT_MS, TimeUnit.MILLISECONDS), true,
                "Forked process did not exit.");
            Assert.assertEquals(process.exitValue(), 0);
        } finally {
            process.destroyForcibly();
        }
    }

    /**
     * Returns the command line for running {@link #main} in a new JVM.
     *
     * @return the command line
     */
    private List<String> forkedJVMCommandLine() {
        Path javaPath = Paths.get(System.getProperty("java.home")).resolve("bin").resolve("java");
        List<String> command = new ArrayList<>();
        command.add(javaPath.toString());
        command.add("-enableassertions");
        command.add("-classpath");
        command.add(System.getProperty("java.class.path"));
        // Uncomment the following line to attach a debugger to the forked JVM
        // command.add("-agentlib:jdwp=transport=dt_socket,quiet=y,server=y,suspend=y,address=5005");
        command.add(ITFileLockSyncContextFactory.class.getName());
        command.add(lockFile.toString());
        return command;
    }

    /**
     * Entry point for the JVM forked by {@link #testSeparateJVMs()}.
     */
    public static void main(String[] arguments) throws IOException, AlreadyBoundException, NotBoundException {
        LocalController localController = new LocalController();

        Registry registry = LocateRegistry.createRegistry(REGISTRY_PORT);
        Controller controller = (Controller) UnicastRemoteObject.exportObject(localController, 0);
        registry.bind(CONTROLLER, controller);

        Path lockFile = Paths.get(arguments[0]);
        RepositorySystemSession repositorySystemSession = Mockito.mock(RepositorySystemSession.class);
        try (FileLockSyncContextFactory syncContextFactory = new FileLockSyncContextFactory(lockFile)) {
            new SyncTask(syncContextFactory, localController, repositorySystemSession).run();
        }

        UnicastRemoteObject.unexportObject(localController, true);
        UnicastRemoteObject.unexportObject(registry, true);
    }
}
