/** Copyright Valkey GLIDE Project Contributors - SPDX Identifier: Apache-2.0 */
package glide.api.logging;

import static glide.ffi.resolvers.LoggerResolver.initInternal;
import static glide.ffi.resolvers.LoggerResolver.logInternal;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.NonNull;

/**
 * A singleton class that allows logging which is consistent with logs from the internal GLIDE core.
 * The logger can be set up in 2 ways -
 *
 * <ol>
 *   <li>By calling <code>Logger.init</code>, which configures the logger only if it wasn't
 *       previously configured.
 *   <li>By calling <code>Logger.setLoggerConfig</code>, which replaces the existing configuration,
 *       and means that new logs will not be saved with the logs that were sent before the call.
 * </ol>
 *
 * If none of these functions are called, the first log attempt will initialize a new logger with
 * default configuration.
 */
public final class Logger {
    @Getter
    public enum Level {
        DEFAULT(-1),
        ERROR(0),
        WARN(1),
        INFO(2),
        DEBUG(3),
        TRACE(4),
        OFF(5);

        private final int level;

        Level(int level) {
            this.level = level;
        }

        public static Level fromInt(int i) {
            switch (i) {
                case 0:
                    return ERROR;
                case 1:
                    return WARN;
                case 2:
                    return INFO;
                case 3:
                    return DEBUG;
                case 4:
                    return TRACE;
                case 5:
                    return OFF;
                default:
                    return DEFAULT;
            }
        }
    }

    @Getter private static Level loggerLevel;

    private static void initLogger(@NonNull Level level, String fileName) {
        loggerLevel = Level.fromInt(initInternal(level.getLevel(), fileName));
    }

    /**
     * Initialize a logger if it wasn't initialized before - this method is meant to be used when
     * there is no intention to replace an existing logger. The logger will filter all logs with a
     * level lower than the given level.
     *
     * @param level Set the logger level to one of <code>
     *     [DEFAULT, ERROR, WARN, INFO, DEBUG, TRACE, OFF]</code>. To turn off logging completely, set
     *     the level to {@link Level#OFF}.
     * @param fileName The target of the logs will be the file mentioned.
     */
    public static void init(@NonNull Level level, String fileName) {
        if (loggerLevel == null) {
            initLogger(level, fileName);
        }
    }

    /**
     * Initialize a logger if it wasn't initialized before - this method is meant to be used when
     * there is no intention to replace an existing logger. The logger will filter all logs with a
     * level lower than the default level decided by Glide core. All logs will be printed to the
     * console.
     *
     * @param fileName The target of the logs will be the file mentioned.
     */
    public static void init(@NonNull String fileName) {
        init(Level.DEFAULT, fileName);
    }

    /**
     * Initialize a logger if it wasn't initialized before - this method is meant to be used when
     * there is no intention to replace an existing logger. The logger will filter all logs with a
     * level lower than the default level decided by Glide core. The logs will be written to the
     * console.
     */
    public static void init() {
        init(Level.DEFAULT, null);
    }

    /**
     * Initialize a logger if it wasn't initialized before - this method is meant to be used when
     * there is no intention to replace an existing logger. The logger will filter all logs with a
     * level lower than the given level. The logs will be written to the console.
     *
     * @param level Set the logger level to one of <code>[DEFAULT, ERROR, WARN, INFO, DEBUG, TRACE]
     *     </code>. To turn off logging completely, set the level to {@link Level#OFF}.
     */
    public static void init(@NonNull Level level) {
        init(level, null);
    }

    /**
     * Logs the provided message if the provided log level is lower than the logger level. This
     * overload takes a <code>Supplier</code> to lazily construct the message.
     *
     * @param level The log level of the provided message.
     * @param logIdentifier The log identifier should give the log a context.
     * @param messageSupplier The <code>Supplier</code> of the message to log.
     */
    public static void log(
            @NonNull Level level,
            @NonNull String logIdentifier,
            @NonNull Supplier<String> messageSupplier) {
        log(level, logIdentifier, messageSupplier.get());
    }

    /**
     * Logs the provided message if the provided log level is lower than the logger level.
     *
     * @param level The log level of the provided message.
     * @param logIdentifier The log identifier should give the log a context.
     * @param message The message to log.
     */
    public static void log(
            @NonNull Level level, @NonNull String logIdentifier, @NonNull String message) {
        if (loggerLevel == null) {
            initLogger(Level.DEFAULT, null);
        }

        if (level == Level.OFF) {
            return;
        }

        if (!(level.getLevel() <= loggerLevel.getLevel())) {
            return;
        }
        logInternal(level.getLevel(), logIdentifier, message);
    }

    /**
     * Logs the provided exception or error if the provided log level is lower than the logger level.
     *
     * @param level The log level of the provided message.
     * @param logIdentifier The log identifier should give the log a context.
     * @param message The message to log with the exception.
     * @param throwable The exception or error to log.
     */
    public static void log(
            @NonNull Level level,
            @NonNull String logIdentifier,
            @NonNull String message,
            @NonNull Throwable throwable) {
        log(level, logIdentifier, message + ": " + prettyPrintException(throwable));
    }

    /**
     * Logs the provided exception or error if the provided log level is lower than the logger level.
     * This overload takes a <code>Supplier</code> to lazily construct the message.
     *
     * @param level The log level of the provided message.
     * @param logIdentifier The log identifier should give the log a context.
     * @param messageSupplier The <code>Supplier</code> of the message to log.
     * @param throwable The exception or error to log.
     */
    public static void log(
            @NonNull Level level,
            @NonNull String logIdentifier,
            @NonNull Supplier<String> messageSupplier,
            @NonNull Throwable throwable) {
        log(level, logIdentifier, messageSupplier.get() + ": " + prettyPrintException(throwable));
    }

    private static String prettyPrintException(@NonNull Throwable throwable) {
        try (StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter)) {
            throwable.printStackTrace(printWriter);
            return stringWriter.toString();
        } catch (IOException e) {
            // This can't happen with a ByteArrayOutputStream.
            return null;
        }
    }

    /**
     * Creates a new logger instance and configure it with the provided log level and file name.
     *
     * @param level Set the logger level to one of <code>
     *     [DEFAULT, ERROR, WARN, INFO, DEBUG, TRACE, OFF]
     *     </code>. If log level isn't provided, the logger will be configured with default
     *     configuration decided by Glide core.
     * @param fileName If provided, the target of the logs will be the file mentioned. Otherwise, logs
     *     will be printed to stdout.
     */
    public static void setLoggerConfig(@NonNull Level level, String fileName) {
        initLogger(level, fileName);
    }

    /**
     * Creates a new logger instance and configure it with the provided log level. The logs will be
     * written to stdout. To turn off the logger, use <code>setLoggerConfig(Level.OFF)</code>.
     *
     * @param level Set the logger level to one of <code>
     *     [DEFAULT, ERROR, WARN, INFO, DEBUG, TRACE, OFF]
     *     </code>. If log level isn't provided, the logger will be configured with default
     *     configuration decided by Glide core.
     */
    public static void setLoggerConfig(@NonNull Level level) {
        setLoggerConfig(level, null);
    }

    /**
     * Creates a new logger instance and configure it with the provided file name and default log
     * level. The logger will filter all logs with a level lower than the default level decided by the
     * Glide core.
     *
     * @param fileName If provided, the target of the logs will be the file mentioned. Otherwise, logs
     *     will be printed to stdout.
     */
    public static void setLoggerConfig(String fileName) {
        setLoggerConfig(Level.DEFAULT, fileName);
    }

    /**
     * Creates a new logger instance. The logger will filter all logs with a level lower than the
     * default level decided by Glide core. The logs will be written to stdout.
     */
    public static void setLoggerConfig() {
        setLoggerConfig(Level.DEFAULT, null);
    }
}
