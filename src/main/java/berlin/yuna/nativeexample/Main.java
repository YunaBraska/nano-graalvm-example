package berlin.yuna.nativeexample;

import berlin.yuna.typemap.model.LinkedTypeMap;
import org.nanonative.nano.core.Nano;
import org.nanonative.nano.services.http.HttpService;
import org.nanonative.nano.services.http.model.HttpObject;
import org.nanonative.nano.services.metric.logic.MetricService;

import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.*;

import static org.nanonative.nano.core.model.Context.CONFIG_LOG_FORMATTER;
import static org.nanonative.nano.core.model.Context.CONFIG_LOG_LEVEL;
import static org.nanonative.nano.core.model.NanoThread.activeCarrierThreads;
import static org.nanonative.nano.core.model.NanoThread.activeNanoThreads;
import static org.nanonative.nano.helper.NanoUtils.formatDuration;
import static org.nanonative.nano.helper.logger.model.LogLevel.DEBUG;
import static org.nanonative.nano.services.http.HttpService.CONFIG_SERVICE_HTTP_PORT;
import static org.nanonative.nano.services.http.HttpService.EVENT_HTTP_REQUEST;

public class Main {

    public static void main(String[] args) {
        final Nano nano = new Nano(Map.of(
            CONFIG_LOG_LEVEL, DEBUG, // or "OFF", "FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE", "ALL"
            CONFIG_LOG_FORMATTER, "console", // or "json"
            CONFIG_SERVICE_HTTP_PORT, "8080" // or any other port
        ), new MetricService(), new HttpService());

        nano.context(Main.class)

            // HTTP Auth
            .subscribeEvent(EVENT_HTTP_REQUEST, event -> event.payloadOpt(HttpObject.class)
                .filter(request -> request.pathMatch("/api/**"))
                .ifPresent(request -> {
                    if (request.authToken() == null || !request.authToken().equals("dummy_token"))
                        request.response().statusCode(403).body(Map.of(
                            "id", request.bodyAsJson().getOpt(UUID.class, "id").orElse(UUID.randomUUID()),
                            "message", "Unauthorized access",
                            "timestamp", Instant.now()
                        )).respond(event);
                    else
                        event.cache().put("username", System.getProperty("user.name"));
                })
            )

            // HTTP /api/data - mirrors data
            .subscribeEvent(EVENT_HTTP_REQUEST, event -> event.payloadOpt(HttpObject.class)
                .filter(HttpObject::isMethodPost)
                .filter(request -> request.pathMatch("/api/data"))
                .ifPresent(request -> {
                    final LinkedTypeMap data = request.bodyAsJson().asMap();
                    data.put("memory", nano.heapMemoryUsage());
                    event.context().logger().info(() -> "Received data: {}", data);
                    request.response()
                        .statusCode(200)
                        .body(data.putReturn("username", event.cache().get(String.class, "username")))
                        .respond(event);
                })
            )

            //HTTP /api/load - starts heavy load
            .subscribeEvent(EVENT_HTTP_REQUEST, event -> event.payloadOpt(HttpObject.class)
                .filter(HttpObject::isMethodGet)
                .filter(request -> request.pathMatch("/load1"))
                .ifPresent(request -> {
                    final int size = request.queryParams().getOpt(Integer.class, "load")
                        .or(() -> request.bodyAsJson().asMap().getOpt(Integer.class, "load"))
                        .orElse(100_000_00);  // Oh, the sweet chaos
                    long start = System.currentTimeMillis();
                    Arrays.sort(new Random().ints(size, 0, 1_000_000).toArray());  // May the JVM have mercy
                    event.context().logger().info(() -> "Testing load [{}]", size);
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
                .ifPresent(request -> request.response().statusCode(200).body(Map.of("name", event.context().get(String.class, "user_name"))).respond(event)))

            // HTTP /info
            .subscribeEvent(EVENT_HTTP_REQUEST, event -> event.payloadOpt(HttpObject.class)
                .filter(HttpObject::isMethodGet)
                .filter(request -> request.pathMatch("/info"))
                .ifPresent(request -> request.response().statusCode(200).body("{\n"
                    + "  \"status\": \"" + (nano.isReady() ? "UP" : "DOWN") + "\",\n"
                    + "  \"pid\": \"" + nano.pid() + "\",\n"
                    + "  \"services\": \"" + nano.services().size() + "\",\n"
                    + "  \"schedulers\": \"" + nano.schedulers().size() + "\",\n"
                    + "  \"listeners\": \"" + nano.listeners().values().stream().mapToLong(Collection::size).sum() + "\",\n"
                    + "  \"cores\": \"" + Runtime.getRuntime().availableProcessors() + "\",\n"
                    + "  \"usedMemory\": \"" + nano.usedMemoryMB() + "mb\",\n"
                    + "  \"threadsNano\": \"" + activeNanoThreads() + "\",\n"
                    + "  \"threadsActive\": \"" + activeCarrierThreads() + "\",\n"
                    + "  \"threadsOther\": \"" + (ManagementFactory.getThreadMXBean().getThreadCount() - activeCarrierThreads()) + "\",\n"
                    + "  \"encoding\": \"" + Charset.defaultCharset() + "\",\n"
                    + "  \"timezone\": \"" + TimeZone.getDefault().getID() + "\",\n"
                    + "  \"locale\": \"" + Locale.getDefault() + "\",\n"
                    + "  \"user\": \"" + event.context().get(String.class, "user_name") + "\",\n"
                    + "  \"java\": \"" + event.context().get(String.class, "java_version") + "\",\n"
                    + "  \"arch\": \"" + event.context().get(String.class, "os_arch") + "\",\n"
                    + "  \"os\": \"" + event.context().get(String.class, "os_name") + " - " + event.context().get(String.class, "os_version") + "\"\n"
                    + "}").respond(event)))
        ;
    }
}

