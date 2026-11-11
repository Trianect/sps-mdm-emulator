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

package com.trianect.mdm.profilepolicy.apprestrictions;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.RestrictionEntry;
import android.content.RestrictionsManager;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import com.trianect.mdm.DeviceAdminReceiver;
import com.trianect.mdm.common.RestrictionManagerCompat;
import java.util.List;
import java.util.Map;

/**
 * Sets managed configuration values from adb, for screenshot automation and testing. Only callers
 * holding {@code android.permission.DUMP} (the adb shell) can send this broadcast.
 *
 * <pre>
 * adb shell am broadcast -a com.trianect.mdm.action.SET_MANAGED_CONFIGURATION \
 *     -n com.trianect.mdm/.profilepolicy.apprestrictions.SetManagedConfigurationReceiver \
 *     --es package com.trianect.strokepointer \
 *     --es MdmLanguage nl --ez MdmResearchMode true
 * </pre>
 *
 * <p>Every extra other than the ones below is a configuration key. Its value replaces the current
 * value; other keys are kept. String values are converted to boolean or integer when the app's
 * manifest declares the key with that type.
 *
 * <ul>
 *   <li>{@code package} (required): one of the managed StrokePointer packages.
 *   <li>{@code reset} (boolean, default false): start from the manifest defaults instead of the
 *       current configuration.
 *   <li>{@code restart} (boolean, default true): stop and relaunch the app so it reads the new
 *       configuration.
 * </ul>
 */
public class SetManagedConfigurationReceiver extends BroadcastReceiver {
  public static final String ACTION_SET_MANAGED_CONFIGURATION =
      "com.trianect.mdm.action.SET_MANAGED_CONFIGURATION";

  private static final String EXTRA_PACKAGE = "package";
  private static final String EXTRA_RESET = "reset";
  private static final String EXTRA_RESTART = "restart";

  private static final String TAG = "SetManagedConfig";

  @Override
  public void onReceive(Context context, Intent intent) {
    if (!ACTION_SET_MANAGED_CONFIGURATION.equals(intent.getAction())) {
      return;
    }
    Bundle extras = intent.getExtras();
    String pkgName = extras == null ? null : extras.getString(EXTRA_PACKAGE);
    if (pkgName == null || !ManagedConfigurationsFragment.MANAGED_PACKAGES.contains(pkgName)) {
      fail("Missing or unsupported package: " + pkgName);
      return;
    }
    try {
      context.getPackageManager().getApplicationInfo(pkgName, 0);
    } catch (PackageManager.NameNotFoundException e) {
      fail("Package not installed: " + pkgName);
      return;
    }
    ComponentName admin = DeviceAdminReceiver.getComponentName(context);
    if (admin == null) {
      fail("MDM Trianect is not device or profile owner");
      return;
    }
    DevicePolicyManager dpm = context.getSystemService(DevicePolicyManager.class);
    Bundle manifestDefaults = getManifestDefaults(context, pkgName);

    Bundle config = dpm.getApplicationRestrictions(admin, pkgName);
    if (extras.getBoolean(EXTRA_RESET, false) || config.isEmpty()) {
      config = new Bundle(manifestDefaults);
    }
    for (String key : extras.keySet()) {
      if (key.equals(EXTRA_PACKAGE) || key.equals(EXTRA_RESET) || key.equals(EXTRA_RESTART)) {
        continue;
      }
      if (!putValue(config, key, extras.get(key), manifestDefaults.get(key))) {
        fail("Invalid value for " + key + ": " + extras.get(key));
        return;
      }
    }
    for (Map.Entry<String, String> fixed : ManagedConfigurationsFragment.FIXED_VALUES.entrySet()) {
      config.putString(fixed.getKey(), fixed.getValue());
    }

    dpm.setApplicationRestrictions(admin, pkgName, config);
    Log.i(TAG, "Set managed configuration of " + pkgName + ": " + config);
    setResultCode(1);
    setResultData("OK");
    if (extras.getBoolean(EXTRA_RESTART, true)) {
      ManagedConfigurationsFragment.restartApp(context, dpm, admin, pkgName);
    }
  }

  /**
   * Puts the value in the config, converted to the type of the manifest default when the value is a
   * string. Returns false if the value cannot be converted.
   */
  private static boolean putValue(Bundle config, String key, Object value, Object manifestDefault) {
    if (value instanceof String) {
      String string = (String) value;
      if (manifestDefault instanceof Boolean) {
        if (!string.equalsIgnoreCase("true") && !string.equalsIgnoreCase("false")) {
          return false;
        }
        config.putBoolean(key, Boolean.parseBoolean(string));
      } else if (manifestDefault instanceof Integer) {
        try {
          config.putInt(key, Integer.parseInt(string));
        } catch (NumberFormatException e) {
          return false;
        }
      } else {
        config.putString(key, string);
      }
    } else if (value instanceof Boolean) {
      config.putBoolean(key, (Boolean) value);
    } else if (value instanceof Integer) {
      config.putInt(key, (Integer) value);
    } else if (value instanceof String[]) {
      config.putStringArray(key, (String[]) value);
    } else {
      return false;
    }
    return true;
  }

  /** Returns the defaults declared in the app's manifest, with choice settings as strings. */
  private static Bundle getManifestDefaults(Context context, String pkgName) {
    RestrictionsManager restrictionsManager = context.getSystemService(RestrictionsManager.class);
    List<RestrictionEntry> entries = restrictionsManager.getManifestRestrictions(pkgName);
    if (entries == null) {
      return new Bundle();
    }
    for (RestrictionEntry entry : entries) {
      if (entry.getType() == RestrictionEntry.TYPE_CHOICE
          || entry.getType() == RestrictionEntry.TYPE_NULL) {
        entry.setType(RestrictionEntry.TYPE_STRING);
      }
    }
    return RestrictionManagerCompat.convertRestrictionsToBundle(entries);
  }

  private void fail(String message) {
    Log.e(TAG, message);
    setResultCode(0);
    setResultData(message);
  }
}
