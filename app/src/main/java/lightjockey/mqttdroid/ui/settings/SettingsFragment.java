package lightjockey.mqttdroid.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
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

        EditTextPreference mqttBrokerPortPref = findPreference(MqttDroidApp.appContext.getString(R.string.pref_key_mqtt_broker_port));
        if (mqttBrokerPortPref != null)
            mqttBrokerPortPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));

        updateDynamicPrefs();

        bindClickablePrefs();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if (key == null)
            return;

        if (key.contains("mqtt_"))
            MqttClient.disconnect();

        if (key.equals(MqttDroidApp.appContext.getString(R.string.pref_key_mqtt_broker_tls)) ||
            key.equals(MqttDroidApp.appContext.getString(R.string.pref_key_mqtt_client_use_auth)))
            updateDynamicPrefs();
    }

    private static void setPrefEnabled(Preference pref, boolean enabled) {
        if (pref == null) return;
        pref.setEnabled(enabled);
        pref.setShouldDisableView(!enabled);
    }

    private void updateDynamicPrefs() {
        Preference tlsValidatePref = findPreference(MqttDroidApp.appContext.getString(R.string.pref_key_mqtt_broker_tls_validate));
        setPrefEnabled(tlsValidatePref, MqttDroidApp.sharedPrefs.getBoolean(MqttDroidApp.appContext.getString(R.string.pref_key_mqtt_broker_tls), false));

        boolean useAuth = MqttDroidApp.sharedPrefs.getBoolean(MqttDroidApp.appContext.getString(R.string.pref_key_mqtt_client_use_auth), false);
        Preference usernamePref = findPreference(MqttDroidApp.appContext.getString(R.string.pref_key_mqtt_client_auth_username));
        setPrefEnabled(usernamePref, useAuth);
        Preference passwordPref = findPreference(MqttDroidApp.appContext.getString(R.string.pref_key_mqtt_client_auth_password));
        setPrefEnabled(passwordPref, useAuth);
    }

    private void bindClickablePrefs() {
        buildClickablePref(R.string.pref_key_backup_save, (pref) -> { Utils.backupData(getContext()); return true; });
        buildClickablePref(R.string.pref_key_backup_restore, (pref) -> { Utils.restoreData(getContext()); return true; });
        buildClickablePref(R.string.pref_key_mqtt_broker_test, (pref) -> { Utils.testBrokerConnection(getContext()); return true; });

        Preference githubPref = findPreference(MqttDroidApp.appContext.getString(R.string.pref_key_info_github));
        if (githubPref != null) {
            githubPref.setSummary(MqttDroidApp.appContext.getString(R.string.pref_sum_info_github, BuildConfig.COMMIT_HASH));
            buildClickablePref(githubPref, (pref) -> { Utils.startUrlIntent("https://github.com/LightJockey/MqttDroid"); return true; });
        }
    }

    private void buildClickablePref(int prefKey, Preference.OnPreferenceClickListener onClickHandler) {
        Preference pref = findPreference(MqttDroidApp.appContext.getString(prefKey));
        buildClickablePref(pref, onClickHandler);
    }
    private void buildClickablePref(Preference pref, Preference.OnPreferenceClickListener onClickHandler) {
        if (pref == null)
            return;
        pref.setOnPreferenceClickListener(onClickHandler);
    }
}