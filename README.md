# trianect-dpc

trianect-dpc is a minimal Android Device Policy Controller (DPC) used by Trianect to set the
managed configurations (app restrictions) of the StrokePointer apps:

- `com.trianect.strokepointer`
- `com.trianect.strokepointerbeta`

The app opens directly on the **Managed configurations** screen. Select a StrokePointer app, press
*Load default* to load the settings the app declares in its manifest (`app_restrictions.xml`), edit
them and press *Save*.

## Based on TestDPC

This project is a fork of Google's [Test DPC](https://github.com/googlesamples/android-testdpc),
based on upstream release **v9.0.16** (commit `e027da9`). TestDPC is a sample DPC that exposes
nearly every Android enterprise API. We stripped it down to what we need:

- Only the *Managed configurations* screen is kept; all other policy screens, search, kiosk mode,
  COSU, work-profile cross-app features, ownership transfer and shell commands were removed.
- The app list only shows the StrokePointer apps (see `MANAGED_PACKAGES` in
  [ManagedConfigurationsFragment.java](src/main/java/com/afwsamples/testdpc/profilepolicy/apprestrictions/ManagedConfigurationsFragment.java)).
- The provisioning flow (device owner / work profile setup) is kept, because only the owner app can
  set managed configurations.
- If the app is not yet device or profile owner, launching it opens the setup screen.
- The app is renamed to *trianect-dpc*. The Java package and application ID are still
  `com.afwsamples.testdpc`, so it cannot be installed next to the original TestDPC.

## Hidden apps

When trianect-dpc is device or profile owner it hides preinstalled apps that are not needed on
StrokePointer devices: communication (Phone, Messages, Contacts), media & files (Camera, Photos,
Gallery, Files, Clock, Calendar, Calculator) and Google apps (Chrome, YouTube, Gmail, Maps, Google
app, Assistant, Drive, Meet). Hidden apps stay installed but are removed from the launcher and
cannot be started. The Play Store and Settings stay available.

The list is applied after provisioning and every time trianect-dpc is opened. It is defined in
[HiddenAppsPolicy.java](src/main/java/com/afwsamples/testdpc/policy/HiddenAppsPolicy.java);
changing it requires a new APK. Removing a package from the list does not unhide it on devices
where it is already hidden.

## Version

| | |
|---|---|
| Version name | `9.0.16` (inherited from TestDPC) |
| Version code | `9016` |
| Application ID | `com.afwsamples.testdpc` |
| Min / target SDK | 21 / 35 |
| Signing | Bazel debug key (SHA-256 `6379ddb41110a3f38dc9cd0855ffdb099184c749a6ddf64b7f67e4a3da5674d7`) |

The version is defined in [AndroidManifest.xml](src/main/AndroidManifest.xml) and the app name in
[strings.xml](src/main/res/values/strings.xml). CI overrides the version on release tags (see below).

## Building

The project uses Bazel (version pinned in `.bazelversion`; use [bazelisk](https://github.com/bazelbuild/bazelisk)).

```console
export ANDROID_HOME=<Path to the Android SDK>   # needs platforms;android-35
./build.sh                                      # or: bazelisk build //:trianect_dpc
```

The APK is written to `bazel-bin/trianect_dpc.apk`.

The required [setupdesign library](https://android.googlesource.com/platform/external/setupdesign/+/refs/heads/main)
is imported and patched dynamically using the command line utility `ed`, which needs to be on the path.

## CI/CD

[.github/workflows/build.yml](.github/workflows/build.yml) runs on GitHub Actions:

- Every push and pull request to `master` or `trianect-dpc` builds the APK and attaches it to the
  workflow run.
- Pushing a tag `vX.Y.Z` sets `versionName` to `X.Y.Z` and `versionCode` to `10000 + run number`,
  and publishes the APK as a GitHub Release:

  ```console
  git tag v1.0.0 && git push origin v1.0.0
  ```

APKs are signed with the Bazel debug key, which is the same on every machine, so CI and local
builds can update each other. Note that this key is public: it is fine for internal and test
devices, but anyone can sign an APK with it.

## Provisioning

trianect-dpc must be device owner or profile owner before it can set managed configurations.

### Device Owner (ADB)

On a freshly reset device (no accounts added):

```console
adb install bazel-bin/trianect_dpc.apk
adb shell dpm set-device-owner com.afwsamples.testdpc/.DeviceAdminReceiver
```

## License

Licensed under the Apache 2.0 license, like the original TestDPC. See the LICENSE file for details.
