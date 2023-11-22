package lightjockey.mqttdroid;

import android.annotation.SuppressLint;
import android.provider.Settings;
import android.util.Log;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientBuilder;
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;

import java.util.function.Consumer;

import lightjockey.mqttdroid.ui.helpers.Utils;

public class MqttClient {
    public static final String TAG = "MQTT";
    private static String MQTT_CLIENT_ID;
    private static final String MQTT_BROKER_DEFAULT_HOST = "192.168.1.1";
    private static final int MQTT_BROKER_DEFAULT_PORT = 1883;

    private static Mqtt3BlockingClient client;

    @SuppressLint("HardwareIds")
    private static void setClientId() {
        if (MQTT_CLIENT_ID == null)
            MQTT_CLIENT_ID = "mqttdroid_" + Settings.Secure.getString(MqttDroidApp.appInstance.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
    private static String getClientId() {
        return MqttDroidApp.GetSharedPref(R.string.pref_key_mqtt_client_id, MQTT_CLIENT_ID);
    }
    private static String getBrokerHost() {
        return MqttDroidApp.GetSharedPref(R.string.pref_key_mqtt_broker_host, MQTT_BROKER_DEFAULT_HOST);
    }
    private static int getBrokerPort() {
        return MqttDroidApp.GetSharedPref(R.string.pref_key_mqtt_broker_port, MQTT_BROKER_DEFAULT_PORT);
    }

    public static boolean isConnected() {
        if (client == null)
            return false;
        return client.getState().isConnected();
    }

    public static void connect() {
        connect(false);
    }
    public static void connect(boolean silent) {
        if (isConnected())
            return;

        setClientId();

        Mqtt3ClientBuilder builder = Mqtt3Client.builder()
                .identifier(getClientId())
                .serverHost(getBrokerHost())
                .serverPort(getBrokerPort())
                .addDisconnectedListener((ctx) -> Log.d(TAG, "Client disconnected"));
        try {
            boolean useAuth = MqttDroidApp.GetSharedPref(R.string.pref_key_mqtt_client_use_auth, false);
            if (useAuth) {
                builder = builder.simpleAuth()
                        .username(MqttDroidApp.GetSharedPref(R.string.pref_key_mqtt_client_auth_username, ""))
                        .password(MqttDroidApp.GetSharedPref(R.string.pref_key_mqtt_client_auth_password, "").getBytes())
                        .applySimpleAuth();
            }
            client = builder.buildBlocking();

            Log.d(TAG, "Connecting client (id: " + MQTT_CLIENT_ID + ", broker host: " + client.getConfig().getServerAddress() + ", use auth: " + useAuth + ") ...");
            Mqtt3ConnAck conn = client.connectWith()
                    .cleanSession(true)
                    .keepAlive(60)
                    .send();
            if (!conn.getReturnCode().isError())
                Log.d(TAG, "Client connected");
            else {
                String rejMsg = "Client connection rejected (code: " + conn.getReturnCode() + ")";
                Log.e(TAG, rejMsg);
                if (!silent)
                    MqttDroidApp.appInstance.sendNotification("MQTT Error", rejMsg);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to connect to broker (" + e.getMessage() + ")");
            if (!silent)
                MqttDroidApp.appInstance.sendNotification("MQTT Error","Unable to connect to broker at host " + client.getConfig().getServerAddress());
        }
    }

    public static void disconnect() {
        if (!isConnected())
            return;
        client.disconnect();
    }

    public static void bindControl(MqttControl control) {
        if (control.isGauge() && !Utils.isStringNullOrEmpty(control.getGaugeTopic())) {
            subscribe(control.getGaugeTopic(), (msg) -> {
                control.controlStatus.payload = new String(msg.getPayloadAsBytes());
                onControlMsgReceived(msg, control);
            });
        }
        if ((control.isToggle() || control.isToggleRange()) && !Utils.isStringNullOrEmpty(control.stateTopic.getTopicSub())) {
            subscribe(control.stateTopic.getTopicSub(), (msg) -> {
                String payload = new String(msg.getPayloadAsBytes());
                if (payload.equals(control.getStateOnPayload()))
                    control.controlStatus.state = true;
                else if (payload.equals(control.getStateOffPayload()))
                    control.controlStatus.state = false;
                onControlMsgReceived(msg, control);
            });
        }
        if ((control.isRange() || control.isToggleRange() && !Utils.isStringNullOrEmpty(control.valueTopic.getTopicSub()))) {
            subscribe(control.valueTopic.getTopicSub(), (msg) -> {
                String payload = new String(msg.getPayloadAsBytes());
                try {
                    control.controlStatus.setValue(Float.parseFloat(payload));
                } catch (Exception e) {
                    Log.e(TAG, "Could not parse payload \"" + payload + "\" as valid float");
                }
                onControlMsgReceived(msg, control);
            });
        }
    }
    public static void unbindControl(MqttControl control) {
        if (control.isGauge() && !Utils.isStringNullOrEmpty(control.getGaugeTopic()))
            unsubscribe(control.getGaugeTopic());
        if ((control.isToggle() || control.isToggleRange()) && !Utils.isStringNullOrEmpty(control.stateTopic.getTopicSub()))
            unsubscribe(control.stateTopic.getTopicSub());
        if ((control.isRange() || control.isToggleRange()) && !Utils.isStringNullOrEmpty(control.valueTopic.getTopicSub()))
            unsubscribe(control.valueTopic.getTopicSub());
    }

    public static void subscribe(String topic, Consumer<Mqtt3Publish> callback) {
        connect();
        if (!isConnected())
            return;

        Log.d(TAG, "Subscribing to topic \"" + topic + "\"");
        client.toAsync().subscribeWith()
                .topicFilter(topic)
                .qos(MqttQos.EXACTLY_ONCE)
                .callback(callback)
                .send();
    }
    public static void unsubscribe(String topic) {
        if (!isConnected())
            return;

        Log.d(TAG, "Unsubscribing from topic \"" + topic + "\"");
        client.toAsync().unsubscribeWith()
                .topicFilter(topic)
                .send();
    }

    public static void publishForControl(MqttControl control, MqttControl.EControlType publishType) {
        switch (publishType) {
            case TRIGGER:
                publish(control.triggerTopic.getTopic(),
                        control.getTriggerPayload(),
                        MqttQos.fromCode(control.triggerTopic.getQos()),
                        control.triggerTopic.getRetained());
                break;
            case TOGGLE:
                publish(control.stateTopic.getTopic(),
                        control.controlStatus.state ? control.getStateOnPayload() : control.getStateOffPayload(),
                        MqttQos.fromCode(control.stateTopic.getQos()),
                        control.stateTopic.getRetained());
                break;
            case RANGE:
                publish(control.valueTopic.getTopic(),
                        control.controlStatus.getValue() + "",
                        MqttQos.fromCode(control.valueTopic.getQos()),
                        control.valueTopic.getRetained());
                break;
        }
    }

    public static void publish(String topic, String payload, MqttQos qos, boolean retained) {
        connect();
        if (!isConnected())
            return;

        String f_payload = !Utils.isStringNullOrEmpty(payload) ? payload : "";

        Log.d(TAG, "Publishing msg (topic: \"" + topic + "\", payload: \"" + f_payload +"\")");
        client.publishWith()
                .topic(topic)
                .qos(qos)
                .retain(retained)
                .payload(f_payload.getBytes())
                .send();
    }

    private static void onControlMsgReceived(Mqtt3Publish msg, MqttControl control) {
        onMsgReceived(msg);
        if (MqttDroidControlsService.instance != null)
            MqttDroidControlsService.instance.updateControl(control, false);
    }

    private static void onMsgReceived(Mqtt3Publish msg) {
        Log.d("MQTT", "Received msg (topic: \"" + msg.getTopic() + "\", payload: \"" + new String(msg.getPayloadAsBytes()) + "\")");
    }
}
