/*
 * Copyright (C) 2026 Trianect
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.afwsamples.testdpc.policy;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;
import com.afwsamples.testdpc.DeviceAdminReceiver;
import com.afwsamples.testdpc.common.Util;
import java.util.Arrays;
import java.util.List;

/**
 * Hides preinstalled apps that should not be available on StrokePointer devices.
 *
 * <p>Hidden apps stay installed but disappear from the launcher and cannot be started. Both the
 * AOSP and the Google variant of each app are listed; packages that are not installed are skipped.
 */
public final class HiddenAppsPolicy {
  private static final String TAG = "HiddenAppsPolicy";

  private static final List<String> HIDDEN_PACKAGES =
      Arrays.asList(
          // Communication
          "com.google.android.dialer",
          "com.android.dialer",
          "com.google.android.apps.messaging",
          "com.android.messaging",
          "com.android.mms",
          "com.google.android.contacts",
          "com.android.contacts",
          // Media & files
          "com.google.android.GoogleCamera",
          "com.android.camera2",
          "com.android.camera",
          "com.google.android.apps.photos",
          "com.android.gallery3d",
          "com.google.android.apps.nbu.files",
          "com.google.android.deskclock",
          "com.android.deskclock",
          "com.google.android.calendar",
          "com.android.calendar",
          "com.google.android.calculator",
          "com.android.calculator2",
          // Google apps
          "com.android.chrome",
          "com.google.android.youtube",
          "com.google.android.apps.youtube.music",
          "com.google.android.videos",
          "com.google.android.gm",
          "com.google.android.apps.maps",
          "com.google.android.googlequicksearchbox",
          "com.google.android.apps.googleassistant",
          "com.google.android.apps.docs",
          "com.google.android.apps.tachyon");

  private HiddenAppsPolicy() {}

  /** Hides all {@link #HIDDEN_PACKAGES}. Does nothing unless this app is device or profile owner. */
  public static void apply(Context context) {
    if (!Util.isDeviceOwner(context) && !Util.isProfileOwner(context)) {
      return;
    }
    DevicePolicyManager dpm = context.getSystemService(DevicePolicyManager.class);
    ComponentName admin = DeviceAdminReceiver.getComponentName(context);
    for (String packageName : HIDDEN_PACKAGES) {
      try {
        if (!dpm.isApplicationHidden(admin, packageName)
            && dpm.setApplicationHidden(admin, packageName, true)) {
          Log.i(TAG, "Hidden " + packageName);
        }
      } catch (SecurityException e) {
        Log.w(TAG, "Not allowed to hide " + packageName, e);
      }
    }
  }
}
