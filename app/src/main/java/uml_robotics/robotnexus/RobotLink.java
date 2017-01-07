package uml_robotics.robotnexus;

import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * view for communicating with a particular robot
 */
public class RobotLink extends AppCompatActivity {
    private ArrayList<Robot> model; // view's copy of the model
    private Robot robot = null; // the robot we are linked with
    private ModelUpdate modelUpdate; // responsible for keeping view's model up to date
    private Handler robotLinkHandler; //handler to manipulate robot link UI

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_robot_link);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        model = ControllerService.getModel(); // getting view's copy of the model

        robotLinkHandler = new Handler(); // getting this view's thread

        // getting id of robot given to us
        String robotID = getIntent().getStringExtra("EXTRA_ROBOT_ID");


        // finding robot in our model
        for (Robot bot: model) {
            if (robotID.equals(bot.getId())) {
                robot = bot;
                break;
            }
        }

        // setting title
        setTitle(robot.getName());

        //set status of robot on UI
        ImageView statusImage = (ImageView)findViewById(R.id.statusIconView);
        String status = robot.getCurrState();
        if (status.equals("ok")) {
            statusImage.setImageResource(R.drawable.ok);
        } else if (status.equals("safe")) {
            statusImage.setImageResource(R.drawable.safe);
        } else if (status.equals("dangerous")) {
            statusImage.setImageResource(R.drawable.dangerous);
        } else if (status.equals("help")) {
            statusImage.setImageResource(R.drawable.help);
        } else if (status.equals("off")) {
            statusImage.setImageResource(R.drawable.off);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // start model update
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
                    Log.i("RobotLink.Update", "Model changed");

                    // finding robot in our model
                    for (Robot bot: model) {
                        if (robot.getId().equals(bot.getId())) {
                            robot = bot;
                            robotLinkHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    //set status of robot on UI
                                    ImageView statusImage = (ImageView)findViewById(R.id.statusIconView);
                                    String status = robot.getCurrState();
                                    if (status.equals("ok")) {
                                        statusImage.setImageResource(R.drawable.ok);
                                    } else if (status.equals("safe")) {
                                        statusImage.setImageResource(R.drawable.safe);
                                    } else if (status.equals("dangerous")) {
                                        statusImage.setImageResource(R.drawable.dangerous);
                                    } else if (status.equals("help")) {
                                        statusImage.setImageResource(R.drawable.help);
                                    } else if (status.equals("off")) {
                                        statusImage.setImageResource(R.drawable.off);
                                    }
                                }
                            });
                            break;
                        }
                    }
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
