package uml_robotics.robotnexus;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ControllerService extends Service {

    public ControllerService() {
    }


    @Override
    public void onCreate() {
        Log.i("Service.onCreate()", "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    private void onStartCommandSeparateThread(Intent intent, int flags, int startId) {
        final String TAG = "onStartSeparateThread";
    }

    @Override
    public void onDestroy() {

        Log.i("Service.onDestroy()", "Destroyed");
    }

    /**
     *returns null -> this is a pure started service
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

