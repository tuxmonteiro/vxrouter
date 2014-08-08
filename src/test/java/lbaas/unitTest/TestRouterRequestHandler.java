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
//import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpVersion;
import org.vertx.java.core.logging.impl.LogDelegate;
import org.vertx.java.platform.Container;
import org.vertx.java.platform.Verticle;

public class TestRouterRequestHandler {

    private RouterRequestHandler routerRequestHandler;
//    private HttpServerRequest httpServerRequest;
    private Verticle verticle;
    private Vertx vertx;
    private Container container;
    private LogDelegate logDelegate;
    private FakeLogger logger;
    private Server server;
    private Map<String, Virtualhost> virtualhosts;

    @Before
    public void setUp() {
//        httpServerRequest = mock(HttpServerRequest.class);
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
    public void headersWithHttpVersion10() {
        MultiMap headersWithConnectionKeepAlive = new CaseInsensitiveMultiMap();
        MultiMap headersWithConnectionClose = new CaseInsensitiveMultiMap();
        MultiMap headersEmpty = new CaseInsensitiveMultiMap();

        headersWithConnectionKeepAlive.set("Connection", "keep-alive");
        headersWithConnectionClose.set("Connection", "close");


        boolean isKeepAliveWithConnectionKeepAlive = routerRequestHandler.isHttpKeepAlive(headersWithConnectionKeepAlive, HttpVersion.HTTP_1_0);
        boolean isKeepAliveWithConnectionClose = routerRequestHandler.isHttpKeepAlive(headersWithConnectionClose, HttpVersion.HTTP_1_0);
        boolean isKeepAliveWithoutConnectionHeader = routerRequestHandler.isHttpKeepAlive(headersEmpty, HttpVersion.HTTP_1_0);

        assertThat(isKeepAliveWithConnectionKeepAlive).as("isKeepAliveWithConnectionKeepAlive").isTrue();
        assertThat(isKeepAliveWithConnectionClose).as("isKeepAliveWithConnectionClose").isFalse();
        assertThat(isKeepAliveWithoutConnectionHeader).as("isKeepAliveWithoutConnectionHeader").isFalse();

    }

    @Test
    public void headersWithHttpVersion11() {
        MultiMap headersWithConnectionKeepAlive = new CaseInsensitiveMultiMap();
        MultiMap headersWithConnectionClose = new CaseInsensitiveMultiMap();
        MultiMap headersEmpty = new CaseInsensitiveMultiMap();

        headersWithConnectionKeepAlive.set("Connection", "keep-alive");
        headersWithConnectionClose.set("Connection", "close");

        boolean isKeepAliveWithConnectionKeepAlive = routerRequestHandler.isHttpKeepAlive(headersWithConnectionKeepAlive, HttpVersion.HTTP_1_1);
        boolean isKeepAliveWithConnectionClose = routerRequestHandler.isHttpKeepAlive(headersWithConnectionClose, HttpVersion.HTTP_1_1);
        boolean isKeepAliveWithoutConnectionHeader = routerRequestHandler.isHttpKeepAlive(headersEmpty, HttpVersion.HTTP_1_1);

        assertThat(isKeepAliveWithConnectionKeepAlive).as("isKeepAliveWithConnectionKeepAlive").isTrue();
        assertThat(isKeepAliveWithConnectionClose).as("isKeepAliveWithConnectionClose").isFalse();
        assertThat(isKeepAliveWithoutConnectionHeader).as("isKeepAliveWithoutConnectionHeader").isTrue();

    }

}
