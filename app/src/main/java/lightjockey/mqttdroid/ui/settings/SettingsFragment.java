package lightjockey.mqttdroid.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import lightjockey.mqttdroid.BuildConfig;
import lightjockey.mqttdroid.MqttDroidApp;
import lightjockey.mqttdroid.MqttClient;
import lightjockey.mqttdroid.R;
import lightjockey.mqttdroid.ui.helpers.Utils;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        MqttDroidApp.sharedPrefs.registerOnSharedPreferenceChangeListener(this);

        updateMqttAuthPrefs();

        Preference clickablePref = findPreference(MqttDroidApp.appContext.getString(R.string.pref_key_backup_save));
        if (clickablePref != null) {
            clickablePref.setOnPreferenceClickListener(pref -> {
                Utils.backupData(getContext());
                return true;
            });
        }
        clickablePref = findPreference(MqttDroidApp.appContext.getString(R.string.pref_key_backup_restore));
        if (clickablePref != null) {
            clickablePref.setOnPreferenceClickListener(pref -> {
                Utils.restoreData(getContext());
                return true;
            });
        }
        clickablePref = findPreference(MqttDroidApp.appContext.getString(R.string.pref_key_mqtt_broker_test));
        if (clickablePref != null) {
            clickablePref.setOnPreferenceClickListener(pref -> {
                Utils.testBrokerConnection(getContext());
                return true;
            });
        }
        clickablePref = findPreference(MqttDroidApp.appContext.getString(R.string.pref_key_info_github));
        if (clickablePref != null) {
            clickablePref.setSummary(MqttDroidApp.appContext.getString(R.string.pref_sum_info_github, BuildConfig.COMMIT_HASH));
            clickablePref.setOnPreferenceClickListener(pref -> {
                Utils.startUrlIntent("https://github.com/LightJockey/MqttDroid");
                return true;
            });
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if (key == null)
            return;

        if (key.contains("mqtt_"))
            MqttClient.disconnect();

        if (key.equals(MqttDroidApp.appContext.getString(R.string.pref_key_mqtt_client_use_auth)))
            updateMqttAuthPrefs();
    }

    private void updateMqttAuthPrefs() {
        boolean useAuth = MqttDroidApp.sharedPrefs.getBoolean(MqttDroidApp.appContext.getString(R.string.pref_key_mqtt_client_use_auth), false);
        Preference usernamePref = findPreference(MqttDroidApp.appContext.getString(R.string.pref_key_mqtt_client_auth_username));
        setPrefEnabled(usernamePref, useAuth);
        Preference passwordPref = findPreference(MqttDroidApp.appContext.getString(R.string.pref_key_mqtt_client_auth_password));
        setPrefEnabled(passwordPref, useAuth);
    }

    private static void setPrefEnabled(Preference pref, boolean enabled) {
        if (pref == null) return;
        pref.setEnabled(enabled);
        pref.setShouldDisableView(!enabled);
    }
}