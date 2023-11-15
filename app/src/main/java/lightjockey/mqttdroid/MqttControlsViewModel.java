package lightjockey.mqttdroid;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class MqttControlsViewModel extends AndroidViewModel {
    private final AppRepository repository;

    private LiveData<List<MqttControl>> allControls;

    public MqttControlsViewModel(@NonNull Application application) {
        super(application);
        repository = MqttDroidApp.appInstance.repository;
    }

    public LiveData<List<MqttControl>> getAllControls() {
        if (allControls != null)
            return allControls;
        else
            return allControls = repository.getAllControlsObservable();
    }

    public MqttControl getControl(int id) {
        return repository.getControl(id);
    }

    public void insert(MqttControl control) {
        repository.insert(control);
    }
    public void insertAll(List<MqttControl> controls) {
        repository.insertAll(controls);
    }

    public void delete(MqttControl control) {
        repository.delete(control);
    }
    public void deleteAll() {
        repository.deleteAll();
    }
}
