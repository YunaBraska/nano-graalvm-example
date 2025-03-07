package berlin.yuna.nativeexample;

import berlin.yuna.typemap.model.LinkedTypeMap;
import org.nanonative.nano.core.Nano;
import org.nanonative.nano.services.http.HttpServer;
import org.nanonative.nano.services.http.model.HttpObject;
import org.nanonative.nano.services.metric.logic.MetricService;

import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.*;

import static org.nanonative.nano.core.model.NanoThread.activeCarrierThreads;
import static org.nanonative.nano.core.model.NanoThread.activeNanoThreads;
import static org.nanonative.nano.helper.NanoUtils.formatDuration;
import static org.nanonative.nano.services.http.HttpServer.CONFIG_SERVICE_HTTP_PORT;
import static org.nanonative.nano.services.http.HttpServer.EVENT_HTTP_REQUEST;
import static org.nanonative.nano.services.logging.LogService.CONFIG_LOG_FORMATTER;
import static org.nanonative.nano.services.logging.LogService.CONFIG_LOG_LEVEL;
import static org.nanonative.nano.services.logging.model.LogLevel.DEBUG;

public class Main {

    public static void main(String[] args) {
        final Nano nano = new Nano(Map.of(
            CONFIG_LOG_LEVEL, DEBUG, // or "OFF", "FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE", "ALL"
            CONFIG_LOG_FORMATTER, "console", // or "json"
            CONFIG_SERVICE_HTTP_PORT, "8080" // or any other port
        ), new MetricService(), new HttpServer());

        nano.context(Main.class)

            // HTTP Auth
            .subscribeEvent(EVENT_HTTP_REQUEST, event -> event.payloadOpt(HttpObject.class)
                .filter(request -> request.pathMatch("/api/**"))
                .ifPresent(request -> {
                    if (request.authToken() == null || !request.authToken().equals("dummy_token"))
                        request.response().statusCode(403).body(Map.of(
                            "id", request.bodyAsJson().asUUIDOpt("id").orElse(UUID.randomUUID()),
                            "message", "Unauthorized access",
                            "timestamp", Instant.now()
                        )).respond(event);
                    else
                        event.put("username", System.getProperty("user.name"));
                })
            )

            // HTTP /api/data - mirrors data
            .subscribeEvent(EVENT_HTTP_REQUEST, event -> event.payloadOpt(HttpObject.class)
                .filter(HttpObject::isMethodPost)
                .filter(request -> request.pathMatch("/api/data"))
                .ifPresent(request -> request.response()
                    .statusCode(200)
                    .body(request.bodyAsJson().asMap()
                        .putR("username", event.asString("username"))
                        .putR("memory", nano.heapMemoryUsage())
                    ).respond(event))
            )

            //HTTP /api/load - starts heavy load
            .subscribeEvent(EVENT_HTTP_REQUEST, event -> event.payloadOpt(HttpObject.class)
                .filter(HttpObject::isMethodGet)
                .filter(request -> request.pathMatch("/load1"))
                .ifPresent(request -> {
                    final int size = request.queryParams().asIntOpt("load")
                        .or(() -> request.bodyAsJson().asMap().asInt("load"))
                        .orElse(100_000_00);  // Oh, the sweet chaos
                    long start = System.currentTimeMillis();
                    Arrays.sort(new Random().ints(size, 0, 1_000_000).toArray()); // May the JVM have mercy
                    event.context().info(() -> "Testing load [{}]", size);
                    request.response()
                        .statusCode(200)
                        .body(Map.of(
                            "load", size,
                            "duration", formatDuration(System.currentTimeMillis() - start)
                        ))
                        .respond(event);
                })
            )

            // HTTP /hello
            .subscribeEvent(EVENT_HTTP_REQUEST, event -> event.payloadOpt(HttpObject.class)
                .filter(HttpObject::isMethodGet)
                .filter(request -> request.pathMatch("/hello"))
                .ifPresent(request -> request.response().statusCode(200).body(Map.of("name", event.context().asString("user_name"))).respond(event)))

            // HTTP /info
            .subscribeEvent(EVENT_HTTP_REQUEST, event -> event.payloadOpt(HttpObject.class)
                .filter(HttpObject::isMethodGet)
                .filter(request -> request.pathMatch("/info"))
                .ifPresent(request -> request.response().statusCode(200).body(new LinkedTypeMap()
                    .putR("status", nano.isReady() ? "UP" : "DOWN")
                    .putR("pid", nano.pid())
                    .putR("services", nano.services().size())
                    .putR("schedulers", nano.schedulers().size())
                    .putR("listeners", nano.listeners().values().stream().mapToLong(Collection::size).sum())
                    .putR("cores", Runtime.getRuntime().availableProcessors())
                    .putR("usedMemory", nano.usedMemoryMB() + "mb")
                    .putR("threadsNano", activeNanoThreads())
                    .putR("threadsActive", activeCarrierThreads())
                    .putR("threadsOther", ManagementFactory.getThreadMXBean().getThreadCount() - activeCarrierThreads())
                    .putR("encoding", Charset.defaultCharset().toString())
                    .putR("timezone", TimeZone.getDefault().getID())
                    .putR("locale", Locale.getDefault().toString())
                    .putR("user", event.context().asString("user_name"))
                    .putR("java", event.context().asString("java_version"))
                    .putR("arch", event.context().asString("os_arch"))
                    .putR("os", event.context().asString("os_name") + " - " + event.context().asString("os_version"))).respond(event)))
        ;
    }
}

