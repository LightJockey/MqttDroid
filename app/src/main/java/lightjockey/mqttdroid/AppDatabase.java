package lightjockey.mqttdroid;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {MqttControl.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract MqttControlDao mqttControlDao();

    private static volatile AppDatabase instance;

    public static AppDatabase getDatabase(final Context ctx) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(ctx.getApplicationContext(), AppDatabase.class, "app_database")
                            .allowMainThreadQueries() // Can't be bothered for async queries on a schema this small
                            .addCallback(AppDatabaseCallback)
                            .build();
                }
            }
        }
        return instance;
    }

    // Called the first time the db is created
    private static final RoomDatabase.Callback AppDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);

            /*MqttControlDao dao = instance.mqttControlDao();
            dao.deleteAll();*/
        }
    };
}
