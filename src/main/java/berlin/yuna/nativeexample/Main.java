package berlin.yuna.nativeexample;

import com.sun.net.httpserver.HttpExchange;
import berlin.yuna.nano.core.Nano;
import berlin.yuna.nano.helper.logger.logic.LogQueue;
import berlin.yuna.nano.services.http.HttpService;
import berlin.yuna.nano.services.metric.logic.MetricService;

import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import static berlin.yuna.nano.core.model.Config.*;
import static berlin.yuna.nano.core.model.NanoThread.activeCarrierThreads;
import static berlin.yuna.nano.core.model.NanoThread.activeNanoThreads;
import static berlin.yuna.nano.helper.event.model.EventType.EVENT_APP_UNHANDLED;
import static berlin.yuna.nano.helper.event.model.EventType.EVENT_HTTP_REQUEST;
import static berlin.yuna.nano.helper.logger.model.LogLevel.DEBUG;

public class Main {

    public static void main(String[] args) {
        final Nano nano = new Nano(Map.of(
            CONFIG_LOG_LEVEL, DEBUG, // or "OFF", "FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE", "ALL"
            CONFIG_LOG_FORMATTER, "console", // or "json"
            CONFIG_SERVICE_HTTP_PORT, "8080" // or any other port
        ), new MetricService(), new LogQueue(), new HttpService());

        nano.newContext(Main.class)
            // print unexpected errors
            .subscribeEvent(EVENT_APP_UNHANDLED, event -> event.context().logger().fatal(event.payload(Throwable.class), () -> "Unexpected error at [{}]", event.payload()))
            // HTTP Endpoints
            .subscribeEvent(EVENT_HTTP_REQUEST, event -> event.payloadOpt(HttpExchange.class)
                .filter(request -> "GET".equals(request.getRequestMethod()))
                .filter(request -> "/hello".equals(request.getRequestURI().getPath()))
                .ifPresent(request -> event.response(new HttpService.HttpResponse(200, event.context().get(String.class, "user_name").getBytes(), null)))
            )
            .subscribeEvent(EVENT_HTTP_REQUEST, event -> event.payloadOpt(HttpExchange.class)
                .filter(request -> "GET".equals(request.getRequestMethod()))
                .filter(request -> "/prometheus".equals(request.getRequestURI().getPath()))
                .ifPresent(request -> event.response(new HttpService.HttpResponse(200, event.context().service(MetricService.class).metrics().prometheus().getBytes(), null)))
            )
            .subscribeEvent(EVENT_HTTP_REQUEST, event -> event.payloadOpt(HttpExchange.class)
                .filter(request -> "GET".equals(request.getRequestMethod()))
                .filter(request -> "/info".equals(request.getRequestURI().getPath()))
                .ifPresent(request -> event.response(new HttpService.HttpResponse(200, ("{\n"
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
                    + "}").getBytes(), null)))
            )
        ;

    }
}

