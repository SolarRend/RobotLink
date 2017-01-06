package uml_robotics.robotnexus;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class RobotNexus extends AppCompatActivity {
    private ArrayList<Robot> model; // view's copy of the model
    private ModelUpdate modelUpdate; // responsible for keeping view's model up to date
    private RobotNavListAdapter displayAdapter; //adapter for listview to display robot info
    private Handler robotNexusHandler; //handler to manipulate robot nexus UI

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_robot_nexus);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        model = ControllerService.getModel(); // getting view's copy of the model

        robotNexusHandler = new Handler(); // getting this view's thread


        //setting up ui robot list
        Integer images[]  = new Integer[model.size()];
        Robot robots[] = new Robot[model.size()];
        int i = 0;
        for (Robot bot : model) {
            robots[i] = bot;
            images[i] = bot.getImage();
            i++;
        }
        displayAdapter = new RobotNavListAdapter(RobotNexus.this, robots, images);
        ListView listView = (ListView) findViewById(R.id.robot_list);
        listView.setAdapter(displayAdapter);


    }

    @Override
    protected void onStart() {
        super.onStart();
        modelUpdate = new ModelUpdate();
        modelUpdate.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // end our update
        modelUpdate.close();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private class ModelUpdate extends Thread {
        private boolean keepAlive = true;
        ArrayList<Robot> modelPrime;
        @Override
        public void run() {
            while (keepAlive) {

                modelPrime = ControllerService.getModel();

                // if our models don't match up
                if (!(modelPrime.containsAll(model) && model.containsAll(modelPrime))) {

                    model = ControllerService.getModel();
                    Log.i("RobotNexus.ModelUpdate", "Model changed");

                    //setting up ui robot list
                    Integer images[]  = new Integer[model.size()];
                    Robot robots[] = new Robot[model.size()];
                    int i = 0;
                    for (Robot bot : model) {
                        robots[i] = bot;
                        images[i] = bot.getImage();
                        i++;
                    }
                    displayAdapter = new RobotNavListAdapter(RobotNexus.this, robots, images);

                    robotNexusHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            ListView listView = (ListView) findViewById(R.id.robot_list);
                            listView.setAdapter(displayAdapter);
                            displayAdapter.notifyDataSetChanged();
                            //Toast.makeText(RobotNexus.this, "Update", Toast.LENGTH_SHORT).show();
                        }
                    });

                }

                try {
                    sleep(300);
                } catch (InterruptedException ex) {

                }

            }
        }

        public void close() {
            keepAlive = false;
        }
    }
}
