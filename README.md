# MDM Trianect

MDM Trianect is a minimal Android Device Policy Controller (DPC) used by Trianect to set the
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
  [ManagedConfigurationsFragment.java](src/main/java/com/trianect/mdm/profilepolicy/apprestrictions/ManagedConfigurationsFragment.java)).
- The provisioning flow (device owner / work profile setup) is kept, because only the owner app can
  set managed configurations.
- If the app is not yet device or profile owner, launching it opens the setup screen.
- The app is renamed to *MDM Trianect*, with Java package and application ID `com.trianect.mdm`,
  so it can be installed next to the original TestDPC.

## Hidden apps

When MDM Trianect is device or profile owner it hides preinstalled apps that are not needed on
StrokePointer devices: communication (Phone, Messages, Contacts), media & files (Camera, Photos,
Gallery, Files, Clock, Calendar, Calculator), SIM toolkit, Google apps (Chrome, YouTube, Gmail,
Maps, Google app, Assistant, Drive, Meet) and the Play Store. Hidden apps stay installed but are
removed from the launcher and cannot be started. Settings stays available.

Hiding *Files* (`documentsui`) also disables the system file picker, so other apps (including
StrokePointer) cannot open or save files through it.

The list is applied after provisioning and every time MDM Trianect is opened. It is defined in
[HiddenAppsPolicy.java](src/main/java/com/trianect/mdm/policy/HiddenAppsPolicy.java);
changing it requires a new APK. Removing a package from the list does not unhide it on devices
where it is already hidden.

## Home screen

As device owner, MDM Trianect replaces the launcher with its own home screen
([HomeActivity.java](src/main/java/com/trianect/mdm/HomeActivity.java)) that only shows
StrokePointer, StrokePointer beta (when installed) and MDM Trianect itself. It is set as the default
home app after provisioning and every time MDM Trianect is opened
([HomeScreenPolicy.java](src/main/java/com/trianect/mdm/policy/HomeScreenPolicy.java)).

## Version

| | |
|---|---|
| Version name | `9.0.16` (inherited from TestDPC) |
| Version code | `9016` |
| Application ID | `com.trianect.mdm` |
| Min / target SDK | 21 / 35 |
| Signing | Bazel debug key (SHA-256 `6379ddb41110a3f38dc9cd0855ffdb099184c749a6ddf64b7f67e4a3da5674d7`) |

The version is defined in [AndroidManifest.xml](src/main/AndroidManifest.xml) and the app name in
[strings.xml](src/main/res/values/strings.xml). CI overrides the version on release tags (see below).

## Building

The project uses Bazel (version pinned in `.bazelversion`; use [bazelisk](https://github.com/bazelbuild/bazelisk)).

```console
export ANDROID_HOME=<Path to the Android SDK>   # needs platforms;android-35
./build.sh                                      # or: bazelisk build //:mdm_trianect
```

The APK is written to `bazel-bin/mdm_trianect.apk`.

The required [setupdesign library](https://android.googlesource.com/platform/external/setupdesign/+/refs/heads/main)
is imported and patched dynamically using the command line utility `ed`, which needs to be on the path.

## CI/CD

[.github/workflows/build.yml](.github/workflows/build.yml) runs on GitHub Actions:

- Every push and pull request to `master` or `mdm-trianect` builds the APK and attaches it to the
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

MDM Trianect must be device owner or profile owner before it can set managed configurations.

### Device Owner (ADB)

On a freshly reset device (no accounts added):

```console
adb install bazel-bin/mdm_trianect.apk
adb shell dpm set-device-owner com.trianect.mdm/.DeviceAdminReceiver
```

## License

Licensed under the Apache 2.0 license, like the original TestDPC. See the LICENSE file for details.
