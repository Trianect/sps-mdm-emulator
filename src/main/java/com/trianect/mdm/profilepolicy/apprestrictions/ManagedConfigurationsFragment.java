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

package com.trianect.mdm.profilepolicy.apprestrictions;

import static com.trianect.mdm.common.keyvaluepair.KeyValuePairDialogFragment.RESULT_VALUE;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.RestrictionEntry;
import android.content.RestrictionsManager;
import android.content.pm.ApplicationInfo;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.trianect.mdm.DeviceAdminReceiver;
import com.trianect.mdm.R;
import com.trianect.mdm.common.ManageAppFragment;
import com.trianect.mdm.common.RestrictionManagerCompat;
import com.trianect.mdm.common.Util;
import com.trianect.mdm.common.keyvaluepair.KeyValuePairDialogFragment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This fragment shows the installed StrokePointer apps and allows viewing and editing application
 * restrictions for those apps. When an app has no restrictions set, its manifest defaults are
 * loaded instead.
 */
public class ManagedConfigurationsFragment extends ManageAppFragment {
  private List<RestrictionEntry> mRestrictionEntries = new ArrayList<>();
  private List<RestrictionEntry> mLastRestrictionEntries;
  /** Default values from the selected app's manifest, used to mark changed settings. */
  private Bundle mDefaultRestrictions = new Bundle();
  /** The selected app's manifest restrictions by key, used for the options of choice settings. */
  private final Map<String, RestrictionEntry> mManifestEntries = new HashMap<>();
  private DevicePolicyManager mDevicePolicyManager;
  private RestrictionsManager mRestrictionsManager;
  private RestrictionValueAdapter mAppRestrictionsArrayAdapter;
  private RestrictionEntry mEditingRestrictionEntry;
  private ComponentName mAdminComponent;
  private Button mSaveButton;

  private static final String TAG = "ManagedConfigurations";
  private static final int RESULT_CODE_EDIT_DIALOG = 1;

  /** Packages whose managed configurations can be edited. */
  private static final Set<String> MANAGED_PACKAGES =
      new HashSet<>(
          Arrays.asList("com.trianect.strokepointer", "com.trianect.strokepointerbeta"));

  /** Settings that are always set to a fixed value and cannot be edited. */
  private static final Map<String, String> FIXED_VALUES = new HashMap<>();

  static {
    FIXED_VALUES.put("MdmDeviceSerial", "999999999");
    FIXED_VALUES.put("MdmDeviceName", "DEMO");
  }

  private static final String[] BOOLEAN_VALUES = {"true", "false"};

  private static final int[] SUPPORTED_TYPES = {
    KeyValuePairDialogFragment.DialogType.BOOL_TYPE,
    KeyValuePairDialogFragment.DialogType.INT_TYPE,
    KeyValuePairDialogFragment.DialogType.STRING_TYPE,
    KeyValuePairDialogFragment.DialogType.STRING_ARRAY_TYPE,
    KeyValuePairDialogFragment.DialogType.BUNDLE_TYPE,
    KeyValuePairDialogFragment.DialogType.BUNDLE_ARRAY_TYPE
  };
  private static final int[] SUPPORTED_TYPES_PRE_M = {
    KeyValuePairDialogFragment.DialogType.BOOL_TYPE,
    KeyValuePairDialogFragment.DialogType.INT_TYPE,
    KeyValuePairDialogFragment.DialogType.STRING_TYPE,
    KeyValuePairDialogFragment.DialogType.STRING_ARRAY_TYPE,
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setRetainInstance(true);
    mDevicePolicyManager =
        (DevicePolicyManager) getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
    mRestrictionsManager =
        (RestrictionsManager) getActivity().getSystemService(Context.RESTRICTIONS_SERVICE);
    if (Util.hasDelegation(getActivity(), DevicePolicyManager.DELEGATION_APP_RESTRICTIONS)) {
      mAdminComponent = null;
    } else {
      mAdminComponent = DeviceAdminReceiver.getComponentName(getActivity());
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    getActivity().getActionBar().setTitle(R.string.managed_configurations);
  }

  protected void loadAppRestrictionsList(RestrictionEntry[] restrictionEntries) {
    if (restrictionEntries != null) {
      mAppRestrictionsArrayAdapter.clear();
      mAppRestrictionsArrayAdapter.addAll(Arrays.asList(restrictionEntries));
    }
  }

  protected RestrictionEntry[] convertBundleToRestrictions(Bundle restrictionBundle) {
    List<RestrictionEntry> restrictionEntries = new ArrayList<>();
    Set<String> keys = restrictionBundle.keySet();
    for (String key : keys) {
      Object value = restrictionBundle.get(key);
      if (value instanceof Boolean) {
        restrictionEntries.add(new RestrictionEntry(key, (boolean) value));
      } else if (value instanceof Integer) {
        restrictionEntries.add(new RestrictionEntry(key, (int) value));
      } else if (value instanceof String) {
        RestrictionEntry entry = new RestrictionEntry(RestrictionEntry.TYPE_STRING, key);
        entry.setSelectedString((String) value);
        restrictionEntries.add(entry);
      } else if (value instanceof String[]) {
        restrictionEntries.add(new RestrictionEntry(key, (String[]) value));
      } else if (value instanceof Bundle) {
        addBundleEntryToRestrictions(restrictionEntries, key, (Bundle) value);
      } else if (value instanceof Parcelable[]) {
        addBundleArrayToRestrictions(restrictionEntries, key, (Parcelable[]) value);
      }
    }
    return restrictionEntries.toArray(new RestrictionEntry[0]);
  }

  @TargetApi(VERSION_CODES.M)
  private void addBundleEntryToRestrictions(
      List<RestrictionEntry> restrictionEntries, String key, Bundle value) {
    restrictionEntries.add(
        RestrictionEntry.createBundleEntry(key, convertBundleToRestrictions(value)));
  }

  @TargetApi(VERSION_CODES.M)
  private void addBundleArrayToRestrictions(
      List<RestrictionEntry> restrictionEntries, String key, Parcelable[] value) {
    int length = value.length;
    RestrictionEntry[] entriesArray = new RestrictionEntry[length];
    for (int i = 0; i < entriesArray.length; ++i) {
      entriesArray[i] =
          RestrictionEntry.createBundleEntry(key, convertBundleToRestrictions((Bundle) value[i]));
    }
    restrictionEntries.add(RestrictionEntry.createBundleArrayEntry(key, entriesArray));
  }

  protected void showToast(String msg) {
    Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
  }

  private void showEditDialog(final RestrictionEntry restrictionEntry) {
    mEditingRestrictionEntry = restrictionEntry;
    int type = KeyValuePairDialogFragment.DialogType.BOOL_TYPE;
    Object value = null;
    String key = "";
    if (restrictionEntry != null) {
      key = restrictionEntry.getKey();
      type = getTypeIndexFromRestrictionType(restrictionEntry.getType());
      switch (restrictionEntry.getType()) {
        case RestrictionEntry.TYPE_BOOLEAN:
          value = restrictionEntry.getSelectedState();
          break;
        case RestrictionEntry.TYPE_INTEGER:
          value = restrictionEntry.getIntValue();
          break;
        case RestrictionEntry.TYPE_STRING:
          value = restrictionEntry.getSelectedString();
          break;
        case RestrictionEntry.TYPE_MULTI_SELECT:
          value = restrictionEntry.getAllSelectedStrings();
          break;
        case RestrictionEntry.TYPE_BUNDLE:
          value =
              RestrictionManagerCompat.convertRestrictionsToBundle(
                  Arrays.asList(getRestrictionEntries(restrictionEntry)));
          break;
        case RestrictionEntry.TYPE_BUNDLE_ARRAY:
          RestrictionEntry[] restrictionEntries = getRestrictionEntries(restrictionEntry);
          Bundle[] bundles = new Bundle[restrictionEntries.length];
          for (int i = 0; i < restrictionEntries.length; i++) {
            bundles[i] =
                RestrictionManagerCompat.convertRestrictionsToBundle(
                    Arrays.asList(getRestrictionEntries(restrictionEntries[i])));
          }
          value = bundles;
          break;
      }
    }
    int[] supportType = (Util.SDK_INT < VERSION_CODES.M) ? SUPPORTED_TYPES_PRE_M : SUPPORTED_TYPES;
    KeyValuePairDialogFragment dialogFragment =
        KeyValuePairDialogFragment.newInstance(
            type, true, key, value, supportType, getCurrentAppName(), getDefaultValue(key, value));
    dialogFragment.setTargetFragment(this, RESULT_CODE_EDIT_DIALOG);
    dialogFragment.show(getFragmentManager(), "dialog");
  }

  private int getTypeIndexFromRestrictionType(int restrictionType) {
    switch (restrictionType) {
      case RestrictionEntry.TYPE_BOOLEAN:
        return KeyValuePairDialogFragment.DialogType.BOOL_TYPE;
      case RestrictionEntry.TYPE_INTEGER:
        return KeyValuePairDialogFragment.DialogType.INT_TYPE;
      case RestrictionEntry.TYPE_STRING:
        return KeyValuePairDialogFragment.DialogType.STRING_TYPE;
      case RestrictionEntry.TYPE_MULTI_SELECT:
        return KeyValuePairDialogFragment.DialogType.STRING_ARRAY_TYPE;
      case RestrictionEntry.TYPE_BUNDLE:
        return KeyValuePairDialogFragment.DialogType.BUNDLE_TYPE;
      case RestrictionEntry.TYPE_BUNDLE_ARRAY:
        return KeyValuePairDialogFragment.DialogType.BUNDLE_ARRAY_TYPE;
      default:
        throw new AssertionError("Unknown restriction type");
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent result) {
    if (resultCode != Activity.RESULT_OK) {
      return;
    }
    RestrictionEntry newRestrictionEntry;
    switch (requestCode) {
      case RESULT_CODE_EDIT_DIALOG:
        int type = result.getIntExtra(KeyValuePairDialogFragment.RESULT_TYPE, 0);
        String key = result.getStringExtra(KeyValuePairDialogFragment.RESULT_KEY);
        newRestrictionEntry = new RestrictionEntry(getRestrictionTypeFromDialogType(type), key);
        updateRestrictionEntryFromResultIntent(newRestrictionEntry, result);
        mAppRestrictionsArrayAdapter.remove(mEditingRestrictionEntry);
        mEditingRestrictionEntry = null;
        mAppRestrictionsArrayAdapter.add(newRestrictionEntry);
        break;
    }
  }

  // TYPE_BUNDLE and TYPE_BUNDLE_ARRAY are only supported from M onward. It is blocked in the
  // UI side.
  @TargetApi(VERSION_CODES.M)
  private void updateRestrictionEntryFromResultIntent(
      RestrictionEntry restrictionEntry, Intent intent) {
    switch (restrictionEntry.getType()) {
      case RestrictionEntry.TYPE_BOOLEAN:
        restrictionEntry.setSelectedState(intent.getBooleanExtra(RESULT_VALUE, false));
        break;
      case RestrictionEntry.TYPE_INTEGER:
        restrictionEntry.setIntValue(intent.getIntExtra(RESULT_VALUE, 0));
        break;
      case RestrictionEntry.TYPE_STRING:
        restrictionEntry.setSelectedString(intent.getStringExtra(RESULT_VALUE));
        break;
      case RestrictionEntry.TYPE_MULTI_SELECT:
        restrictionEntry.setAllSelectedStrings(intent.getStringArrayExtra(RESULT_VALUE));
        break;
      case RestrictionEntry.TYPE_BUNDLE:
        {
          Bundle bundle = intent.getBundleExtra(RESULT_VALUE);
          restrictionEntry.setRestrictions(convertBundleToRestrictions(bundle));
          break;
        }
      case RestrictionEntry.TYPE_BUNDLE_ARRAY:
        {
          Parcelable[] bundleArray = intent.getParcelableArrayExtra(RESULT_VALUE);
          RestrictionEntry[] restrictionEntryArray = new RestrictionEntry[bundleArray.length];
          for (int i = 0; i < bundleArray.length; i++) {
            restrictionEntryArray[i] =
                RestrictionEntry.createBundleEntry(
                    String.valueOf(i), convertBundleToRestrictions((Bundle) bundleArray[i]));
          }
          restrictionEntry.setRestrictions(restrictionEntryArray);
          break;
        }
    }
  }

  private int getRestrictionTypeFromDialogType(int typeIndex) {
    switch (typeIndex) {
      case KeyValuePairDialogFragment.DialogType.BOOL_TYPE:
        return RestrictionEntry.TYPE_BOOLEAN;
      case KeyValuePairDialogFragment.DialogType.INT_TYPE:
        return RestrictionEntry.TYPE_INTEGER;
      case KeyValuePairDialogFragment.DialogType.STRING_TYPE:
        return RestrictionEntry.TYPE_STRING;
      case KeyValuePairDialogFragment.DialogType.STRING_ARRAY_TYPE:
        return RestrictionEntry.TYPE_MULTI_SELECT;
      case KeyValuePairDialogFragment.DialogType.BUNDLE_TYPE:
        return RestrictionEntry.TYPE_BUNDLE;
      case KeyValuePairDialogFragment.DialogType.BUNDLE_ARRAY_TYPE:
        return RestrictionEntry.TYPE_BUNDLE_ARRAY;
      default:
        throw new AssertionError("Unknown type index");
    }
  }

  @Override
  protected void addNewRow() {
    // Adding new keys is not supported; only keys from the app's manifest are edited.
  }

  @Override
  public View onCreateView(
      LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {
    View view = super.onCreateView(layoutInflater, container, savedInstanceState);
    view.findViewById(R.id.add_new_row).setVisibility(View.GONE);
    mSaveButton = view.findViewById(R.id.save_app);
    updateSaveButton();
    return view;
  }

  /** Shows on the save button whether saving applies changes, which restarts StrokePointer. */
  private void updateSaveButton() {
    if (mSaveButton != null) {
      mSaveButton.setText(hasUnsavedChanges() ? R.string.save_and_restart_sps : R.string.save);
    }
  }

  /** Whether the shown values differ from the last loaded or saved values. */
  private boolean hasUnsavedChanges() {
    if (mLastRestrictionEntries == null) {
      return false;
    }
    return !valuesEqual(
        RestrictionManagerCompat.convertRestrictionsToBundle(mRestrictionEntries),
        RestrictionManagerCompat.convertRestrictionsToBundle(mLastRestrictionEntries));
  }

  /** Places the app spinner on the right side of the action bar instead of in the layout. */
  @Override
  protected Spinner createSpinner(View view) {
    view.findViewById(R.id.managed_apps_list).setVisibility(View.GONE);
    ActionBar actionBar = getActivity().getActionBar();
    Spinner spinner = new Spinner(actionBar.getThemedContext());
    actionBar.setCustomView(
        spinner,
        new ActionBar.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.END | Gravity.CENTER_VERTICAL));
    actionBar.setDisplayShowCustomEnabled(true);
    return spinner;
  }

  /** Hides the action bar spinner while a nested bundle editor is shown on top. */
  @Override
  public void onHiddenChanged(boolean hidden) {
    super.onHiddenChanged(hidden);
    getActivity().getActionBar().setDisplayShowCustomEnabled(!hidden);
  }

  @Override
  public void onDestroyView() {
    ActionBar actionBar = getActivity().getActionBar();
    actionBar.setDisplayShowCustomEnabled(false);
    actionBar.setCustomView(null);
    super.onDestroyView();
  }

  @Override
  protected BaseAdapter createListAdapter() {
    mAppRestrictionsArrayAdapter = new RestrictionValueAdapter(getActivity(), mRestrictionEntries);
    return mAppRestrictionsArrayAdapter;
  }

  @Override
  protected boolean filterApp(ApplicationInfo info) {
    return MANAGED_PACKAGES.contains(info.packageName);
  }

  @Override
  protected void onSpinnerItemSelected(ApplicationInfo appInfo) {
    if (appInfo == null) {
      mHeaderView.setVisibility(View.VISIBLE);
      mHeaderView.setText(R.string.no_managed_apps_installed);
      return;
    }
    String pkgName = appInfo.packageName;
    if (!TextUtils.isEmpty(pkgName)) {
      List<RestrictionEntry> manifestRestrictions = getManifestRestrictions(pkgName);
      mDefaultRestrictions = RestrictionManagerCompat.convertRestrictionsToBundle(manifestRestrictions);
      mManifestEntries.clear();
      for (RestrictionEntry entry : manifestRestrictions) {
        mManifestEntries.put(entry.getKey(), entry);
      }
      Bundle bundle = mDevicePolicyManager.getApplicationRestrictions(mAdminComponent, pkgName);
      RestrictionEntry[] entries =
          bundle.isEmpty()
              ? getManifestRestrictions(pkgName).toArray(new RestrictionEntry[0])
              : convertBundleToRestrictions(bundle);
      loadAppRestrictionsList(applyFixedValues(entries));
      mLastRestrictionEntries = new ArrayList<>(mRestrictionEntries);
      updateSaveButton();
    }
  }

  /** Overrides the settings in {@link #FIXED_VALUES}, adding them if they are missing. */
  private RestrictionEntry[] applyFixedValues(RestrictionEntry[] entries) {
    List<RestrictionEntry> result = new ArrayList<>(Arrays.asList(entries));
    for (Map.Entry<String, String> fixed : FIXED_VALUES.entrySet()) {
      for (int i = result.size() - 1; i >= 0; i--) {
        if (result.get(i).getKey().equals(fixed.getKey())) {
          result.remove(i);
        }
      }
      RestrictionEntry entry = new RestrictionEntry(RestrictionEntry.TYPE_STRING, fixed.getKey());
      entry.setSelectedString(fixed.getValue());
      result.add(entry);
    }
    return result.toArray(new RestrictionEntry[0]);
  }

  /** Return the name of the application whose restrictions are currently displayed. */
  private String getCurrentAppName() {
    ApplicationInfo applicationInfo = (ApplicationInfo) mManagedAppsSpinner.getSelectedItem();
    return (String) getActivity().getPackageManager().getApplicationLabel(applicationInfo);
  }

  /** Returns the default restrictions declared in the app's manifest, or an empty list. */
  private List<RestrictionEntry> getManifestRestrictions(String pkgName) {
    try {
      List<RestrictionEntry> manifestRestrictions =
          mRestrictionsManager.getManifestRestrictions(pkgName);
      if (manifestRestrictions != null) {
        convertTypeChoiceAndNullToString(manifestRestrictions);
        return manifestRestrictions;
      }
    } catch (NullPointerException e) {
      // This means no default restrictions.
    }
    return new ArrayList<>();
  }

  /**
   * The manifest default for the key, or null if there is none or it is of a different type than
   * the current value. A missing string default is returned as an empty string.
   */
  private Object getDefaultValue(String key, Object currentValue) {
    if (!mDefaultRestrictions.containsKey(key)) {
      return null;
    }
    Object defaultValue = mDefaultRestrictions.get(key);
    if (defaultValue == null && (currentValue == null || currentValue instanceof String)) {
      return "";
    }
    if (defaultValue == null
        || (currentValue != null && !currentValue.getClass().equals(defaultValue.getClass()))) {
      return null;
    }
    return defaultValue;
  }

  /** Whether the entry's value differs from the default declared in the app's manifest. */
  private boolean isModifiedFromDefault(RestrictionEntry entry) {
    String key = entry.getKey();
    if (!mDefaultRestrictions.containsKey(key)) {
      return false;
    }
    Bundle current = RestrictionManagerCompat.convertRestrictionsToBundle(Arrays.asList(entry));
    return !valuesEqual(current.get(key), mDefaultRestrictions.get(key));
  }

  private static boolean valuesEqual(Object a, Object b) {
    if (a instanceof Bundle && b instanceof Bundle) {
      Bundle bundleA = (Bundle) a;
      Bundle bundleB = (Bundle) b;
      if (!bundleA.keySet().equals(bundleB.keySet())) {
        return false;
      }
      for (String key : bundleA.keySet()) {
        if (!valuesEqual(bundleA.get(key), bundleB.get(key))) {
          return false;
        }
      }
      return true;
    }
    if (a instanceof Object[] && b instanceof Object[]) {
      Object[] arrayA = (Object[]) a;
      Object[] arrayB = (Object[]) b;
      if (arrayA.length != arrayB.length) {
        return false;
      }
      for (int i = 0; i < arrayA.length; i++) {
        if (!valuesEqual(arrayA[i], arrayB[i])) {
          return false;
        }
      }
      return true;
    }
    // An empty string and a missing string default are treated as the same value.
    if ((a == null || a instanceof String) && (b == null || b instanceof String)) {
      return TextUtils.equals(a == null ? "" : (String) a, b == null ? "" : (String) b);
    }
    return a != null && a.equals(b);
  }

  /** TODO (b/23378519): Remove this method and add support for type choice and null. */
  private void convertTypeChoiceAndNullToString(List<RestrictionEntry> restrictionEntries) {
    for (RestrictionEntry entry : restrictionEntries) {
      if (entry.getType() == RestrictionEntry.TYPE_CHOICE
          || entry.getType() == RestrictionEntry.TYPE_NULL) {
        entry.setType(RestrictionEntry.TYPE_STRING);
      }
    }
  }

  @Override
  protected void saveConfig() {
    ApplicationInfo appInfo = (ApplicationInfo) mManagedAppsSpinner.getSelectedItem();
    if (appInfo == null) {
      return;
    }
    String pkgName = appInfo.packageName;
    boolean changed = hasUnsavedChanges();
    mDevicePolicyManager.setApplicationRestrictions(
        mAdminComponent,
        pkgName,
        RestrictionManagerCompat.convertRestrictionsToBundle(mRestrictionEntries));
    mLastRestrictionEntries = new ArrayList<>(mRestrictionEntries);
    updateSaveButton();
    showToast(getString(R.string.set_app_restrictions_success, pkgName));
    if (changed) {
      restartApp(pkgName);
    }
  }

  /**
   * Stops the app so it reads the new configuration on its next start, then launches it. Hiding an
   * app kills its processes; it is unhidden right away.
   */
  private void restartApp(String pkgName) {
    try {
      mDevicePolicyManager.setApplicationHidden(mAdminComponent, pkgName, true);
      mDevicePolicyManager.setApplicationHidden(mAdminComponent, pkgName, false);
    } catch (SecurityException e) {
      Log.w(TAG, "Unable to stop " + pkgName, e);
    }
    Intent launchIntent = getActivity().getPackageManager().getLaunchIntentForPackage(pkgName);
    if (launchIntent == null) {
      Log.w(TAG, "No launch intent for " + pkgName);
      return;
    }
    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(launchIntent);
  }

  /** Replaces all shown values by the app's manifest defaults. They are applied on save. */
  @Override
  protected void resetConfig() {
    ApplicationInfo appInfo = (ApplicationInfo) mManagedAppsSpinner.getSelectedItem();
    if (appInfo == null) {
      return;
    }
    loadAppRestrictionsList(
        applyFixedValues(
            getManifestRestrictions(appInfo.packageName).toArray(new RestrictionEntry[0])));
  }

  @TargetApi(VERSION_CODES.M)
  private RestrictionEntry[] getRestrictionEntries(RestrictionEntry restrictionEntry) {
    return restrictionEntry.getRestrictions();
  }

  /** The current value of a boolean or string entry, as shown in its dropdown. */
  private static String getOptionValue(RestrictionEntry entry) {
    return entry.getType() == RestrictionEntry.TYPE_BOOLEAN
        ? String.valueOf(entry.getSelectedState())
        : entry.getSelectedString();
  }

  /**
   * Fills the values and labels of the fixed options for the entry: booleans and choices from the
   * app's manifest. Returns false if the entry has no fixed options.
   */
  private boolean getOptions(RestrictionEntry entry, List<String> values, List<String> labels) {
    if (entry.getType() == RestrictionEntry.TYPE_BOOLEAN) {
      values.addAll(Arrays.asList(BOOLEAN_VALUES));
      labels.addAll(values);
      markDefaultOption(entry.getKey(), values, labels);
      return true;
    }
    RestrictionEntry manifestEntry = mManifestEntries.get(entry.getKey());
    if (entry.getType() != RestrictionEntry.TYPE_STRING
        || manifestEntry == null
        || manifestEntry.getChoiceValues() == null
        || manifestEntry.getChoiceValues().length == 0) {
      return false;
    }
    String[] choiceValues = manifestEntry.getChoiceValues();
    String[] choiceEntries = manifestEntry.getChoiceEntries();
    values.addAll(Arrays.asList(choiceValues));
    labels.addAll(
        choiceEntries != null && choiceEntries.length == choiceValues.length
            ? Arrays.asList(choiceEntries)
            : values);
    // Keep a current value that is not one of the options selectable.
    String current = entry.getSelectedString();
    if (current != null && !values.contains(current)) {
      values.add(current);
      labels.add(current);
    }
    markDefaultOption(entry.getKey(), values, labels);
    return true;
  }

  /** Appends " (default)" to the label of the option that is the app's default. */
  private void markDefaultOption(String key, List<String> values, List<String> labels) {
    Object defaultValue = mDefaultRestrictions.get(key);
    if (defaultValue == null) {
      return;
    }
    int index = values.indexOf(String.valueOf(defaultValue));
    if (index >= 0) {
      labels.set(index, getString(R.string.default_option_label, labels.get(index)));
    }
  }

  /** Text shown for entries without fixed options. */
  private static String getValueText(RestrictionEntry entry) {
    switch (entry.getType()) {
      case RestrictionEntry.TYPE_INTEGER:
        return String.valueOf(entry.getIntValue());
      case RestrictionEntry.TYPE_STRING:
        return entry.getSelectedString();
      case RestrictionEntry.TYPE_MULTI_SELECT:
        String[] strings = entry.getAllSelectedStrings();
        return strings == null ? "" : TextUtils.join(", ", strings);
      default:
        return "…";
    }
  }

  /** Replaces the entry by a copy with the new value, so that reset can restore the old one. */
  private void setOptionValue(RestrictionEntry entry, String value) {
    RestrictionEntry newEntry;
    if (entry.getType() == RestrictionEntry.TYPE_BOOLEAN) {
      newEntry = new RestrictionEntry(entry.getKey(), Boolean.parseBoolean(value));
    } else {
      newEntry = new RestrictionEntry(RestrictionEntry.TYPE_STRING, entry.getKey());
      newEntry.setSelectedString(value);
    }
    int index = mRestrictionEntries.indexOf(entry);
    if (index >= 0) {
      mRestrictionEntries.set(index, newEntry);
      mAppRestrictionsArrayAdapter.notifyDataSetChanged();
    }
  }

  /** Shows each restriction with its value: a dropdown for fixed options, text otherwise. */
  private class RestrictionValueAdapter extends ArrayAdapter<RestrictionEntry> {
    private final List<RestrictionEntry> mEntries;

    RestrictionValueAdapter(Context context, List<RestrictionEntry> entries) {
      super(context, 0, entries);
      mEntries = entries;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      if (convertView == null) {
        convertView =
            LayoutInflater.from(getContext())
                .inflate(R.layout.restriction_value_row, parent, false);
      }
      final RestrictionEntry entry = getItem(position);
      TextView keyText = convertView.findViewById(R.id.restriction_key);
      View modifiedIndicator = convertView.findViewById(R.id.modified_indicator);
      Spinner valueSpinner = convertView.findViewById(R.id.restriction_value_spinner);
      TextView valueText = convertView.findViewById(R.id.restriction_value_text);

      keyText.setText(entry.getKey());
      modifiedIndicator.setVisibility(isModifiedFromDefault(entry) ? View.VISIBLE : View.GONE);
      convertView.setOnClickListener(null);
      convertView.setClickable(false);

      final List<String> values = new ArrayList<>();
      List<String> labels = new ArrayList<>();
      if (getOptions(entry, values, labels)) {
        valueText.setVisibility(View.GONE);
        valueSpinner.setVisibility(View.VISIBLE);
        bindSpinner(valueSpinner, entry, values, labels);
      } else {
        valueSpinner.setVisibility(View.GONE);
        valueText.setVisibility(View.VISIBLE);
        valueText.setText(getValueText(entry));
        boolean fixed = FIXED_VALUES.containsKey(entry.getKey());
        valueText.setEnabled(!fixed);
        if (!fixed) {
          convertView.setOnClickListener(
              new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                  showEditDialog(entry);
                }
              });
        }
      }
      return convertView;
    }

    private void bindSpinner(
        Spinner spinner,
        final RestrictionEntry entry,
        final List<String> values,
        List<String> labels) {
      ArrayAdapter<String> optionsAdapter =
          new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, labels);
      optionsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      spinner.setOnItemSelectedListener(null);
      spinner.setAdapter(optionsAdapter);
      spinner.setSelection(Math.max(0, values.indexOf(getOptionValue(entry))), false);
      spinner.setOnItemSelectedListener(
          new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
              String value = values.get(position);
              if (!value.equals(getOptionValue(entry))) {
                setOptionValue(entry, value);
              }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
          });
    }

    @Override
    public void notifyDataSetChanged() {
      Collections.sort(
          mEntries,
          new Comparator<RestrictionEntry>() {
            @Override
            public int compare(RestrictionEntry entry1, RestrictionEntry entry2) {
              return entry1.getKey().compareTo(entry2.getKey());
            }
          });
      super.notifyDataSetChanged();
      updateSaveButton();
    }
  }
}
