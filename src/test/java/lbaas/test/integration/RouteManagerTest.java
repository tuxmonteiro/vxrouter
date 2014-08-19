package lbaas.test.integration;
import lbaas.test.integration.util.Action;
import lbaas.test.integration.util.UtilTestVerticle;

import org.junit.Test;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class RouteManagerTest extends UtilTestVerticle {
	
	@Test
    public void testWhenEmptyGetUnknownURI() {
        // Test GET unknown URI
        // Expected: { "status_message" : "Bad Request" }
    	newGet().onPort(9090).atUri("/unknownuri").expectCode(400).expectJson("{\"status_message\": \"Bad Request\"}").run();
    }
    
    @Test
    public void testWhenEmptyGetVHost() {
      // Test GET /virtualhost
      // Expected: { "version" : 0, "routes" : [ ] }
        JsonObject expectedJson = new JsonObject().putNumber("version", 0).putArray("routes", new JsonArray());
        newGet().onPort(9090).atUri("/virtualhost").expectJson(expectedJson).run();
    }

    @Test
    public void testWhenEmptyGetVHostId() {
      // Test GET /virtualhost/id
      // Expected: { }
      newGet().onPort(9090).atUri("/virtualhost/1234").expectJson(new JsonObject()).run();;
    }
    
    @Test
    public void testWhenEmptyGetRoute() {
    	// Test GET /route
    	// Expected: { "version" : 0, "routes" : [ ] }
    	JsonObject expectedJson = new JsonObject().putNumber("version", 0).putArray("routes", new JsonArray());
    	newGet().onPort(9090).atUri("/route").expectJson(expectedJson).run();;
    }
    
    @Test
    public void testWhenEmptyGetVersion() {
      // Test GET /version
      // Expected: { "version" : 0 }
    	newGet().onPort(9090).atUri("/version").expectJson("{ \"version\" : 0 }").run();
    }

    // Test POST /virtualhost
    @Test
    public void testWhenEmptyPostVHost() {
        JsonObject vhostJson = new JsonObject().putString("name", "test.localdomain");
        JsonObject expectedJson = new JsonObject().putString("status_message", "OK");

        Action action1 = newPost().onPort(9090).setBodyJson(vhostJson).atUri("/virtualhost").expectJson(expectedJson);
        
        JsonObject getExpectedJson = new JsonObject()
            .putString("name", "test.localdomain")
            .putObject("properties", new JsonObject())
            .putArray("backends", new JsonArray())
            .putArray("badBackends", new JsonArray());
        
        final Action action2 = newGet().onPort(9090).atUri("/virtualhost/test.localdomain").expectJson(getExpectedJson).after(action1);
        action2.setDontStop(true);
        

    	getVertx().eventBus().registerHandler("ended.action", new Handler<Message<String>>() {
    		@Override
    		public void handle(Message<String> message) {
    			if (message.body().equals(action2.id())) {
    				System.out.println("Testing other things after action2");
    				testCompleteWrapper();
    			}
    		};
		});

    	action1.run();

    }

}
