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

package com.afwsamples.testdpc;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.os.Bundle;
import com.afwsamples.testdpc.common.Util;
import com.afwsamples.testdpc.policy.HiddenAppsPolicy;
import com.afwsamples.testdpc.profilepolicy.apprestrictions.ManagedConfigurationsFragment;

/**
 * Entry activity. Shows the managed configurations screen when this app manages the device or
 * profile, otherwise forwards to the setup flow.
 */
public class PolicyManagementActivity extends Activity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (!Util.isDeviceOwner(this)
        && !Util.isProfileOwner(this)
        && !Util.hasDelegation(this, DevicePolicyManager.DELEGATION_APP_RESTRICTIONS)) {
      startActivity(new Intent(this, SetupManagementActivity.class));
      finish();
      return;
    }
    HiddenAppsPolicy.apply(this);
    setContentView(R.layout.activity_main);
    if (savedInstanceState == null) {
      getFragmentManager()
          .beginTransaction()
          .add(R.id.container, new ManagedConfigurationsFragment())
          .commit();
    }
  }
}
