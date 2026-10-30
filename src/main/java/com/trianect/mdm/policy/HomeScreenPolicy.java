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

package com.trianect.mdm.policy;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.trianect.mdm.DeviceAdminReceiver;
import com.trianect.mdm.HomeActivity;
import com.trianect.mdm.common.Util;

/** Makes {@link HomeActivity} the default home screen. */
public final class HomeScreenPolicy {

  private HomeScreenPolicy() {}

  /** Sets {@link HomeActivity} as home app. Does nothing unless this app is device owner. */
  public static void apply(Context context) {
    if (!Util.isDeviceOwner(context)) {
      return;
    }
    DevicePolicyManager dpm = context.getSystemService(DevicePolicyManager.class);
    IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
    filter.addCategory(Intent.CATEGORY_HOME);
    filter.addCategory(Intent.CATEGORY_DEFAULT);
    dpm.addPersistentPreferredActivity(
        DeviceAdminReceiver.getComponentName(context),
        filter,
        new ComponentName(context, HomeActivity.class));
  }
}
