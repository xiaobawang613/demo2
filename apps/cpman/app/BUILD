COMPILE_DEPS = CORE_DEPS + JACKSON + METRICS + KRYO + CLI + REST + [
    "@rrd4j//jar",
    "//core/store/serializers:onos-core-serializers",
    "//apps/cpman/api:onos-apps-cpman-api",
    "//apps/nodemetrics/api:onos-apps-nodemetrics-api",
]


BUNDLES = [
    "//apps/cpman/api:onos-apps-cpman-api",
    ":onos-apps-cpman-app",
    "@rrd4j//jar",
]

EXCLUDED_BUNDLES = [
    "@rrd4j//jar",
]

osgi_jar_with_tests(
    suppress_checkstyle = True,
    api_description = "REST API for Control Plane Manager",
    api_package = "org.onosproject.cpman.rest",
    api_title = "Control Plane Manager API",
    api_version = "1.0",
    karaf_command_packages = ["org.onosproject.cpman.cli"],
    web_context = "/onos/cpman",
    deps = COMPILE_DEPS,

)

onos_app(
    app_name = "org.onosproject.cpman",
    category = "Monitoring",
    description = "Control Plane Management application for monitoring the health of the ONOS cluster",
    excluded_bundles = EXCLUDED_BUNDLES,
    included_bundles = BUNDLES,
    required_apps = ["org.onosproject.openflow-message","org.onosproject.nodemetrics"],
    title = "Control Plane Manager",
    url = "http://onosproject.org",
)
