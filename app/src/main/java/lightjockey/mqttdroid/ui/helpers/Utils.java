package lightjockey.mqttdroid.ui.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.google.gson.Gson;
import com.hivemq.client.mqtt.datatypes.MqttQos;

import java.util.Arrays;
import java.util.List;

import lightjockey.mqttdroid.MqttDroidApp;
import lightjockey.mqttdroid.AppRepository;
import lightjockey.mqttdroid.MqttClient;
import lightjockey.mqttdroid.MqttControl;
import lightjockey.mqttdroid.R;

public class Utils {
    public static void showSimpleDialog(final Context ctx, String title, String message) {
        // Make it so that it runs on the UI thread if called from within an async function such as MqttClient.subscribe()'s callback
        new Handler(Looper.getMainLooper()).post(() -> new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(ctx.getText(android.R.string.ok), null)
                .show());
    }

    public static void showPositiveNegativeActionsDialog(final Context ctx,
                                                         String title,
                                                         String message,
                                                         String positiveLabel,
                                                         DialogInterface.OnClickListener positiveAction,
                                                         String negativeLabel,
                                                         DialogInterface.OnClickListener negativeAction) {
        new Handler(Looper.getMainLooper()).post(() -> new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveLabel, positiveAction)
                .setNegativeButton(negativeLabel, negativeAction)
                .show());
    }

    public static void deleteControlDialog(final Context ctx, MqttControl control, Runnable callback) {
        showPositiveNegativeActionsDialog(ctx,
                !control.hasEmptyName() ? ctx.getString(R.string.dialog_delete_control_title_hasname, control.getName()) : ctx.getString(R.string.dialog_delete_control_title_noname),
                ctx.getString(R.string.dialog_delete_control_body),
                "Delete",
                (dialog, which) -> {
                    MqttClient.unbindControl(control);
                    MqttDroidApp.appInstance.repository.delete(control);
                    if (callback != null)
                        callback.run();
                },
                ctx.getString(android.R.string.cancel),
                null);
    }

    public static void testBrokerConnection(final Context ctx) {
        MqttClient.disconnect();
        MqttClient.connect(true);
        String brokerHost = MqttClient.client.getConfig().getServerAddress().toString();
        if (MqttClient.isConnected())
            showSimpleDialog(ctx, "Test Passed", "Successfully connected to broker at " + brokerHost + "!");
        else
            showSimpleDialog(ctx, "Test Failed", "Couldn't connect to broker at " + brokerHost + ". Check logs for details");
        MqttClient.disconnect();
    }

    private static final String BACKUP_MQTT_TOPIC = "mqttdroid/backup";
    public static void backupData(final Context ctx) {
        showPositiveNegativeActionsDialog(ctx,
                "Backup Controls",
                "All controls will be serialized into the payload of a retained message and then sent to the broker you specified in the settings.\n\nAny previously made backup will be overwritten.\n\nProceed?",
                "Backup",
                (dialog, which) -> {
                    Gson gson = new Gson();
                    AppRepository repo = MqttDroidApp.appInstance.repository;

                    try {
                        List<MqttControl> controls = repo.getAllControls();
                        String serializedControls = gson.toJson(controls);
                        Log.d("JSON", serializedControls);
                        Log.d("JSON", "Serialized " + controls.size() + " controls, length: " + serializedControls.length());

                        MqttClient.publish(BACKUP_MQTT_TOPIC, serializedControls, MqttQos.EXACTLY_ONCE, true);
                        showSimpleDialog(ctx, "Backup Successful", "Successfully saved " + controls.size() + " controls on the broker");
                    } catch (Exception e) {
                        showSimpleDialog(ctx, "Backup Error", e.getMessage());
                    }
                },
                ctx.getString(android.R.string.cancel),
                null);
    }
    public static void restoreData(final Context ctx) {
        showPositiveNegativeActionsDialog(ctx,
                "Restore Controls",
                "If any previously made backup exists on the broker currently set in the settings, then the existing controls will be deleted and the backup restored.\n\nProceed?",
                "Yes, delete and restore",
                (dialog, which) -> {
                    Gson gson = new Gson();
                    AppRepository repo = MqttDroidApp.appInstance.repository;

                    try {
                        MqttClient.subscribe(BACKUP_MQTT_TOPIC, (msg) -> {
                            String payload = new String(msg.getPayloadAsBytes());
                            MqttControl[] deserializedControls = gson.fromJson(payload, MqttControl[].class);

                            if (deserializedControls != null && deserializedControls.length > 0) {
                                repo.deleteAll();
                                List<Long> recordIds = repo.insertAll(Arrays.asList(deserializedControls));

                                Log.d(MqttDroidApp.TAG, "Restored " + recordIds.size() + " controls");
                                showSimpleDialog(ctx, "Restore Successful", "Successfully restored " + recordIds.size() + " controls");
                            }
                            else {
                                showSimpleDialog(ctx, "Restore Aborted", "No controls found in the saved backup");
                            }
                        });
                    } catch (Exception e) {
                        showSimpleDialog(ctx, "Restore Error", "Couldn't restore the previous backup\n\n" + e.getMessage());
                    } finally {
                        MqttClient.unsubscribe(BACKUP_MQTT_TOPIC);
                    }
                },
                ctx.getString(android.R.string.cancel),
                null);
    }

    public static void startUrlIntent(String url) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        MqttDroidApp.appContext.startActivity(i);
    }

    public static int getThemePrimaryColorInverted(final Context ctx) {
        int nightModeFlags = ctx.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES ? Color.WHITE : Color.BLACK;
    }

    public static Bitmap getCustomIconBitmap(int iconId, boolean adaptToTheme) {
        Bitmap iconBitmap = null;
        Drawable customIconDrawable = MqttDroidApp.appInstance.iconPack.getIconDrawable(iconId, MqttDroidApp.appInstance.iconPackLoader.getDrawableLoader());
        if (customIconDrawable != null) {
            iconBitmap = Bitmap.createBitmap(customIconDrawable.getIntrinsicWidth(), customIconDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(iconBitmap);
            customIconDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            customIconDrawable.setColorFilter(new BlendModeColorFilter(adaptToTheme ? Utils.getThemePrimaryColorInverted(MqttDroidApp.appContext) : Color.WHITE, BlendMode.SRC_ATOP));
            customIconDrawable.draw(canvas);
        }
        return iconBitmap;
    }

    public static boolean isStringNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    @SuppressLint("DefaultLocale")
    public static String formatFloatReadable(float v) {
        if (v % 1 == 0)
            return String.valueOf((int)v);
        else
            return String.valueOf(v);
    }

    public static int boolToInt(boolean b) {
        return b ? 1 : 0;
    }
}
