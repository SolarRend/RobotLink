package uml_robotics.robotnexus;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

/*
 * * Dec 22, 2016
 * * created project
 * created controller and RobotNexus view
 *
 */

public class MainActivity extends AppCompatActivity {
    private Intent serviceIntent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //boot up service
        serviceIntent = new Intent(this, ControllerService.class);
        this.startService(serviceIntent);
        Log.i("MAIN.onCreate()", "Started service");

        // starting the *real main activity* which will be a hub for all robots in the area
        this.startActivity(new Intent(MainActivity.this, RobotNexus.class));
        Log.i("MAIN.onCreate()", "Transitioning to Robot Nexus activity");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.stopService(serviceIntent);
        Log.i("MAIN.onDestroy()", "Stopped Service");
    }
}
