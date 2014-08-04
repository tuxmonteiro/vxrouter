package lbaas.unitTest;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import java.util.Map;

import lbaas.Server;
import lbaas.Virtualhost;
import lbaas.handlers.RouterRequestHandler;
import lbaas.unitTest.util.FakeLogger;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.CaseInsensitiveMultiMap;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpVersion;
import org.vertx.java.core.logging.impl.LogDelegate;
import org.vertx.java.platform.Container;
import org.vertx.java.platform.Verticle;

public class TestRouterRequestHandler {

    private RouterRequestHandler routerRequestHandler;
    private HttpServerRequest httpServerRequest;
    private Verticle verticle;
    private Vertx vertx;
    private Container container;
    private LogDelegate logDelegate;
    private FakeLogger logger;
    private Server server;
    private Map<String, Virtualhost> virtualhosts;

    @Before
    public void setUp() {
        httpServerRequest = mock(HttpServerRequest.class);
        verticle = mock(Verticle.class);
        vertx = mock(Vertx.class);
        container = mock(Container.class);
        logDelegate = mock(LogDelegate.class);
        logger = new FakeLogger(logDelegate);

        when(verticle.getVertx()).thenReturn(vertx);
        when(verticle.getVertx().eventBus()).thenReturn(null);
        when(verticle.getContainer()).thenReturn(container);
        when(verticle.getContainer().logger()).thenReturn(logger);

        routerRequestHandler = new RouterRequestHandler(vertx, container, virtualhosts, server, null);
    }

    @Test
    public void isHttpKeepAliveEnabledWithConnectionHeaderAndHttpVersion10() {
        MultiMap headers = new CaseInsensitiveMultiMap();
        headers.set("Connection", "keep-alive");

        boolean isKeepAlive = routerRequestHandler.isHttpKeepAlive(headers, HttpVersion.HTTP_1_0);

        assertThat(isKeepAlive).as("isKeepAlive").isTrue();
    }

    @Test
    public void isHttpKeepAliveEnabledWithoutConnectionHeaderAndHttpVersion10() {
        MultiMap headers = new CaseInsensitiveMultiMap();

        boolean isKeepAlive = routerRequestHandler.isHttpKeepAlive(headers, HttpVersion.HTTP_1_0);

        assertThat(isKeepAlive).as("isKeepAlive").isFalse();
    }

    @Test
    public void isHttpKeepAliveEnabledWithConnectionHeaderAndHttpVersion11() {
        MultiMap headers = new CaseInsensitiveMultiMap();
        headers.set("Connection", "keep-alive");

        boolean isKeepAlive = routerRequestHandler.isHttpKeepAlive(headers, HttpVersion.HTTP_1_1);

        assertThat(isKeepAlive).as("isKeepAlive").isTrue();
    }

    @Test
    public void isHttpKeepAliveEnabledWithoutConnectionHeaderAndHttpVersion11() {
        MultiMap headers = new CaseInsensitiveMultiMap();

        boolean isKeepAlive = routerRequestHandler.isHttpKeepAlive(headers, HttpVersion.HTTP_1_1);

        assertThat(isKeepAlive).as("isKeepAlive").isTrue();
    }
}
