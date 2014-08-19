package lbaas.test.integration.util;

import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonObject;

public class Util {

    public static JsonObject safeExtractJson(String s) {
        JsonObject json = null;
        try {
            json = new JsonObject(s);
        } catch (DecodeException e) {
            System.out.printf("The string %s is not a well-formed Json", s);
        }
        return json;
    }
}
