load("@rules_jvm_external//:defs.bzl", "artifact")

exports_files([
    "LICENSE",
    ".blazeproject",
    "WORKSPACE",
])

MANIFEST = "src/main/AndroidManifest.xml"

MANIFEST_DEBUG = "src/main/AndroidManifestDebug.xml"

PACKAGE = "com.afwsamples.testdpc"

android_library(
    name = "androidx_deps",
    exports = [
        "@maven//:androidx_annotation_annotation",
        "@maven//:androidx_appcompat_appcompat",
        "@maven//:androidx_collection_collection",
        "@maven//:androidx_core_core",
        "@maven//:androidx_enterprise_enterprise_feedback",
        "@maven//:androidx_legacy_legacy_support_v13",
        "@maven//:androidx_lifecycle_lifecycle_common",
        "@maven//:androidx_lifecycle_lifecycle_process",
        "@maven//:androidx_lifecycle_lifecycle_runtime",
        "@maven//:androidx_localbroadcastmanager_localbroadcastmanager",
        "@maven//:androidx_preference_preference",
        "@maven//:androidx_recyclerview_recyclerview",
    ],
)

android_library(
    name = "test_deps",
    exports = [
        artifact("com.google.testparameterinjector:test-parameter-injector"),
        artifact("com.google.truth:truth"),
        artifact("androidx.test:core"),
        artifact("androidx.test:monitor"),
        artifact("androidx.test:runner"),
        artifact("junit:junit"),
        artifact("org.hamcrest:java-hamcrest"),
        artifact("org.robolectric:robolectric"),
        artifact("org.robolectric:robolectric-annotations"),
        artifact("org.robolectric:shadows-core"),
        "@robolectric//bazel:android-all",
    ],
)

android_binary(
    name = "trianect_dpc",
    custom_package = PACKAGE,
    dexopts = [
        "--force-jumbo",
    ],
    manifest = MANIFEST,
    deps = [
        ":trianect_dpc_lib",
    ],
)

android_binary(
    name = "trianect_dpc_debug",
    custom_package = PACKAGE,
    dexopts = [
        "--force-jumbo",
    ],
    manifest = MANIFEST,
    deps = [
        ":trianect_dpc_lib",
        ":trianect_dpc_lib_debug",
    ],
)

android_library(
    name = "trianect_dpc_lib",
    srcs = glob(["src/main/java/**/*.java"]),
    custom_package = PACKAGE,
    manifest = MANIFEST,
    resource_files = glob(["src/main/res/**"]),
    deps = [
        ":androidx_deps",
        "@setupdesign//:setupdesign",
        "@setupcompat//:setupcompat",
    ],
)

android_library(
    name = "trianect_dpc_lib_debug",
    custom_package = PACKAGE,
    manifest = MANIFEST_DEBUG,
)

