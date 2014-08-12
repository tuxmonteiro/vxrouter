package lbaas.test.unit.util;

import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LogDelegate;

public class FakeLogger extends Logger {

    private boolean quiet;
    private String testId;

    public FakeLogger(LogDelegate delegate) {
        super(delegate);
        this.quiet = false;
    }

    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    @Override
    public void info(Object message) {
        if (!quiet) {
            System.out.println(String.format("[%s (%s): INFO] %s", this, testId, message));
        }
    }

    @Override
    public void warn(Object message) {
        if (!quiet) {
            System.out.println(String.format("[%s (%s): WARN] %s", this, testId, message));
        }
    }

    @Override
    public void error(Object message) {
        if (!quiet) {
            System.out.println(String.format("[%s (%s): ERR] %s", this, testId, message));
        }
    }

    @Override
    public void debug(Object message) {
        if (!quiet) {
            System.out.println(String.format("[%s (%s): DEBUG] %s", this, testId, message));
        }
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
