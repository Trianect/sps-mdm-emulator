/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.trianect.mdm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION_CODES;
import android.util.Log;
import android.widget.Toast;
import com.trianect.mdm.common.Util;
import com.trianect.mdm.provision.PostProvisioningTask;

/** Handles events related to the managed profile or managed device. */
public class DeviceAdminReceiver extends android.app.admin.DeviceAdminReceiver {
  private static final String TAG = "DeviceAdminReceiver";

  @Override
  public void onProfileProvisioningComplete(Context context, Intent intent) {
    if (Util.SDK_INT >= VERSION_CODES.O) {
      // See http://b/177617306.
      return;
    }
    PostProvisioningTask task = new PostProvisioningTask(context);
    if (!task.performPostProvisioningOperations(intent)) {
      return;
    }
    final Intent launchIntent = task.getPostProvisioningLaunchIntent(intent);
    if (launchIntent != null) {
      context.startActivity(launchIntent);
    } else {
      Log.e(
          TAG,
          "DeviceAdminReceiver.onProvisioningComplete() invoked, but ownership " + "not assigned");
      Toast.makeText(context, R.string.device_admin_receiver_failure, Toast.LENGTH_LONG).show();
    }
  }

  /**
   * Get the ComponentName that DevicePolicyManager expects when calling its APIs.
   *
   * <p>This is the actual ComponentName of the DeviceAdminReceiver if we are calling as the DPC,
   * and null for other cases (delegates, Role Holder etc)
   */
  public static ComponentName getComponentName(Context context) {
    if (Util.isDeviceOwner(context) || Util.isProfileOwner(context)) {
      return getReceiverComponentName(context);
    } else {
      return null;
    }
  }

  /**
   * @param context The context of the application.
   * @return The component name of this component in the given context.
   */
  public static ComponentName getReceiverComponentName(Context context) {
    return new ComponentName(context.getApplicationContext(), DeviceAdminReceiver.class);
  }
}
