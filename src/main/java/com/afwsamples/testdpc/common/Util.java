/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.afwsamples.testdpc.common;

import android.annotation.TargetApi;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.widget.ImageView;
import android.widget.Toast;
import com.afwsamples.testdpc.DeviceAdminReceiver;
import com.afwsamples.testdpc.R;
import java.io.FileNotFoundException;
import java.io.InputStream;

/** Common utility functions. */
public class Util {
  private static final boolean IS_RUNNING_V = VERSION.CODENAME.equals("VanillaIceCream");

  /**
   * A replacement for {@link VERSION.SDK_INT} that is compatible with pre-release SDKs
   *
   * <p>This will be set to the version SDK, or {@link VERSION_CODES.CUR_DEVELOPMENT} if the SDK int
   * is not yet assigned.
   */
  public static final int SDK_INT = IS_RUNNING_V ? VERSION_CODES.CUR_DEVELOPMENT : VERSION.SDK_INT;

  public static void updateImageView(Context context, ImageView imageView, Uri uri) {
    try {
      InputStream inputStream = context.getContentResolver().openInputStream(uri);
      // Avoid decoding the entire image if the imageView holding this image is smaller.
      BitmapFactory.Options bounds = new BitmapFactory.Options();
      bounds.inJustDecodeBounds = true;
      BitmapFactory.decodeStream(inputStream, null, bounds);
      int streamWidth = bounds.outWidth;
      int streamHeight = bounds.outHeight;
      int maxDesiredWidth = imageView.getMaxWidth();
      int maxDesiredHeight = imageView.getMaxHeight();
      int ratio = Math.max(streamWidth / maxDesiredWidth, streamHeight / maxDesiredHeight);
      if (ratio > 1) {
        bounds.inSampleSize = ratio;
      }
      bounds.inJustDecodeBounds = false;

      inputStream = context.getContentResolver().openInputStream(uri);
      imageView.setImageBitmap(BitmapFactory.decodeStream(inputStream, null, bounds));
    } catch (FileNotFoundException e) {
      Toast.makeText(context, R.string.error_opening_image_file, Toast.LENGTH_SHORT);
    }
  }

  /**
   * Return {@code true} iff we are the profile owner of a managed profile. Note that profile owner
   * can be in primary user and secondary user too.
   */
  @TargetApi(VERSION_CODES.N)
  public static boolean isManagedProfileOwner(Context context) {
    final DevicePolicyManager dpm = getDevicePolicyManager(context);

    if (Util.SDK_INT >= VERSION_CODES.N) {
      try {
        return dpm.isManagedProfile(DeviceAdminReceiver.getReceiverComponentName(context));
      } catch (SecurityException ex) {
        // This is thrown if we are neither profile owner nor device owner.
        return false;
      }
    }

    // Pre-N, TestDPC only supports being the profile owner for a managed profile. Other apps
    // may support being a profile owner in other contexts (e.g. a secondary user) which will
    // require further checks.
    return isProfileOwner(context);
  }

  @TargetApi(VERSION_CODES.LOLLIPOP)
  public static boolean isDeviceOwner(Context context) {
    final DevicePolicyManager dpm = getDevicePolicyManager(context);
    return dpm.isDeviceOwnerApp(context.getPackageName());
  }

  @TargetApi(VERSION_CODES.LOLLIPOP)
  public static boolean isProfileOwner(Context context) {
    final DevicePolicyManager dpm = getDevicePolicyManager(context);
    return dpm.isProfileOwnerApp(context.getPackageName());
  }

  private static DevicePolicyManager getDevicePolicyManager(Context context) {
    return (DevicePolicyManager) context.getSystemService(Service.DEVICE_POLICY_SERVICE);
  }

  public static boolean hasDelegation(Context context, String delegation) {
    if (Util.SDK_INT < VERSION_CODES.O) {
      return false;
    }
    DevicePolicyManager dpm = context.getSystemService(DevicePolicyManager.class);
    return dpm.getDelegatedScopes(null, context.getPackageName()).contains(delegation);
  }
}
