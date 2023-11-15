package lightjockey.mqttdroid;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MqttControlDao {
    @Query("SELECT * FROM controls")
    LiveData<List<MqttControl>> getAllControlsObservable();
    @Query("SELECT * FROM controls")
    List<MqttControl> getAllControls();

    @Query("SELECT * FROM controls WHERE id = :id")
    MqttControl getControl(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(MqttControl control);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertAll(List<MqttControl> controls);

    @Delete
    void delete(MqttControl control);
    @Query("DELETE FROM controls")
    void deleteAll();
}
