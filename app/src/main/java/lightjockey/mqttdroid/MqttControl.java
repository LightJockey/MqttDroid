package lightjockey.mqttdroid;

import android.annotation.SuppressLint;
import android.service.controls.DeviceTypes;

import androidx.annotation.NonNull;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;

import lightjockey.mqttdroid.ui.helpers.Utils;

@Entity(tableName = "controls")
public class MqttControl extends BaseObservable implements Serializable {
    public enum EControlType {
        TRIGGER(R.string.control_type_trigger),
        TOGGLE(R.string.control_type_toggle),
        RANGE(R.string.control_type_range),
        TOGGLERANGE(R.string.control_type_togglerange),
        GAUGE(R.string.control_type_gauge);

        private final int resString;
        EControlType(int resString) {
            this.resString = resString;
        }
        public int getIndex() {
            return ordinal();
        }
        public int getResString() {
            return resString;
        }

        public final static EControlType[] _VALUES = values();
    }
    public enum EControlFlavour {
        SWITCH(DeviceTypes.TYPE_SWITCH, R.string.control_flavour_switch),
        LIGHT(DeviceTypes.TYPE_LIGHT, R.string.control_flavour_light),
        OUTLET(DeviceTypes.TYPE_OUTLET, R.string.control_flavour_outlet),
        THERMOSTAT(DeviceTypes.TYPE_THERMOSTAT, R.string.control_flavour_thermostat),
        CUSTOM(DeviceTypes.TYPE_UNKNOWN, R.string.control_flavour_custom);

        private final int index;
        private final int resString;
        EControlFlavour(int index, int resString) {
            this.index = index;
            this.resString = resString;
        }
        public int getIndex() {
            return index;
        }
        public int getResString() {
            return resString;
        }

        public final static EControlFlavour[] _VALUES = values();
    }

    public static class ControlTopicPubSub extends ControlTopic {
        @ColumnInfo(name = "topic_sub")
        private String topicSub = "";
        @ColumnInfo(name = "is_pub_sub")
        private boolean isPubSub = false;

        public ControlTopicPubSub(ControlTopicPubSub otherTopicPubSub) {
            this.topic = otherTopicPubSub.topic;
            this.topicSub = otherTopicPubSub.topicSub;
            this.isPubSub = otherTopicPubSub.isPubSub;
            this.qos = otherTopicPubSub.qos;
            this.retained = otherTopicPubSub.retained;
        }
        public ControlTopicPubSub() { }

        @Bindable
        public String getTopicSub() {
            return this.isPubSub ? this.topicSub : this.topic;
        }
        public void setTopicSub(String topicSub) {
            if (topicSub != null && !this.topicSub.equals(topicSub)) {
                this.topicSub = topicSub;
                notifyPropertyChanged(BR.topicSub);
            }
        }
        @Bindable
        public boolean getIsPubSub() {
            return this.isPubSub;
        }
        public void setIsPubSub(boolean isPubSub) {
            if (this.isPubSub != isPubSub) {
                this.isPubSub = isPubSub;
                notifyPropertyChanged(BR.isPubSub);
            }
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof ControlTopicPubSub))
                return false;

            ControlTopicPubSub otherPubSubTopic = (ControlTopicPubSub)other;

            return  super.equals(otherPubSubTopic) &&
                    topicSub.equals(otherPubSubTopic.topicSub) &&
                    isPubSub == otherPubSubTopic.isPubSub;
        }
    }

    public static class ControlTopic extends BaseObservable {
        public enum EQos {
            QOS_0(R.string.control_topic_qos_0),
            QOS_1(R.string.control_topic_qos_1),
            QOS_2(R.string.control_topic_qos_2);

            private final int resString;
            EQos(int resString) {
                this.resString = resString;
            }
            public int getIndex() {
                return ordinal();
            }
            public int getResString() {
                return resString;
            }

            public final static EQos[] _VALUES = values();
        }

        @ColumnInfo(name = "topic")
        protected String topic = "";
        @ColumnInfo(name = "qos")
        protected int qos = 1;
        @ColumnInfo(name = "retained")
        protected boolean retained = false;

        public ControlTopic(ControlTopic otherTopic) {
            this.topic = otherTopic.topic;
            this.qos = otherTopic.qos;
            this.retained = otherTopic.retained;
        }
        // Needed for Room
        public ControlTopic() { }

        @Bindable
        public String getTopic() {
            return this.topic;
        }
        public void setTopic(String topic) {
            if (topic != null && !this.topic.equals(topic)) {
                this.topic = topic;
                notifyPropertyChanged(BR.topic);
            }
        }
        @Bindable
        public int getQos() {
            return this.qos;
        }
        public void setQos(int qos) {
            if (this.qos != qos) {
                this.qos = qos;
                notifyPropertyChanged(BR.qos);
            }
        }
        @Bindable
        public boolean getRetained() {
            return this.retained;
        }
        public void setRetained(boolean retained) {
            if (this.retained != retained) {
                this.retained = retained;
                notifyPropertyChanged(BR.retained);
            }
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof ControlTopic))
                return false;

            ControlTopic otherTopic = (ControlTopic)other;

            return  topic.equals(otherTopic.topic) &&
                    qos == otherTopic.qos &&
                    retained == otherTopic.retained;
        }
    }

    public class ControlStatus implements Serializable {
        public boolean state;
        private float value;
        public String payload = "";

        public float getValue() {
            return this.value;
        }
        public void setValue(float value) {
            if (value < getValueMin())
                this.value = getValueMin();
            else if (value > getValueMax())
                this.value = getValueMax();
            else
                this.value = value;
        }

        public float getValueReadable() {
            return getValueShowPercentage() ? getValuePercentage() : this.value;
        }
        public void setValueReadable(float valueReadable) {
            if (getValueShowPercentage())
                this.setValue((valueReadable * (getValueMax() - getValueMin()) / 100) + getValueMin());
            else
                this.setValue(valueReadable);
        }

        private int getValuePercentage() {
            return (int)(this.value / getValueMax() * 100);
        }
    }
    @Ignore
    public final transient ControlStatus controlStatus = new ControlStatus();

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private transient int id;
    @ColumnInfo(name = "name")
    private String name = "";
    @ColumnInfo(name = "subtitle")
    private String subtitle = "";
    @ColumnInfo(name = "group")
    private String group = "";
    @ColumnInfo(name = "type")
    private int type;
    @ColumnInfo(name = "flavour")
    private int flavour;
    @ColumnInfo(name = "custom_icon")
    private int customIcon;
    @ColumnInfo(name = "custom_color")
    private int customColor;
    @ColumnInfo(name = "needs_unlocking")
    private boolean needsUnlocking = true;
    @Embedded(prefix = "trigger_")
    public ControlTopic triggerTopic = new ControlTopic();
    @ColumnInfo(name = "trigger_payload")
    private String triggerPayload = "";
    @ColumnInfo(name = "gauge_topic")
    private String gaugeTopic = "";
    @Embedded(prefix = "state_")
    public ControlTopicPubSub stateTopic = new ControlTopicPubSub();
    @ColumnInfo(name = "state_on_payload")
    private String stateOnPayload = "On";
    @ColumnInfo(name = "state_off_payload")
    private String stateOffPayload = "Off";
    @ColumnInfo(name = "state_label_from_payload")
    private boolean stateLabelFromPayload = false;
    @Embedded(prefix = "value_")
    public ControlTopicPubSub valueTopic = new ControlTopicPubSub();
    @ColumnInfo(name = "value_min")
    private float valueMin = 0;
    @ColumnInfo(name = "value_max")
    private float valueMax = 100;
    @ColumnInfo(name = "value_step")
    private float valueStep = 1;
    @ColumnInfo(name = "value_show_percentage")
    private boolean valueShowPercentage;

    @Ignore
    public MqttControl(EControlType type, EControlFlavour flavour) {
        this.type = type.getIndex();
        this.flavour = flavour.getIndex();
    }
    @Ignore
    public MqttControl(EControlType type, EControlFlavour flavour, @NonNull String name) {
        this.type = type.getIndex();
        this.flavour = flavour.getIndex();
        this.name = name;
    }
    @Ignore
    public MqttControl(MqttControl otherControl) {
        this.id = otherControl.id;
        this.name = otherControl.name;
        this.subtitle = otherControl.subtitle;
        this.group = otherControl.group;
        this.type = otherControl.type;
        this.flavour = otherControl.flavour;
        this.customIcon = otherControl.customIcon;
        this.customColor = otherControl.customColor;
        this.needsUnlocking = otherControl.needsUnlocking;
        this.triggerTopic = new ControlTopic(otherControl.triggerTopic);
        this.triggerPayload = otherControl.triggerPayload;
        this.gaugeTopic = otherControl.gaugeTopic;
        this.stateTopic = new ControlTopicPubSub(otherControl.stateTopic);
        this.stateOnPayload = otherControl.stateOnPayload;
        this.stateOffPayload = otherControl.stateOffPayload;
        this.stateLabelFromPayload = otherControl.stateLabelFromPayload;
        this.valueTopic = new ControlTopicPubSub(otherControl.valueTopic);
        this.valueMin = otherControl.valueMin;
        this.valueMax = otherControl.valueMax;
        this.valueStep = otherControl.valueStep;
        this.valueShowPercentage = otherControl.valueShowPercentage;
    }
    // Needed for Room
    public MqttControl() { }

    @SuppressLint("DefaultLocale")
    @NonNull
    @Override
    @Ignore
    public String toString() {
        return String.format("id: %d, type: %d, name: %s [STATE on: %b, value: %f, last payload: %s]",
                getId(),
                getType(),
                getName(),
                controlStatus.state,
                controlStatus.value,
                controlStatus.payload);
    }

    @Ignore
    public boolean hasEmptyName() {
        return Utils.isStringNullOrEmpty(name);
    }
    @Ignore
    public boolean isTrigger() { return type == EControlType.TRIGGER.getIndex(); }
    @Ignore
    public boolean isGauge() { return type == EControlType.GAUGE.getIndex(); }
    @Ignore
    public boolean isToggle() { return type == EControlType.TOGGLE.getIndex(); }
    @Ignore
    public boolean isRange() { return type == EControlType.RANGE.getIndex(); }
    @Ignore
    public boolean isToggleRange() { return type == EControlType.TOGGLERANGE.getIndex(); }
    @Ignore
    public boolean isCustomFlavour() { return flavour == EControlFlavour.CUSTOM.getIndex(); }

    public int getId() {
        return this.id;
    }
    public void setId(int id) { this.id = id; }
    @Bindable
    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        if (name != null && !this.name.equals(name)) {
            this.name = name;
            notifyPropertyChanged(BR.name);
        }
    }
    @Bindable
    public String getSubtitle() {
        return this.subtitle;
    }
    public void setSubtitle(String subtitle) {
        if (subtitle != null && !this.subtitle.equals(subtitle)) {
            this.subtitle = subtitle;
            notifyPropertyChanged(BR.subtitle);
        }
    }
    @Bindable
    public String getGroup() { return this.group; }
    public void setGroup(String group) {
        if (group != null && !this.group.equals(group)) {
            this.group = group;
            notifyPropertyChanged(BR.group);
        }
    }
    @Bindable
    public int getType() {
        return this.type;
    }
    public void setType(int type) {
        if (this.type != type) {
            this.type = type;
            notifyPropertyChanged(BR._all);
        }
    }
    @Bindable
    public int getFlavour() {
        return this.flavour;
    }
    public void setFlavour(int flavour) {
        if (this.flavour != flavour) {
            this.flavour = flavour;
            notifyPropertyChanged(BR._all);
        }
    }
    public int getCustomIcon() { return this.customIcon; }
    public void setCustomIcon(int customIcon) { this.customIcon = customIcon; }
    public int getCustomColor() { return this.customColor; }
    public void setCustomColor(int customColor) { this.customColor = customColor; }
    @Bindable
    public boolean getNeedsUnlocking() {
        return this.needsUnlocking;
    }
    public void setNeedsUnlocking(boolean needsUnlocking) {
        if (this.needsUnlocking != needsUnlocking) {
            this.needsUnlocking = needsUnlocking;
            notifyPropertyChanged(BR.needsUnlocking);
        }
    }
    @Bindable
    public String getTriggerPayload() {
        return this.triggerPayload;
    }
    public void setTriggerPayload(String triggerPayload) {
        if (triggerPayload != null && !this.triggerPayload.equals(triggerPayload)) {
            this.triggerPayload = triggerPayload;
            notifyPropertyChanged(BR.triggerPayload);
        }
    }
    @Bindable
    public String getGaugeTopic() {
        return this.gaugeTopic;
    }
    public void setGaugeTopic(String gaugeTopic) {
        if (gaugeTopic != null && !this.gaugeTopic.equals(gaugeTopic)) {
            this.gaugeTopic = gaugeTopic;
            notifyPropertyChanged(BR.gaugeTopic);
        }
    }
    @Bindable
    public String getStateOnPayload() { return this.stateOnPayload; }
    public void setStateOnPayload(String stateOnPayload) {
        if (stateOnPayload != null && !this.stateOnPayload.equals(stateOnPayload)) {
            this.stateOnPayload = stateOnPayload;
            notifyPropertyChanged(BR.stateOnPayload);
        }
    }
    @Bindable
    public String getStateOffPayload() { return this.stateOffPayload; }
    public void setStateOffPayload(String stateOffPayload) {
        if (stateOffPayload != null && !this.stateOffPayload.equals(stateOffPayload)) {
            this.stateOffPayload = stateOffPayload;
            notifyPropertyChanged(BR.stateOffPayload);
        }
    }
    @Bindable
    public boolean getStateLabelFromPayload() { return this.stateLabelFromPayload; }
    public void setStateLabelFromPayload(boolean stateLabelFromPayload) {
        if (this.stateLabelFromPayload != stateLabelFromPayload) {
            this.stateLabelFromPayload = stateLabelFromPayload;
            notifyPropertyChanged(BR.stateLabelFromPayload);
        }
    }
    @Bindable
    public float getValueMin() { return this.valueMin; }
    public float getValueMinReadable() {
        return getValueShowPercentage() ? 0 : getValueMin();
    }
    public void setValueMin(float valueMin) {
        if (this.valueMin != valueMin) {
            this.valueMin = valueMin;
            notifyPropertyChanged(BR.valueMin);
        }
    }
    @Bindable
    public float getValueMax() { return this.valueMax; }
    public float getValueMaxReadable() {
        return getValueShowPercentage() ? 100 : getValueMax();
    }
    public void setValueMax(float valueMax) {
        if (this.valueMax != valueMax) {
            this.valueMax = valueMax;
            notifyPropertyChanged(BR.valueMax);
        }
    }
    @Bindable
    public float getValueStep() { return this.valueStep; }
    public void setValueStep(float valueStep) {
        if (this.valueStep != valueStep) {
            this.valueStep = valueStep;
            notifyPropertyChanged(BR.valueStep);
        }
    }
    @Bindable
    public boolean getValueShowPercentage() { return this.valueShowPercentage; }
    public void setValueShowPercentage(boolean valueShowPercentage) {
        if (this.valueShowPercentage != valueShowPercentage) {
            this.valueShowPercentage = valueShowPercentage;
            notifyPropertyChanged(BR.valueShowPercentage);
        }
    }

    public String getValueFormatString() {
        if (!valueShowPercentage)
            return controlStatus.value % 1 != 0 && valueStep % 1 != 0 ? "%.1f" : "%.0f";
        else
            return "%.0f%%";
    }

    public static MqttControl Light_Toggle() { return Light(EControlType.TOGGLE); }
    public static MqttControl Light_Range() {
        return Light(EControlType.RANGE);
    }
    public static MqttControl Light_ToggleRange() {
        return Light(EControlType.TOGGLERANGE);
    }
    public static MqttControl Light(EControlType controlType) {
        return new MqttControl(controlType, EControlFlavour.LIGHT);
    }
    public static MqttControl Light(String ControlName, String ControlTopic) {
        MqttControl light = Light_Toggle();
        light.name = ControlName;
        light.stateTopic.topic = ControlTopic;
        light.stateOnPayload = "On";
        light.stateOffPayload = "Off";
        return light;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MqttControl))
            return false;

        MqttControl otherControl = (MqttControl)other;

        if (this.id != otherControl.id)
            return false;

        return  this.name.equals(otherControl.name) &&
                this.subtitle.equals(otherControl.subtitle) &&
                this.group.equals(otherControl.group) &&
                this.type == otherControl.type &&
                this.flavour == otherControl.flavour &&
                this.customIcon == otherControl.customIcon &&
                this.customColor == otherControl.customColor &&
                this.needsUnlocking == otherControl.needsUnlocking &&
                this.triggerTopic.equals(otherControl.triggerTopic) &&
                this.triggerPayload.equals(otherControl.triggerPayload) &&
                this.gaugeTopic.equals(otherControl.gaugeTopic) &&
                this.stateTopic.equals(otherControl.stateTopic) &&
                this.stateOnPayload.equals(otherControl.stateOnPayload) &&
                this.stateOffPayload.equals(otherControl.stateOffPayload) &&
                this.stateLabelFromPayload == otherControl.stateLabelFromPayload &&
                this.valueTopic.equals(otherControl.valueTopic) &&
                this.valueMin == otherControl.valueMin &&
                this.valueMax == otherControl.valueMax &&
                this.valueStep == otherControl.valueStep &&
                this.valueShowPercentage == otherControl.valueShowPercentage;
    }
}
