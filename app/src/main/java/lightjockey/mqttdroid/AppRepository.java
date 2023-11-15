package lightjockey.mqttdroid;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

public class AppRepository {
    private final MqttControlDao mqttControlDao;
    private final LiveData<List<MqttControl>> mqttControls;

    public AppRepository(Application application) {
        mqttControlDao = AppDatabase.getDatabase(application).mqttControlDao();

        mqttControls = mqttControlDao.getAllControlsObservable();
    }

    public LiveData<List<MqttControl>> getAllControlsObservable() {
        return mqttControls;
    }
    public List<MqttControl> getAllControls() {
        return mqttControlDao.getAllControls();
    }

    public MqttControl getControl(int id) {
        return mqttControlDao.getControl(id);
    }

    public void insert(MqttControl control) {
        mqttControlDao.insert(control);
    }
    public List<Long> insertAll(List<MqttControl> controls) {
        return mqttControlDao.insertAll(controls);
    }

    public void delete(MqttControl control) {
        mqttControlDao.delete(control);
    }
    public void deleteAll() {
        mqttControlDao.deleteAll();
    }
}
