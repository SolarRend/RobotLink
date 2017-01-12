package uml_robotics.robotnexus;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

/*
 * DON'T do heavy work in CALLBACKS (~3 threads in pool )
 *
 */

/*
 * * Dec 22, 2016
 * * created project
 * created controller and RobotSelector view
 * created Robot object that the model will contain an array list of
 * finished onstartcommand stuff in controllerservice
 *
 *
 * * Dec 28, 2016
 * * changed theme of app
 *
 *
 * * Dec 30, 2016
 * * worked on new dialog protocol
 *
 */

public class MainActivity extends AppCompatActivity {
    private Intent controllerIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //boot up service
        controllerIntent = new Intent(this, ControllerService.class);
        this.startService(controllerIntent);
        Log.i("MAIN.onCreate()", "Started Controller");


        // starting the *real main activity* which will be a navigation screen for all robots in the area
        this.startActivity(new Intent(MainActivity.this, RobotSelector.class));
        Log.i("MAIN.onCreate()", "Transitioning to RobotSelector activity");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //this.stopService(controllerIntent);
        //Log.i("MAIN.onDestroy()", "Stopped Controller");

    }
}
