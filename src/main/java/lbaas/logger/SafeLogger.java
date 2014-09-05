package lbaas.logger;

import org.vertx.java.core.logging.Logger;

public class SafeLogger {

    public enum LogLevel {
        FATAL,
        ERROR,
        WARN,
        INFO,
        DEBUG,
        TRACE,
        UNDEF
    }

    private Logger log = null;
    private LogLevel level = LogLevel.UNDEF;

    public SafeLogger setLogger(final Logger log) {
        this.log=log;
        return this;
    }

    private void logPrintln(LogLevel logLevel, final Object message, final Throwable t) {
        if (logLevel==null) logLevel = LogLevel.UNDEF;
        System.err.println(String.format("[%s] %s", logLevel.toString(), message));
        if (t!=null) t.printStackTrace();
    }

    public void fatal(final Object message) {
        level = LogLevel.FATAL;
        if (log==null) {
            logPrintln(level, message, null);
            return;
        }
        log.fatal(message);
    }

    public void fatal(final Object message, final Throwable t) {
        level = LogLevel.FATAL;
        if (log==null) {
            logPrintln(level, message, t);
            return;
        }
        log.fatal(message, t);
    }

    public void error(final Object message) {
        level = LogLevel.ERROR;
        if (log==null) {
            logPrintln(level, message, null);
            return;
        }
        log.error(message);
    }

    public void error(final Object message, final Throwable t) {
        level = LogLevel.ERROR;
        if (log==null) {
            logPrintln(level, message, t);
            return;
        }
        log.error(message, t);
    }

    public void warn(final Object message) {
        level = LogLevel.WARN;
        if (log==null) {
            logPrintln(level, message, null);
            return;
        }
        log.warn(message);
    }

    public void warn(final Object message, final Throwable t) {
        level = LogLevel.WARN;
        if (log==null) {
            logPrintln(level, message, t);
            return;
        }
        log.warn(message, t);
    }

    public void info(final Object message) {
        level = LogLevel.INFO;
        if (log==null) {
            logPrintln(level, message, null);
            return;
        }
        log.info(message);
    }

    public void info(final Object message, final Throwable t) {
        level = LogLevel.INFO;
        if (log==null) {
            logPrintln(level, message, t);
            return;
        }
        log.info(message, t);
    }

    public void debug(final Object message) {
        level = LogLevel.DEBUG;
        if (log==null) {
            logPrintln(level, message, null);
            return;
        }
        log.debug(message);
    }

    public void debug(final Object message, final Throwable t) {
        level = LogLevel.DEBUG;
        if (log==null) {
            logPrintln(level, message, t);
            return;
        }
        log.debug(message, t);
    }

    public void trace(final Object message) {
        level = LogLevel.TRACE;
        if (log==null) {
            logPrintln(level, message, null);
            return;
        }
        log.trace(message);
    }

    public void trace(final Object message, final Throwable t) {
        level = LogLevel.TRACE;
        if (log==null) {
            logPrintln(level, message, t);
            return;
        }
        log.trace(message, t);
    }

    public LogLevel getLastLogLevel() {
        return this.level;
    }

}
