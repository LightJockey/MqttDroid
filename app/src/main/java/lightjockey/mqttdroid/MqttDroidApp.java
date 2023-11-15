package lightjockey.mqttdroid;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.color.DynamicColors;
import com.maltaisn.icondialog.pack.IconPack;
import com.maltaisn.icondialog.pack.IconPackLoader;
import com.maltaisn.iconpack.defaultpack.IconPackDefault;

public class MqttDroidApp extends Application {
    public static final String TAG = "MqttDroid";
    public static MqttDroidApp appInstance;
    public static Context appContext;
    public static SharedPreferences sharedPrefs;

    public AppRepository repository;
    public IconPack iconPack;
    public IconPackLoader iconPackLoader;

    private NotificationManagerCompat notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();

        appInstance = this;
        appContext = getApplicationContext();
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        repository = new AppRepository(this);
        notificationManager = NotificationManagerCompat.from(appContext);
        loadIconPack();

        DynamicColors.applyToActivitiesIfAvailable(this);
    }

    @SuppressLint("MissingPermission")
    public void sendNotification(CharSequence title, CharSequence text) {
        NotificationChannel channel = new NotificationChannel(
                "0",
                "General",
                NotificationManager.IMPORTANCE_DEFAULT);
        notificationManager.createNotificationChannel(channel);
        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(MqttDroidApp.appContext, "0")
                .setSmallIcon(R.drawable.baseline_hub_24)
                .setContentTitle(title)
                .setContentText(text);
        notificationManager.notify(0, notifBuilder.build());
    }

    @SuppressWarnings("unchecked")
    public static <T> T GetSharedPref(int key, T def) {
        try {
            if (def instanceof String)
                return (T)sharedPrefs.getString(appContext.getString(key), (String)def);
            else if (def instanceof Integer)
                return (T)(Integer)sharedPrefs.getInt(appContext.getString(key), (Integer)def);
            else if (def instanceof Boolean)
                return (T)(Boolean)sharedPrefs.getBoolean(appContext.getString(key), (Boolean)def);
        } catch (ClassCastException ignored) { }
        return def;
    }

    private void loadIconPack() {
        iconPackLoader = new IconPackLoader(getBaseContext());
        iconPack = IconPackDefault.createDefaultIconPack(iconPackLoader);
    }
}
