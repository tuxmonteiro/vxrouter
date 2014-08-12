package lbaas.integrationTest;

import static org.vertx.testtools.VertxAssert.assertEquals;
import static org.vertx.testtools.VertxAssert.testComplete;

import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;

import static org.vertx.testtools.VertxAssert.*;

public class RouteManagerTest extends TestVerticle {

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

	private JsonObject safeExtractJson(String s) {
		JsonObject json = null;
		try {
			json = new JsonObject(s);
		} catch (DecodeException e) {
			System.out.println("The string is not a Json. Test will fail");
		}
		return json;
	}

	@Test
	public void testGetWhenEmpty() {
		// Test GET unknown URI
		vertx.createHttpClient().setPort(9090).getNow("/unknownuri", new Handler<HttpClientResponse>() {
			@Override
			public void handle(HttpClientResponse resp) {
				assertEquals(400, resp.statusCode());

				resp.bodyHandler(new Handler<Buffer>() {
					public void handle(Buffer body) {
						// Expected: { "status_message" : "Bad Request" }
						JsonObject expectedJson = new JsonObject().putString("status_message", "Bad Request");
						JsonObject respJson = safeExtractJson(body.toString());
						assertEquals(expectedJson, respJson);
						testComplete();
					}
				});

			}
		});

		// Test GET /virtualhost
		vertx.createHttpClient().setPort(9090).getNow("/virtualhost", new Handler<HttpClientResponse>() {
			@Override
			public void handle(HttpClientResponse resp) {
				assertEquals(200, resp.statusCode());

				resp.bodyHandler(new Handler<Buffer>() {
					public void handle(Buffer body) {
						// Expected: { "version" : 0, "routes" : [ ] }
						JsonObject expectedJson = new JsonObject()
								.putNumber("version", 0)
								.putArray("routes",
								new JsonArray());

						JsonObject respJson = safeExtractJson(body.toString());
						assertEquals(expectedJson, respJson);
						testComplete();
					}
				});

			}
		});

		// Test GET /virtualhost/id
		vertx.createHttpClient().setPort(9090).getNow("/virtualhost/1234", new Handler<HttpClientResponse>() {
			@Override
			public void handle(HttpClientResponse resp) {
				assertEquals(200, resp.statusCode());

				resp.bodyHandler(new Handler<Buffer>() {
					public void handle(Buffer body) {
						// Expected: { }
						JsonObject expectedJson = new JsonObject();
						JsonObject respJson = safeExtractJson(body.toString());
						assertEquals(expectedJson, respJson);
						testComplete();
					}
				});

			}
		});

		// Test GET /route
		vertx.createHttpClient().setPort(9090).getNow("/route", new Handler<HttpClientResponse>() {
			@Override
			public void handle(HttpClientResponse resp) {
				assertEquals(200, resp.statusCode());

				resp.bodyHandler(new Handler<Buffer>() {
					public void handle(Buffer body) {
						// Expected: { "version" : 0, "routes" : [ ] }
						JsonObject expectedJson = new JsonObject()
							.putNumber("version", 0)
							.putArray("routes", new JsonArray());

						JsonObject respJson = safeExtractJson(body.toString());
						assertEquals(expectedJson, respJson);
						testComplete();
					}
				});

			}
		});

		// Test GET /version
		vertx.createHttpClient().setPort(9090).getNow("/version", new Handler<HttpClientResponse>() {
			@Override
			public void handle(HttpClientResponse resp) {
				assertEquals(200, resp.statusCode());

				resp.bodyHandler(new Handler<Buffer>() {
					public void handle(Buffer body) {
						// Expected: { "version" : 0 }
						JsonObject expectedJson = new JsonObject().putNumber("version", 0);
						JsonObject respJson = safeExtractJson(body.toString());
						assertEquals(expectedJson, respJson);
						testComplete();
					}
				});

			}
		});

	}
	

}
