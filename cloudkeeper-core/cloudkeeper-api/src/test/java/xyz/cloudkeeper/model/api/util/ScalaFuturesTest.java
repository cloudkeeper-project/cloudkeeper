package xyz.cloudkeeper.model.api.util;

import akka.dispatch.ExecutionContexts;
import akka.dispatch.Futures;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.util.Failure;
import scala.util.Try;
import xyz.cloudkeeper.model.util.ImmutableList;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScalaFuturesTest {
    private ExecutorService executorService;
    private ExecutionContext executionContext;

    @BeforeTest
    public void setup() {
        executorService = Executors.newCachedThreadPool();
        executionContext = ExecutionContexts.fromExecutorService(executorService);
    }

    @AfterTest
    public void tearDown() {
        executorService.shutdown();
    }

    private static final class ExpectedException extends RuntimeException {
        private static final long serialVersionUID = -1374943519055881413L;

        private ExpectedException(String message) {
            super(message);
        }
    }

    @Test
    public void testCreateListFuture() throws Exception {
        Future<Integer> integerFuture = Futures.future(() -> {
            Thread.sleep(50);
            return 1;
        }, executionContext);
        Future<ImmutableList<Integer>> future = ScalaFutures.createListFuture(Arrays.asList(
            Futures.failed(new ExpectedException("This is the primary exception.")),
            Futures.failed(new ExpectedException("This is a suppressed exception.")),
            integerFuture
        ), executionContext);
        Future<Try<ImmutableList<Integer>>> tryFuture = ScalaFutures.tryFuture(future, executionContext);
        Try<ImmutableList<Integer>> tryList = Await.result(tryFuture, Duration.Inf());
        Assert.assertTrue(integerFuture.isCompleted());
        ExpectedException exception = (ExpectedException) ((Failure<?>) tryList).exception();
        Assert.assertTrue("This is the primary exception.".equals(exception.getMessage()));
        Throwable[] suppressedExceptions = exception.getSuppressed();
        Assert.assertEquals(suppressedExceptions.length, 1);
        Assert.assertTrue("This is a suppressed exception.".equals(suppressedExceptions[0].getMessage()));
    }
}
