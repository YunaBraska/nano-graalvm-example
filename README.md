# Nano Graalvm Example

Demonstrating the integration of [Nano](https://github.com/YunaBraska/nano) with GraalVM for creating efficient native executables, showcasing optimal
configurations and techniques for leveraging nano in high-performance, low-footprint applications.
This example starts a nano service with following endpoints [`GET /info`, `GET /hello`, `GET /prometheus`].

### Requirements

* `Graalvm` >= 21.0.0 in `$JAVA_HOME` environment variable

### Native Builds

* Maven build native executable _(see [pom file](pom.xml))_: `./mvnw clean package -Pnative`
* CLI minimal _(
  replace `your-application-classpath`)_:

```shell
    native-image \
    -H:Name=native-executable \
    -cp your-application-classpath berlin.yuna.nativeexample.Main
```

* CLI strict _(
  replace `your-application-classpath`)_:

```shell
    native-image \
    --no-fallback \
    --no-server \
    --initialize-at-build-time \
    -H:Name=native-executable \
    -cp your-application-classpath berlin.yuna.nativeexample.Main
```

### GraalVM parameter explanation

| Parameter                               | Description                                                                                                                                                                                                                                  |
|-----------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `--no-fallback`                         | Disables the fallback feature, ensuring that the native image doesn't include the JVM. This results in a smaller, more efficient executable but requires all code to be fully native-image compatible.                                       |
| `--initialize-at-build-time`            | Instructs GraalVM to initialize specified classes at build time rather than at runtime. This can improve startup time but requires careful management of class initialization to avoid errors.                                               |
| `--initialize-at-run-time=<class-name>` | Specifies classes to be initialized at runtime instead of at build time. This is useful for classes that perform operations during initialization that must occur at runtime, such as loading native libraries or reading system properties. |
| `-H:Name=native-executable`             | Sets the name of the generated executable file.                                                                                                                                                                                              |
| `-cp "<class-path>"`                    | Specifies the classpath, which includes paths to all classes and libraries that your application depends on. Separate multiple paths with `:` on Unix-like systems or `;` on Windows.                                                        |
| `<main-class>`                          | The fully qualified name of your main class. This is the entry point of your application.                                                                                                                                                    |

### Endpoints Examples

#### GET /info
Response:
```text
"yuna"
```

#### GET /info
Response:
```json
{
    "status": "UP",
    "pid": "84925",
    "services": "3",
    "schedulers": "2",
    "listeners": "12",
    "cores": "10",
    "usedMemory": "6.5mb",
    "threadsNano": "0",
    "threadsActive": "0",
    "threadsOther": "10",
    "encoding": "UTF-8",
    "timezone": "UTC",
    "locale": "en_US",
    "user": "yuna",
    "java": "22",
    "arch": "aarch64",
    "os": "Mac OS X"
}
```

#### GET /prometheus
Response:
```text
application_listeners 10.0
application_pid 85058.0
application_ready_time 1.0
application_schedulers 2.0
application_services_ready_time{class="HttpService"} 1.0
application_services_ready_time{class="LogQueue"} 0.0
application_services_ready_time{class="MetricService"} 0.0
application_services{class="HttpService"} 1.0
application_services{class="LogQueue"} 1.0
application_services{class="MetricService"} 1.0
application_started_time 6.0
disk_free_bytes 1.57301051392E11
disk_total_bytes 4.94384795648E11
java_version 22.0
jvm_classes_loaded 0.0
jvm_classes_unloaded 0.0
jvm_memory_committed_bytes{area="heap",id="eden space"} 7340032.0
jvm_memory_committed_bytes{area="heap",id="old generation space"} 0.0
jvm_memory_committed_bytes{area="heap",id="survivor space"} 0.0
jvm_memory_committed_bytes{area="nonheap",id="runtime code cache (code and data)"} 0.0
jvm_memory_committed_bytes{area="nonheap",id="runtime code cache (native metadata)"} 0.0
jvm_memory_max_bytes 5.4975266816E10
jvm_memory_used_bytes 7340032.0
jvm_memory_used_bytes{area="heap",id="eden space"} 7340032.0
jvm_memory_used_bytes{area="heap",id="old generation space"} 0.0
jvm_memory_used_bytes{area="heap",id="survivor space"} 0.0
jvm_memory_used_bytes{area="nonheap",id="runtime code cache (code and data)"} 0.0
jvm_memory_used_bytes{area="nonheap",id="runtime code cache (native metadata)"} 0.0
jvm_threads_carrier 0.0
jvm_threads_daemon 8.0
jvm_threads_live 11.0
jvm_threads_nano 1.0
jvm_threads_peak 11.0
process_cpu_usage 1.5066418452024105E-4
service_metrics_bytes 13708.0
service_metrics_counters 0.0
service_metrics_gauges 45.0
service_metrics_timers 0.0
system_arch_bit 64.0
system_cpu_cores 10.0
system_cpu_usage 0.22554890219560877
system_load_average_1m 3.58837890625
system_type 2.0
system_unix 1.0
system_version 1441.0
```
