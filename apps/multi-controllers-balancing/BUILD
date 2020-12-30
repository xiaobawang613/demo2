COMPILE_DEPS = CORE_DEPS + JACKSON + METRICS + KRYO + CLI + REST + [
    "@rrd4j//jar",
    "//core/store/serializers:onos-core-serializers",
    "//apps/cpman/api:onos-apps-cpman-api",
    "//apps/cpman/app:onos-apps-cpman-app",
    "//apps/nodemetrics/api:onos-apps-nodemetrics-api",
    "//core/api:onos-api-native",
    "//utils/misc:onlab-misc-native",
]

osgi_jar(
    suppress_checkstyle = True,
    deps= COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.mcb",
    category = "LoadBalance",
    description = "test",
    required_apps = ["org.onosproject.cpman","org.onosproject.nodemetrics"],
    title = "load balancing",
    url = "http://onosproject.org",
)

#java_binary(
#    name = "loadbalance",
#    srcs = ["src/main/java/org/onosproject/mcb/MultiControllersBalancing.java"],
#    main_class = "org.onosproject.mcb.MultiControllersBalancing",
#    deps = ["//apps/cpman/api:onos-apps-cpman-api","//core/api:onos-api-native","//utils/misc:onlab-misc-native",],
#)

