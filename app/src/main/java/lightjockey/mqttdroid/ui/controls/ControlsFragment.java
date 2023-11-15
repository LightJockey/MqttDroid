package lightjockey.mqttdroid.ui.controls;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import lightjockey.mqttdroid.MqttControlsViewModel;
import lightjockey.mqttdroid.databinding.FragmentControlsBinding;

public class ControlsFragment extends Fragment {
    private FragmentControlsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        MqttControlsViewModel controlsViewModel = new ViewModelProvider(this).get(MqttControlsViewModel.class);

        binding = FragmentControlsBinding.inflate(getLayoutInflater());
        binding.setViewModel(controlsViewModel);

        final RecyclerView recyclerView = binding.recyclerview;
        final ControlListAdapter adapter = new ControlListAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        controlsViewModel.getAllControls().observe(getViewLifecycleOwner(), (controls) -> {
            binding.layoutNoControlsHint.setVisibility(controls.size() > 0 ? View.GONE : View.VISIBLE);
            adapter.submitList(controls);
        });

        binding.fabAdd.setOnClickListener(view -> {
            Intent intent = new Intent(getActivity(), ControlActivity.class);
            startActivity(intent);
        });

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}