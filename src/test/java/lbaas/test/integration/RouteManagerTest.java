package lbaas.test.integration;

import lbaas.test.integration.util.TestMoreVerticle;

import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import static org.vertx.testtools.VertxAssert.*;

public class RouteManagerTest extends TestMoreVerticle {

    @Override
    public void start() {
        System.out.println("Starting module: " + System.getProperty("vertx.modulename"));
        // Make sure we call initialize() - this sets up the assert stuff so
        // assert functionality works correctly
        initialize();
        // Deploy the module - the System property `vertx.modulename` will
        // contain the name of the module so you
        // don't have to hardecode it in your tests
        container.deployModule(System.getProperty("vertx.modulename"), new AsyncResultHandler<String>() {
            @Override
            public void handle(AsyncResult<String> asyncResult) {
                // Deployment is asynchronous and this this handler will
                // be called when it's complete (or failed)
                assertTrue(asyncResult.succeeded());
                assertNotNull("deploymentID should not be null", asyncResult.result());
                // If deployed correctly then start the tests!
                // TODO: better way to check that
                System.out.println("Waiting 1s for everything to start");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println("Interrupted");
                }
                System.out.println("Starting tests!!!");
                startTests();
            }
        });
    }


    @Test
    public void testGetWhenEmpty() {
        // Test GET unknown URI
        // Expected: { "status_message" : "Bad Request" }
        JsonObject expectedJson = new JsonObject().putString("status_message", "Bad Request");
        getAndTest(9090, "/unknownuri", 400, expectedJson);
        
        // Test GET /virtualhost
        // Expected: { "version" : 0, "routes" : [ ] }
        expectedJson = new JsonObject().putNumber("version", 0).putArray("routes", new JsonArray());
        getAndTest(9090, "/virtualhost", 200, expectedJson);

        // Test GET /virtualhost/id
        // Expected: { }
        expectedJson = new JsonObject();
        getAndTest(9090, "/virtualhost/1234", 200, expectedJson);
        
        // Test GET /route
        // Expected: { "version" : 0, "routes" : [ ] }
        expectedJson = new JsonObject().putNumber("version", 0).putArray("routes", new JsonArray());
        getAndTest(9090, "/route", 200, expectedJson);
        
        // Test GET /version
        // Expected: { "version" : 0 }
        expectedJson = new JsonObject().putNumber("version", 0);
        getAndTest(9090, "/route", 200, expectedJson);
    }

    // Test POST /virtualhost
    @Test
    public void testPostVHostWhenEmpty() {
        // { "name": "www.globo.com" }
        JsonObject vhostJson = new JsonObject().putString("name", "test.localdomain");
        
        // Expected: { "status_message" : "OK" }
        JsonObject expectedJson = new JsonObject().putString("status_message", "OK");

        // Expected: {
        //      "name" : "test.localdomain",
        //      "backends" : [ ],
        //      "badBackends" : [ ]
        //    }
        JsonObject getExpectedJson = new JsonObject()
            .putString("name", "test.localdomain")
            .putObject("properties", new JsonObject())
            .putArray("backends", new JsonArray())
            .putArray("badBackends", new JsonArray());

        JsonObject nextMethodParams = new JsonObject()
            .putNumber("port", 9090)
            .putString("uri", "/virtualhost/test.localdomain")
            .putNumber("expectedCode", 200)
            .putObject("expectedJson", getExpectedJson);
        
        postAndTestMore(9090, "/virtualhost", vhostJson, 200, expectedJson, "getAndTest", nextMethodParams);
        
    }

}
