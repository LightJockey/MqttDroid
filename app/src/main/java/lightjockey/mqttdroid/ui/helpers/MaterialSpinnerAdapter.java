package lightjockey.mqttdroid.ui.helpers;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import androidx.annotation.NonNull;

import java.util.List;

// This abomination is because Google lacks a proper material spinner.
// Credits to: https://rmirabelle.medium.com/there-is-no-material-design-spinner-for-android-3261b7c77da8
public class MaterialSpinnerAdapter extends ArrayAdapter<String> {
    private final List<String> values;

    public MaterialSpinnerAdapter(@NonNull Context context, int resource, @NonNull List<String> objects) {
        super(context, resource, objects);
        values = objects;
    }

    private final Filter filterThatDoesNothing = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            results.values = values;
            results.count = values.size();
            return results;
        }
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            notifyDataSetChanged();
        }
    };

    @NonNull
    @Override
    public Filter getFilter() {
        return filterThatDoesNothing;
    }
}
