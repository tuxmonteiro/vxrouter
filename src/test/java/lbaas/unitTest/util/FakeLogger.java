package lbaas.unitTest.util;

import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LogDelegate;

public class FakeLogger extends Logger {

    public FakeLogger(LogDelegate delegate) {
        super(delegate);
    }

    @Override
    public void info(Object message) {
        System.out.println(String.format("[%s: INFO] %s", this, message));
    }

    @Override
    public void warn(Object message) {
        System.out.println(String.format("[%s: WARN] %s", this, message));
    }

    @Override
    public void error(Object message) {
        System.out.println(String.format("[%s: ERR] %s", this, message));
    }

    @Override
    public void debug(Object message) {
        System.out.println(String.format("[%s: DEBUG] %s", this, message));
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
