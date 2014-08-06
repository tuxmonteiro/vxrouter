package lbaas.unitTest;

import static lbaas.Constants.SEPARATOR;
import static lbaas.Constants.STRING_PATTERN;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static lbaas.unitTest.assertj.custom.VirtualHostAssert.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import lbaas.QueueMap;
import lbaas.Virtualhost;
import lbaas.unitTest.util.FakeLogger;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LogDelegate;
import org.vertx.java.platform.Container;
import org.vertx.java.platform.Verticle;

public class TestQueueMap {

    private Verticle verticle;
    private Vertx vertx;
    private Container container;
    private Logger logger;
    private LogDelegate logDelegate;
    private String virtualhostStr = "test.virtualhost.com";
    private String endpointStr = "0.0.0.0";
    private String portStr = "00";

    private Map<String, Virtualhost> virtualhosts = new HashMap<String, Virtualhost>();

    @Before
    public void setUp() {
        verticle = mock(Verticle.class);
        vertx = mock(Vertx.class);
        container = mock(Container.class);
        logDelegate = mock(LogDelegate.class);
        logger = new FakeLogger(logDelegate);

        when(verticle.getVertx()).thenReturn(vertx);
        when(verticle.getVertx().eventBus()).thenReturn(null);
        when(verticle.getContainer()).thenReturn(container);
        when(verticle.getContainer().logger()).thenReturn(logger);

        virtualhosts.clear();
    }

    @Test
    public void insertNewVirtualhostToRouteMap() {
        String uriStr = "/virtualhost";
        String statusStr = "";
        String message = QueueMap.buildMessage(virtualhostStr, endpointStr, portStr, statusStr, uriStr, "{}");
        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        boolean isOk = queueMap.processAddMessage(message);

        assertThat(virtualhosts).containsKey(virtualhostStr);
        assertThat(isOk).isTrue();
    }

    @Test
    public void insertDuplicatedVirtualhostToRouteMap() {
        String uriStr = "/virtualhost";
        String statusStr = "";
        String message = QueueMap.buildMessage(virtualhostStr, endpointStr, portStr, statusStr, uriStr, "{}");
        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        queueMap.processAddMessage(message);
        boolean isOk = queueMap.processAddMessage(message);

        assertThat(virtualhosts).containsKey(virtualhostStr);
        assertThat(isOk).isFalse();
    }

    @Test
    public void removeExistingVirtualhostFromRouteMap() {
        String uriStr = "/virtualhost";
        String statusStr = "";
        String endpointStr = "";
        String portStr = "";
        String properties = "{}";
        String message = QueueMap.buildMessage(virtualhostStr, endpointStr, portStr, statusStr, uriStr, properties);
        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        queueMap.processAddMessage(message);
        boolean isOk = queueMap.processDelMessage(message);

        assertThat(virtualhosts).doesNotContainKey(virtualhostStr);
        assertThat(isOk).isTrue();
    }

    @Test
    public void removeAbsentVirtualhostFromRouteMap() {
        String uriStr = "/virtualhost";
        String statusStr = "";
        String endpointStr = "";
        String portStr = "";
        String properties = "{}";
        String message = QueueMap.buildMessage(virtualhostStr, endpointStr, portStr, statusStr, uriStr, properties);
        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        boolean isOk = queueMap.processDelMessage(message);

        assertThat(virtualhosts).doesNotContainKey(virtualhostStr);
        assertThat(isOk).isFalse();
    }

    @Test
    public void insertNewRealToExistingVirtualhostSet() {
        String statusStr = "";
        String endpointStrWithPort = String.format("%s:%s", endpointStr, portStr);
        String messageVirtualhost = QueueMap.buildMessage(virtualhostStr, "", "", "", "/virtualhost", "{}");
        String messageReal = QueueMap.buildMessage(virtualhostStr, endpointStr, portStr, statusStr, "/real", "{}");
        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        boolean isOkVirtualhost = queueMap.processAddMessage(messageVirtualhost);
        boolean isOkReal = queueMap.processAddMessage(messageReal);
        Virtualhost virtualhost = virtualhosts.get(virtualhostStr);

        assertThat(virtualhosts).containsKey(virtualhostStr);
        assertThat(virtualhost).containsReal(endpointStrWithPort, !"0".equals(statusStr));
        assertThat(isOkVirtualhost).as("isOkVirtualhost").isTrue();
        assertThat(isOkReal).as("isOkReal").isTrue();
    }

    @Test
    public void insertNewRealToAbsentVirtualhostSet() {
        String statusStr = "";
        String messageReal = QueueMap.buildMessage(virtualhostStr, endpointStr, portStr, statusStr, "/real", "{}");
        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        boolean isOk = queueMap.processAddMessage(messageReal);

        assertThat(virtualhosts).doesNotContainKey(virtualhostStr);
        assertThat(isOk).isFalse();
    }

    @Test
    public void insertDuplicatedRealToExistingVirtualhostSet() {
        String statusStr = "";
        String endpointStrWithPort = String.format("%s:%s", endpointStr, portStr);
        String messageVirtualhost = QueueMap.buildMessage(virtualhostStr, "", "", "", "/virtualhost", "{}");
        String messageReal = QueueMap.buildMessage(virtualhostStr, endpointStr, portStr, statusStr, "/real", "{}");
        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        boolean isOkVirtualhost = queueMap.processAddMessage(messageVirtualhost);
        boolean isOkRealAdd = queueMap.processAddMessage(messageReal);
        boolean isOkRealAddAgain = queueMap.processAddMessage(messageReal);
       Virtualhost virtualhost = virtualhosts.get(virtualhostStr);

        assertThat(virtualhosts).containsKey(virtualhostStr);
        assertThat(virtualhost).containsReal(endpointStrWithPort, !"0".equals(statusStr));
        assertThat(isOkVirtualhost).as("isOkVirtualhost").isTrue();
        assertThat(isOkRealAdd).as("isOkRealAdd").isTrue();
        assertThat(isOkRealAddAgain).as("isOkRealRemove").isFalse();
    }

    @Test
    public void removeExistingRealFromExistingVirtualhostSet() throws UnsupportedEncodingException {
        String statusStr = "";
        String endpointStrWithPort = String.format("%s:%s", endpointStr, portStr);
        String messageVirtualhost = QueueMap.buildMessage(virtualhostStr, "", "", "", "/virtualhost", "{}");
        String messageReal = QueueMap.buildMessage(virtualhostStr,
                                                   endpointStr,
                                                   portStr,
                                                   statusStr,
                                                   String.format("/real/%s", URLEncoder.encode(endpointStrWithPort,"UTF-8")),
                                                   "{}");
        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        boolean isOkVirtualhost = queueMap.processAddMessage(messageVirtualhost);
        boolean isOkRealAdd = queueMap.processAddMessage(messageReal);
        boolean isOkRealRemove = queueMap.processDelMessage(messageReal);
        Virtualhost virtualhost = virtualhosts.get(virtualhostStr);

        assertThat(virtualhosts).containsKey(virtualhostStr);
        assertThat(virtualhost).doesNotContainsReal(endpointStrWithPort, !"0".equals(statusStr));
        assertThat(isOkVirtualhost).as("isOkVirtualhost").isTrue();
        assertThat(isOkRealAdd).as("isOkRealAdd").isTrue();
        assertThat(isOkRealRemove).as("isOkRealRemove").isTrue();
    }

    @Test
    public void removeRealFromAbsentVirtualhostSet() throws UnsupportedEncodingException {
        String statusStr = "";
        String endpointStrWithPort = String.format("%s:%s", endpointStr, portStr);
        String messageReal = QueueMap.buildMessage(virtualhostStr,
                                                    endpointStr,
                                                    portStr,
                                                    statusStr,
                                                    String.format("/real/%s", URLEncoder.encode(endpointStrWithPort,"UTF-8")),
                                                    "{}");
        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        boolean isOk = queueMap.processDelMessage(messageReal);

        assertThat(virtualhosts).doesNotContainKey(virtualhostStr);
        assertThat(isOk).isFalse();
    }

    @Test
    public void removeAbsentRealFromVirtualhostSet() throws UnsupportedEncodingException {
        String statusStr = "";
        String endpointStrWithPort = String.format("%s:%s", endpointStr, portStr);
        String messageVirtualhost = QueueMap.buildMessage(virtualhostStr, "", "", "", "/virtualhost", "{}");
        String messageReal = QueueMap.buildMessage(virtualhostStr,
                                                   endpointStr,
                                                   portStr,
                                                   statusStr,
                                                   String.format("/real/%s", URLEncoder.encode(endpointStrWithPort,"UTF-8")),
                                                   "{}");
        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        boolean isOkVirtualhost = queueMap.processAddMessage(messageVirtualhost);
        boolean isOkRealRemove = queueMap.processDelMessage(messageReal);
        Virtualhost virtualhost = virtualhosts.get(virtualhostStr);

        assertThat(virtualhosts).containsKey(virtualhostStr);
        assertThat(virtualhost).doesNotContainsReal(endpointStrWithPort, !"0".equals(statusStr));
        assertThat(isOkVirtualhost).as("isOkVirtualhost").isTrue();
        assertThat(isOkRealRemove).as("isOkRealRemove").isFalse();
    }

    @Test
    public void removeAllRoutes() {
        String statusStr = "";
        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        for (int idVirtualhost=0; idVirtualhost<10; idVirtualhost++) {

            String aVirtualhostStr = String.format("%d%s", idVirtualhost, virtualhostStr);
            String messageVirtualhost = QueueMap.buildMessage(
                    aVirtualhostStr, "", "", "", "/virtualhost", "{}");

            queueMap.processAddMessage(messageVirtualhost);

            for (int idReal=0; idReal<10; idReal++) {
                String messageReal = QueueMap.buildMessage(
                        aVirtualhostStr, endpointStr, String.format("%d", idReal), statusStr, "/real","{}");
                queueMap.processAddMessage(messageReal);
            }
        }
        String messageDelRoutes = QueueMap.buildMessage("", "", "", "", "/route", "{}");
        queueMap.processDelMessage(messageDelRoutes);

        assertThat(virtualhosts).hasSize(0);
    }

    @Test
    public void validateBuildMessage() {
        String statusStr = "";
        String uriStr = "/test";
        JsonObject properties = new JsonObject("{}");

        String message = QueueMap.buildMessage(virtualhostStr,
                                               endpointStr,
                                               portStr,
                                               statusStr,
                                               uriStr,
                                               properties.toString());

        assertThat(message).isEqualTo(String.format(STRING_PATTERN,
                                                    virtualhostStr,
                                                    SEPARATOR,
                                                    endpointStr,
                                                    SEPARATOR,
                                                    portStr,
                                                    SEPARATOR,
                                                    statusStr,
                                                    SEPARATOR,
                                                    uriStr,
                                                    SEPARATOR,
                                                    properties
                                                    ));
    }
}
