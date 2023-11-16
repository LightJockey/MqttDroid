package lightjockey.mqttdroid.ui.controls;

import android.content.ComponentName;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.service.controls.ControlsProviderService;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.InverseMethod;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputLayout;
import com.maltaisn.icondialog.IconDialog;
import com.maltaisn.icondialog.IconDialogSettings;
import com.maltaisn.icondialog.data.Icon;
import com.maltaisn.icondialog.filter.DefaultIconFilter;
import com.maltaisn.icondialog.pack.IconPack;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import lightjockey.mqttdroid.MqttDroidControlsService;
import lightjockey.mqttdroid.MqttDroidApp;
import lightjockey.mqttdroid.MqttClient;
import lightjockey.mqttdroid.MqttControl;
import lightjockey.mqttdroid.MqttControlsViewModel;
import lightjockey.mqttdroid.R;
import lightjockey.mqttdroid.databinding.ActivityControlBinding;
import lightjockey.mqttdroid.ui.helpers.MaterialSpinnerAdapter;
import lightjockey.mqttdroid.ui.helpers.Utils;

public class ControlActivity extends AppCompatActivity implements IconDialog.Callback {
    private static final HashMap<Integer, String> CONTROLTYPE_SPINNER_MAP = new HashMap<>();
    private static final HashMap<Integer, String> CONTROLFLAVOUR_SPINNER_MAP = new HashMap<>();
    private static final HashMap<Integer, String> CONTROLTOPICQOS_SPINNER_MAP = new HashMap<>();

    private ActivityControlBinding binding;
    private IconDialog iconDialog;
    private AlertDialog colorPickerDialog;

    private MqttControlsViewModel controlsViewModel;
    private MqttControl controlSnapshot;
    private MqttControl control;

    public boolean canDelete;
    public boolean isNew;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setNavigationBarColor(getColor(android.R.color.transparent));

        controlsViewModel = new ViewModelProvider(this).get(MqttControlsViewModel.class);
        int controlId = getIntent().getIntExtra("controlId", -1);
        control = controlsViewModel.getControl(controlId);
        canDelete = getIntent().getBooleanExtra("canDelete", true);
        isNew = controlId == -1;
        if (control == null)
            control = MqttControl.Light_Toggle();
        if (canDelete && isNew)
            canDelete = false;
        controlSnapshot = new MqttControl(control);

        binding = ActivityControlBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.setControl(control);
        binding.setControlActivity(this);

        CONTROLTYPE_SPINNER_MAP.clear();
        for (MqttControl.EControlType type : MqttControl.EControlType._VALUES)
            CONTROLTYPE_SPINNER_MAP.put(type.getIndex(), getResources().getString(type.getResString()));
        binding.textViewControlTypeSpinner.setAdapter(new MaterialSpinnerAdapter(this,
                android.R.layout.simple_spinner_dropdown_item,
                CONTROLTYPE_SPINNER_MAP.values().stream().sorted().collect(Collectors.toList()) ));

        CONTROLFLAVOUR_SPINNER_MAP.clear();
        for (MqttControl.EControlFlavour type : MqttControl.EControlFlavour._VALUES)
            CONTROLFLAVOUR_SPINNER_MAP.put(type.getIndex(), getResources().getString(type.getResString() ));
        binding.textViewControlFlavourSpinner.setAdapter(new MaterialSpinnerAdapter(this,
                android.R.layout.simple_spinner_dropdown_item,
                CONTROLFLAVOUR_SPINNER_MAP.values().stream().sorted().collect(Collectors.toList()) ));

        CONTROLTOPICQOS_SPINNER_MAP.clear();
        for (MqttControl.ControlTopic.EQos qos : MqttControl.ControlTopic.EQos._VALUES)
            CONTROLTOPICQOS_SPINNER_MAP.put(qos.getIndex(), getResources().getString(qos.getResString()));
        MaterialSpinnerAdapter qosSpinnerAdapter = new MaterialSpinnerAdapter(this,
                android.R.layout.simple_spinner_dropdown_item,
                new ArrayList<>(CONTROLTOPICQOS_SPINNER_MAP.values()));
        binding.mqttTopicTrigger.spinnerQos.setAdapter(qosSpinnerAdapter);
        binding.mqttTopicState.spinnerQos.setAdapter(qosSpinnerAdapter);
        binding.mqttTopicValue.spinnerQos.setAdapter(qosSpinnerAdapter);

        setCustomIconDrawable(control.getCustomIcon());
        iconDialog = (IconDialog)getSupportFragmentManager().findFragmentByTag(IconDialog.class.getName());
        if (iconDialog == null) {
            IconDialogSettings settings = new IconDialogSettings(new DefaultIconFilter(),
                    IconDialog.SearchVisibility.ALWAYS,
                    IconDialog.HeadersVisibility.STICKY,
                    IconDialog.TitleVisibility.NEVER,
                    R.string.icd_title,
                    1,
                    false,
                    false,
                    true);
            iconDialog = IconDialog.newInstance(settings);
        }
        binding.imageViewCustomIcon.setOnClickListener(view -> {
            MqttDroidApp.appInstance.iconPack.loadDrawables(((MqttDroidApp)getApplication()).iconPackLoader.getDrawableLoader());
            iconDialog.show(getSupportFragmentManager(), IconDialog.class.getName());
        });

        setCustomColorDrawable(control.getCustomColor());
        colorPickerDialog = new ColorPickerDialog.Builder(this)
                .setPreferenceName(ControlActivity.class.getName())
                .setPositiveButton("Select", (ColorEnvelopeListener) (envelope, fromUser) -> {
                    control.setCustomColor(envelope.getColor());
                    setCustomColorDrawable(envelope.getColor());
                })
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                .attachAlphaSlideBar(false)
                .attachBrightnessSlideBar(true)
                .setBottomSpace(12)
                .create();
        binding.imageViewCustomColor.setOnClickListener(view -> colorPickerDialog.show());

        binding.layoutNeedsUnlocking.setVisibility(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setupValidateEvents();
    }

    @Override
    public void onBackPressed() {
        if (control.equals(controlSnapshot)) {
            super.onBackPressed();
            return;
        }
        Utils.showPositiveNegativeActionsDialog(this,
                "Discard Changes?",
                "If you proceed, all changes you've made to this control will be lost!",
                "Discard",
                (dialog, which) -> super.onBackPressed(),
                getString(android.R.string.cancel),
                null);
    }

    public void save() {
        if (!validate())
            return;

        MqttClient.unbindControl(controlSnapshot);
        controlsViewModel.insert(control);
        MqttClient.bindControl(control);

        if (MqttDroidControlsService.instance != null)
            MqttDroidControlsService.instance.updateControl(control, true);

        ControlsProviderService.requestAddControl(this,
                new ComponentName(getApplicationContext(), MqttDroidControlsService.class),
                MqttDroidControlsService.createStatelessControl(control, true));

        finish();
    }
    public void delete() {
        if (!canDelete)
            return;

        Utils.deleteControlDialog(this, controlSnapshot, this::finish);
    }

    private void setupValidateEvents() {
        final TextWatcher e = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) { validate(); }
        };
        Objects.requireNonNull(binding.textInputLayoutName.getEditText()).addTextChangedListener(e);
        Objects.requireNonNull(binding.mqttTopicTrigger.textInputLayoutTopic.getEditText()).addTextChangedListener(e);
        Objects.requireNonNull(binding.textInputLayoutGaugeTopic.getEditText()).addTextChangedListener(e);
        Objects.requireNonNull(binding.mqttTopicState.textInputLayoutTopic.getEditText()).addTextChangedListener(e);
        Objects.requireNonNull(binding.textInputLayoutStateOnPayload.getEditText()).addTextChangedListener(e);
        Objects.requireNonNull(binding.textInputLayoutStateOffPayload.getEditText()).addTextChangedListener(e);
        Objects.requireNonNull(binding.mqttTopicValue.textInputLayoutTopic.getEditText()).addTextChangedListener(e);
        Objects.requireNonNull(binding.textInputLayoutValueMin.getEditText()).addTextChangedListener(e);
        Objects.requireNonNull(binding.textInputLayoutValueMax.getEditText()).addTextChangedListener(e);
        Objects.requireNonNull(binding.textInputLayoutValueStep.getEditText()).addTextChangedListener(e);
    }
    private boolean validate() {
        int errors = 0;

        errors += Utils.boolToInt(validateFieldEmpty(control.getName(), binding.textInputLayoutName));

        if (control.isTrigger())
            errors += Utils.boolToInt(validateFieldEmpty(control.triggerTopic.getTopic(), binding.mqttTopicTrigger.textInputLayoutTopic));
        if (control.isGauge())
            errors += Utils.boolToInt(validateFieldEmpty(control.getGaugeTopic(), binding.textInputLayoutGaugeTopic));
        if (control.isToggle() || control.isToggleRange()) {
            errors += Utils.boolToInt(validateFieldEmpty(control.stateTopic.getTopic(), binding.mqttTopicState.textInputLayoutTopic));
            errors += Utils.boolToInt(validateFieldEmpty(control.getStateOnPayload(), binding.textInputLayoutStateOnPayload));
            errors += Utils.boolToInt(validateFieldEmpty(control.getStateOffPayload(), binding.textInputLayoutStateOffPayload));
        }
        if (control.isRange() || control.isToggleRange()) {
            errors += Utils.boolToInt(validateFieldEmpty(control.valueTopic.getTopic(), binding.mqttTopicValue.textInputLayoutTopic));
            errors += Utils.boolToInt(validateFieldNaN(Objects.requireNonNull(binding.textInputLayoutValueMin.getEditText()).getText().toString(),
                    binding.textInputLayoutValueMin,
                    true));
            if (control.getValueMin() >= control.getValueMax()) {
                binding.textInputLayoutValueMin.setError("Min value should be < than max value!");
                errors++;
            }
            errors += Utils.boolToInt(validateFieldNaN(Objects.requireNonNull(binding.textInputLayoutValueMax.getEditText()).getText().toString(),
                    binding.textInputLayoutValueMax,
                    false));
            if (control.getValueMax() <= control.getValueMin()) {
                binding.textInputLayoutValueMax.setError("Max value should be > than min value!");
                errors++;
            }
            errors += Utils.boolToInt(validateFieldNaN(Objects.requireNonNull(binding.textInputLayoutValueStep.getEditText()).getText().toString(),
                    binding.textInputLayoutValueStep,
                    false));
            if (control.getValueStep() >= control.getValueMax() - control.getValueMin()) {
                binding.textInputLayoutValueStep.setError("Step value should be < than max - min!");
                errors++;
            }
        }

        return errors == 0;
    }
    private static boolean validateFieldEmpty(String input, TextInputLayout textInputLayout) {
        boolean isEmpty = false;
        if (Utils.isStringNullOrEmpty(input)) {
            textInputLayout.setError(textInputLayout.getHint() + " cannot be empty!");
            isEmpty = true;
        }
        else
            textInputLayout.setError(null);
        return isEmpty;
    }
    private static boolean validateFieldNaN(String input, TextInputLayout textInputLayout, boolean isZeroValid) {
        boolean isNaN = false;
        // Positive whole or decimal number regEx: https://regex101.com/r/hR3hI3/1
        if (Utils.isStringNullOrEmpty(input) || !input.matches("^(0|[1-9]\\d*)(\\.\\d+)?$")) {
            textInputLayout.setError(textInputLayout.getHint() + " must be a valid number!");
            isNaN = true;
        }
        else if (!isZeroValid && input.equals("0")) {
            textInputLayout.setError(textInputLayout.getHint() + " must not be 0!");
            isNaN = true;
        }
        else
            textInputLayout.setError(null);
        return isNaN;
    }

    private void setCustomIconDrawable(int iconId) {
        binding.imageViewCustomIcon.setImageBitmap(Utils.getCustomIconBitmap(iconId, true));
    }
    private void setCustomColorDrawable(int color) {
        GradientDrawable customColorDrawable = new GradientDrawable();
        customColorDrawable.setShape(GradientDrawable.OVAL);
        customColorDrawable.setColor(color);
        customColorDrawable.setStroke(8, Utils.getThemePrimaryColorInverted(getApplicationContext()));

        binding.imageViewCustomColor.setImageDrawable(customColorDrawable);
    }

    @InverseMethod("controlType_indexToName")
    public int controlType_nameToIndex(String string) {
        for (Map.Entry<Integer, String> entry : CONTROLTYPE_SPINNER_MAP.entrySet()) {
            if (entry.getValue().equals(string))
                return entry.getKey();
        }
        return 0;
    }
    public String controlType_indexToName(int index) {
        return CONTROLTYPE_SPINNER_MAP.get(index);
    }

    @InverseMethod("controlFlavour_indexToName")
    public int controlFlavour_nameToIndex(String string) {
        for (Map.Entry<Integer, String> entry : CONTROLFLAVOUR_SPINNER_MAP.entrySet()) {
            if (entry.getValue().equals(string))
                return entry.getKey();
        }
        return 0;
    }
    public String controlFlavour_indexToName(int index) {
        return CONTROLFLAVOUR_SPINNER_MAP.get(index);
    }

    @InverseMethod("controlTopicQos_indexToName")
    public int controlTopicQos_nameToIndex(String string) {
        for (Map.Entry<Integer, String> entry : CONTROLTOPICQOS_SPINNER_MAP.entrySet()) {
            if (entry.getValue().equals(string))
                return entry.getKey();
        }
        return 0;
    }
    public String controlTopicQos_indexToName(int index) {
        return CONTROLTOPICQOS_SPINNER_MAP.get(index);
    }

    // ICON DIALOG HANDLERS
    ////////////////////////////////////////
    @Nullable
    @Override
    public IconPack getIconDialogIconPack() {
        return MqttDroidApp.appInstance.iconPack;
    }
    @Override
    public void onIconDialogIconsSelected(@NonNull IconDialog iconDialog, @NonNull List<Icon> list) {
        Icon icon = list.get(0);
        if (icon != null) {
            control.setCustomIcon(icon.getId());
            setCustomIconDrawable(icon.getId());
        }
    }
    @Override
    public void onIconDialogCancelled() { }
}
