package lbaas.unit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static lbaas.Constants.SEPARATOR;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import lbaas.Client;
import lbaas.QueueMap;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;
import org.vertx.java.platform.Verticle;

public class TestQueueMap {

    private Verticle verticle;
    private Vertx vertx;
    private Container container;
    private Logger logger;
    private final String messageFormat = "%s%s%s%s%s%s%s%s%s";
    private String virtualhostStr = "test.virtualhost.com";
    private String endpointStr = "0.0.0.0";
    private String portStr = "00";

    private final Map<String, Set<Client>> graphRoutes = new HashMap<>();
    private final Map<String, Set<Client>> badGraphRoutes = new HashMap<>();

    private String buildMessage(String virtualhostStr,
                                String endpointStr,
                                String portStr,
                                String uriStr,
                                String statusStr)
    {
        return String.format(messageFormat,
                virtualhostStr,
                SEPARATOR,
                endpointStr,
                SEPARATOR,
                portStr,
                SEPARATOR,
                statusStr,
                SEPARATOR,
                uriStr);
    }

    @Before
    public void setUp() {
        verticle = mock(Verticle.class);
        vertx = mock(Vertx.class);
        container = mock(Container.class);
        logger = mock(Logger.class);

        when(verticle.getVertx()).thenReturn(vertx);
        when(verticle.getVertx().eventBus()).thenReturn(null);
        when(verticle.getContainer()).thenReturn(container);
        when(verticle.getContainer().logger()).thenReturn(logger);

        graphRoutes.clear();
        badGraphRoutes.clear();
    }

    @Test
    public void insertNewVirtualhostToRouteMap() {
        String uriStr = "/virtualhost";
        String statusStr = "";
        String message = buildMessage(virtualhostStr, endpointStr, portStr, uriStr, statusStr);
        QueueMap queueMap = new QueueMap(verticle, graphRoutes, badGraphRoutes);

        boolean isOk = queueMap.processAddMessage(message);

        assertThat(graphRoutes).containsKey(virtualhostStr);
        assertThat(isOk).isTrue();
    }

    @Test
    public void insertDuplicatedVirtualhostToRouteMap() {
        String uriStr = "/virtualhost";
        String statusStr = "";
        String message = buildMessage(virtualhostStr, endpointStr, portStr, uriStr, statusStr);
        QueueMap queueMap = new QueueMap(verticle, graphRoutes, badGraphRoutes);

        queueMap.processAddMessage(message);
        boolean isOk = queueMap.processAddMessage(message);

        assertThat(graphRoutes).containsKey(virtualhostStr);
        assertThat(isOk).isFalse();
    }

    @Test
    public void removeExistingVirtualhostFromRouteMap() {
        String uriStr = "/virtualhost";
        String statusStr = "";
        String endpointStr = "";
        String portStr = "";
        String message = buildMessage(virtualhostStr, endpointStr, portStr, uriStr, statusStr);
        QueueMap queueMap = new QueueMap(verticle, graphRoutes, badGraphRoutes);

        queueMap.processAddMessage(message);
        boolean isOk = queueMap.processDelMessage(message);

        assertThat(graphRoutes).doesNotContainKey(virtualhostStr);
        assertThat(isOk).isTrue();
    }

    @Test
    public void removeAbsentVirtualhostToRouteMap() {
        String uriStr = "/virtualhost";
        String statusStr = "";
        String endpointStr = "";
        String portStr = "";
        String message = buildMessage(virtualhostStr, endpointStr, portStr, uriStr, statusStr);
        QueueMap queueMap = new QueueMap(verticle, graphRoutes, badGraphRoutes);

        boolean isOk = queueMap.processDelMessage(message);

        assertThat(graphRoutes).doesNotContainKey(virtualhostStr);
        assertThat(isOk).isFalse();
    }
}
