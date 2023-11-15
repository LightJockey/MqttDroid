package lightjockey.mqttdroid.ui.helpers;

import android.annotation.SuppressLint;
import android.widget.EditText;

import androidx.databinding.BindingAdapter;
import androidx.databinding.InverseBindingAdapter;

public class BindingUtils {
    @SuppressLint("DefaultLocale")
    @BindingAdapter("android:text")
    public static void setText(EditText view, float value) {
        if (Float.isNaN(value))
            view.setText("");
        else
            view.setText(Utils.formatFloatReadable(value));
        view.setSelection(view.length());
    }
    @InverseBindingAdapter(attribute = "android:text", event = "android:textAttrChanged")
    public static float getTextString(EditText view) {
        String num = view.getText().toString();
        if (num.isEmpty())
            return 0f;
        try {
            return Float.parseFloat(num);
        } catch (NumberFormatException e) {
            return 0f;
        }
    }
}
