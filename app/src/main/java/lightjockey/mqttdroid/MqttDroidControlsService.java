package lightjockey.mqttdroid;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.controls.Control;
import android.service.controls.ControlsProviderService;
import android.service.controls.actions.BooleanAction;
import android.service.controls.actions.CommandAction;
import android.service.controls.actions.ControlAction;
import android.service.controls.actions.FloatAction;
import android.service.controls.templates.ControlButton;
import android.service.controls.templates.ControlTemplate;
import android.service.controls.templates.RangeTemplate;
import android.service.controls.templates.StatelessTemplate;
import android.service.controls.templates.ToggleRangeTemplate;
import android.service.controls.templates.ToggleTemplate;
import android.util.Log;

import androidx.annotation.NonNull;

import org.reactivestreams.FlowAdapters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.processors.ReplayProcessor;
import lightjockey.mqttdroid.ui.controls.ControlActivity;
import lightjockey.mqttdroid.ui.helpers.Utils;

public class MqttDroidControlsService extends ControlsProviderService {
    public static MqttDroidControlsService instance;

    private static final String TAG = "MqttDroidControlsService";

    private AppRepository repository;

    private final Map<String, ReplayProcessor<Control>> controlsProcessors = new HashMap<>();
    public ReplayProcessor<Control> GetControlProcessor(String controlId) {
        return controlsProcessors.get(controlId);
    }

    private final Map<String, MqttControl> controlsMap = new HashMap<>();
    public MqttControl getControlForId(String controlId) {
        MqttControl control = controlsMap.get(controlId);
        if (control == null)
            Log.e(TAG, "Found no control for id " + controlId);
        return control;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
        repository = MqttDroidApp.appInstance.repository;
    }
    @Override
    public void onDestroy() {
        MqttClient.disconnect();

        super.onDestroy();
    }

    @NonNull
    @Override
    public Flow.Publisher<Control> createPublisherForAllAvailable() {
        List<Control> controls = new ArrayList<>();

        repository.getAllControls().forEach(control -> controls.add(createStatelessControl(control, false)));

        return FlowAdapters.toFlowPublisher(Flowable.fromIterable(controls));
    }

    @NonNull
    @Override
    public Flow.Publisher<Control> createPublisherFor(@NonNull List<String> controlIds) {
        ReplayProcessor<Control> updatePublisher = ReplayProcessor.create(8);

        repository.getAllControls().forEach(control -> controlsMap.put(control.getId() + "", control));

        MqttClient.disconnect();
        controlIds.forEach(controlId -> {
            controlsProcessors.put(controlId, updatePublisher);

            MqttControl control = getControlForId(controlId);
            if (control != null) {
                MqttClient.bindControl(control);
                UpdateControl(control, false);
            }
        });

        return FlowAdapters.toFlowPublisher(updatePublisher);
    }

    @Override
    public void performControlAction(@NonNull String controlId, @NonNull ControlAction action, @NonNull Consumer<Integer> consumer) {
        MqttControl control = getControlForId(controlId);
        if (control == null)
            return;

        if (!control.isGauge())
            consumer.accept(ControlAction.RESPONSE_OK);

        if (action instanceof CommandAction) {
            if (control.isTrigger())
                MqttClient.publishForControl(control, MqttControl.EControlType.TRIGGER);
        }
        else if (action instanceof BooleanAction) {
            control.controlStatus.state = ((BooleanAction)action).getNewState();
            MqttClient.publishForControl(control, MqttControl.EControlType.TOGGLE);
        }
        else if (action instanceof FloatAction) {
            control.controlStatus.setValueReadable( ((FloatAction)action).getNewValue() );
            MqttClient.publishForControl(control, MqttControl.EControlType.RANGE);
        }
    }

    public void UpdateControl(MqttControl control, boolean updateMap) {
        Log.d(TAG, "Updating control: " + control.toString());

        if (updateMap)
            controlsMap.put(control.getId() + "", control);

        ReplayProcessor<Control> controlProcessor = GetControlProcessor(control.getId() + "");
        if (controlProcessor != null)
            controlProcessor.onNext(MakeControl(control));
    }

    private Control MakeControl(MqttControl control) {
        ControlTemplate controlTemplate;
        String controlId = control.getId() + "";

        switch (MqttControl.EControlType.values()[control.getType()]) {
            case TOGGLE:
                controlTemplate = new ToggleTemplate(controlId,
                    new ControlButton(
                        control.controlStatus.state,
                        String.valueOf(control.controlStatus.state).toUpperCase(Locale.getDefault())
                    )
                );
                break;
            case RANGE:
                controlTemplate = new RangeTemplate(controlId,
                        control.getValueMinReadable(),
                        control.getValueMaxReadable(),
                        control.controlStatus.getValueReadable(),
                        control.getValueStep(),
                        control.getValueFormatString());
                break;
            case TOGGLERANGE:
                controlTemplate = new ToggleRangeTemplate(controlId,
                    new ControlButton(
                        control.controlStatus.state,
                        String.valueOf(control.controlStatus.state).toUpperCase(Locale.getDefault())
                    ),
                    new RangeTemplate(controlId,
                        control.getValueMinReadable(),
                        control.getValueMaxReadable(),
                        control.controlStatus.getValueReadable(),
                        control.getValueStep(),
                        control.getValueFormatString()));
                break;

            default: // This also serves TRIGGER and GAUGE
                controlTemplate = new StatelessTemplate(controlId);
                break;
        }

        return createStatefulControl(control, controlTemplate);
    }

    @SuppressLint("WrongConstant")
    @NonNull
    public static Control createStatelessControl(MqttControl control, boolean forRequestAdd) {
        // If this stateless control is for feeding ControlsProviderService.requestAddControl()
        // then adding a custom icon crashes the system UI for whatever reason
        return new Control.StatelessBuilder(control.getId() + "", createDummyPendingIntent())
                .setTitle(control.getName())
                .setSubtitle(control.getSubtitle())
                .setDeviceType(control.getFlavour())
                //.setStructure(control.getGroup())
                //.setZone(control.getGroup())
                .setCustomIcon(control.isCustomFlavour() && !forRequestAdd ? Icon.createWithBitmap(Utils.getCustomIconBitmap(control.getCustomIcon(), false)) : null)
                .setCustomColor(control.isCustomFlavour() ? ColorStateList.valueOf(control.getCustomColor()) : null)
                .build();
    }

    @SuppressLint("WrongConstant")
    @NonNull
    private Control createStatefulControl(MqttControl control, ControlTemplate template) {
        /*Intent intent = new Intent(this, MainActivity.class)
                .putExtra(EXTRA_MESSAGE, "$title $state")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);*/

        PendingIntent pendingIntent;
        if (control.isTrigger())
            pendingIntent = createDummyPendingIntent();
        else {
            Intent intent = new Intent(this, ControlActivity.class)
                    //.putExtra("control", control)
                    .putExtra("controlId", control.getId())
                    .putExtra("canDelete", false)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK);
            pendingIntent = PendingIntent.getActivity(
                    this,
                    control.getId(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
        }

        String statusText = "";
        if (control.isGauge())
            statusText += control.controlStatus.payload;
        else if (control.isTrigger())
            statusText += "Tap to Trigger";
        else {
            if (control.isToggle() || control.isToggleRange()) {
                if (control.getStateLabelFromPayload())
                    statusText += control.controlStatus.state ? control.getStateOnPayload() : control.getStateOffPayload();
                else
                    statusText += control.controlStatus.state ? "On" : "Off";
            }
            if (control.isToggleRange() && control.controlStatus.state)
                statusText += " •";
            /*if (control.isRange())
                statusText += control.controlStatus.getValue();*/
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new Control.StatefulBuilder(control.getId() + "", pendingIntent)
                    .setTitle(control.getName())
                    .setSubtitle(control.getSubtitle())
                    .setControlTemplate(template)
                    .setDeviceType(control.getFlavour())
                    .setStructure(control.getGroup())
                    //.setZone(control.getGroup()) // Zones are pretty much useless? They only appear in the add controls screen, at least on AOSP
                    .setCustomIcon(control.isCustomFlavour() ? Icon.createWithBitmap(Utils.getCustomIconBitmap(control.getCustomIcon(), false)) : null)
                    .setCustomColor(control.isCustomFlavour() ? ColorStateList.valueOf(control.getCustomColor()) : null)
                    .setAuthRequired(control.getNeedsUnlocking())
                    .setStatus(Control.STATUS_OK)
                    .setStatusText(statusText)
                    .build();
        }
        else {
            return new Control.StatefulBuilder(control.getId() + "", pendingIntent)
                    .setTitle(control.getName())
                    .setSubtitle(control.getSubtitle())
                    .setControlTemplate(template)
                    .setDeviceType(control.getFlavour())
                    .setStructure(control.getGroup())
                    //.setZone(control.getGroup())
                    .setCustomIcon(control.isCustomFlavour() ? Icon.createWithBitmap(Utils.getCustomIconBitmap(control.getCustomIcon(), false)) : null)
                    .setCustomColor(control.isCustomFlavour() ? ColorStateList.valueOf(control.getCustomColor()) : null)
                    .setStatus(Control.STATUS_OK)
                    .setStatusText(statusText)
                    .build();
        }
    }

    private static PendingIntent createDummyPendingIntent() {
        Intent dummyIntent = new Intent();
        Context ctx;
        if (instance != null)
            ctx = instance.getBaseContext();
        else
            ctx = MqttDroidApp.appContext;
        return PendingIntent.getActivity(ctx, 0, dummyIntent, PendingIntent.FLAG_IMMUTABLE);
    }
}
