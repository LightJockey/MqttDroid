package lightjockey.mqttdroid.ui.logs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import lightjockey.mqttdroid.MqttClient;
import lightjockey.mqttdroid.databinding.FragmentLogsBinding;

public class LogsFragment extends Fragment {
    private FragmentLogsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLogsBinding.inflate(inflater, container, false);

        try {
            Process process = Runtime.getRuntime().exec("logcat -d");

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            StringBuilder log = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains(MqttClient.TAG)) {
                    log.append(line);
                    log.append("\n");
                }
            }

            if (log.length() > 0)
                binding.textLogs.setText(log.toString());

            ScrollView scroller = binding.scrollerV;
            scroller.post(() -> scroller.fullScroll(View.FOCUS_DOWN));
        } catch (IOException ignored) { }

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}