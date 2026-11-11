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

package com.trianect.mdm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Minimal home screen that only shows the StrokePointer apps and MDM Trianect itself.
 *
 * <p>Set as the default home app by {@link com.trianect.mdm.policy.HomeScreenPolicy}.
 */
public class HomeActivity extends Activity {

  /** Packages shown on the home screen, in this order. Packages that are not installed are skipped. */
  private static final List<String> HOME_PACKAGES =
      Arrays.asList("com.trianect.strokepointer", "com.trianect.strokepointerbeta");

  private ArrayAdapter<Intent> mAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.home_activity);
    mAdapter = new HomeAppAdapter(this);
    GridView grid = findViewById(R.id.home_apps);
    grid.setAdapter(mAdapter);
    grid.setOnItemClickListener(
        (parent, view, position, id) -> startActivity(mAdapter.getItem(position)));
  }

  @Override
  protected void onResume() {
    super.onResume();
    // Reload on every resume so apps installed or removed in the meantime are picked up.
    mAdapter.clear();
    mAdapter.addAll(getHomeIntents());
  }

  @Override
  public void onBackPressed() {
    // This is the home screen; there is nothing to go back to.
  }

  private List<Intent> getHomeIntents() {
    PackageManager pm = getPackageManager();
    List<Intent> intents = new ArrayList<>();
    for (String packageName : HOME_PACKAGES) {
      Intent launch = pm.getLaunchIntentForPackage(packageName);
      if (launch != null) {
        intents.add(launch);
      }
    }
    intents.add(new Intent(this, PolicyManagementActivity.class));
    return intents;
  }

  private static class HomeAppAdapter extends ArrayAdapter<Intent> {
    private final PackageManager mPackageManager;

    HomeAppAdapter(Context context) {
      super(context, R.layout.home_app_tile);
      mPackageManager = context.getPackageManager();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View view =
          convertView != null
              ? convertView
              : LayoutInflater.from(getContext()).inflate(R.layout.home_app_tile, parent, false);
      String packageName = getItem(position).getComponent().getPackageName();
      ImageView icon = view.findViewById(R.id.home_app_icon);
      TextView label = view.findViewById(R.id.home_app_label);
      try {
        ApplicationInfo info = mPackageManager.getApplicationInfo(packageName, 0);
        icon.setImageDrawable(mPackageManager.getApplicationIcon(info));
        label.setText(mPackageManager.getApplicationLabel(info));
      } catch (PackageManager.NameNotFoundException e) {
        icon.setImageDrawable(null);
        label.setText(packageName);
      }
      return view;
    }
  }
}
