package lightjockey.mqttdroid;

import org.junit.Test;

public class MqttControlTest {
    @Test
    public void testEquals() {
        MqttControl controlA = MqttControl.Light_Toggle();
        MqttControl controlB = new MqttControl(controlA);
        assert controlA.equals(controlB);
    }
}