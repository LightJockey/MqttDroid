package lightjockey.mqttdroid.ui.controls;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Objects;

import lightjockey.mqttdroid.MqttControl;
import lightjockey.mqttdroid.databinding.RecyclerviewControlitemBinding;
import lightjockey.mqttdroid.ui.helpers.Utils;

public class ControlListAdapter extends ListAdapter<MqttControl, ControlListAdapter.ControlViewHolder> {
    public ControlListAdapter() {
        super(new ControlListAdapter.ControlDiff());
    }

    @NonNull
    @Override
    public ControlViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        RecyclerviewControlitemBinding binding = RecyclerviewControlitemBinding.inflate(layoutInflater, parent, false);
        return new ControlViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ControlViewHolder holder, int position) {
        MqttControl current = getItem(position);
        holder.bind(current);
    }

    public static class ControlViewHolder extends RecyclerView.ViewHolder {
        private final RecyclerviewControlitemBinding binding;

        public ControlViewHolder(RecyclerviewControlitemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(MqttControl control) {
            binding.textViewName.setText(control.getName());
            binding.textViewSubtitle.setText(control.getSubtitle());
            for (MqttControl.EControlType type : MqttControl.EControlType._VALUES) {
                if (type.getIndex() == control.getType())
                    binding.textViewType.setText(type.getResString());
            }
            for (MqttControl.EControlFlavour flavour : MqttControl.EControlFlavour._VALUES) {
                if (flavour.getIndex() == control.getFlavour())
                    binding.textViewFlavour.setText(flavour.getResString());
            }

            binding.getRoot().setOnClickListener(view -> {
                Intent intent = new Intent(view.getContext(), ControlActivity.class)
                        .putExtra("controlId", control.getId());
                view.getContext().startActivity(intent);
            });

            binding.imageViewDelete.setOnClickListener(view -> Utils.deleteControlDialog(view.getContext(), control, null));
        }
    }

    private static class ControlDiff extends DiffUtil.ItemCallback<MqttControl> {
        @Override
        public boolean areItemsTheSame(@NonNull MqttControl oldItem, @NonNull MqttControl newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull MqttControl oldItem, @NonNull MqttControl newItem) {
            return Objects.equals(oldItem, newItem);
        }
    }
}
