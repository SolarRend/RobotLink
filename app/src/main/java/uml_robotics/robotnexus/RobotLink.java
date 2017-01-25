package uml_robotics.robotnexus;

import android.graphics.Color;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

/**
 * view for communicating with a particular robot
 */
public class RobotLink extends AppCompatActivity {
    private ArrayList<Robot> model; // view's copy of the model
    private Robot robot = null; // the robot we are linked with
    private ModelUpdate modelUpdate; // responsible for keeping view's model up to date
    private Handler robotLinkHandler; //handler to manipulate robot link UI
    private LinearLayout scrollLayout; // view for appending progressions on

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

        // setting robot's image in toolbar
        ActionBar actionBar = getSupportActionBar(); // getting this activity's actionbar
        ImageView robotImage = new ImageView(actionBar.getThemedContext());
        robotImage.setImageResource(robot.getImage());
        ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(
                120, //works for junior image
                170, //works for junior image
                Gravity.END | Gravity.CENTER_VERTICAL);
        layoutParams.rightMargin = 50;
        robotImage.setLayoutParams(layoutParams);
        toolbar.addView(robotImage);


        //set status of robot on UI
        ImageView statusImage = (ImageView)findViewById(R.id.statusIconView);
        String status = robot.getCurrState();
        if (status.equals("ok")) {
            statusImage.setImageResource(R.drawable.svg_ok);
        } else if (status.equals("safe")) {
            statusImage.setImageResource(R.drawable.svg_safe);
        } else if (status.equals("dangerous")) {
            statusImage.setImageResource(R.drawable.svg_dangerous);
        } else if (status.equals("help")) {
            statusImage.setImageResource(R.drawable.svg_help);
        } else if (status.equals("off")) {
            statusImage.setImageResource(R.drawable.svg_off);
        }

        scrollLayout = (LinearLayout)findViewById(R.id.scroll_layout);

        displayProgression(robot.getProgression());

        /**
         * MOCK-UP progression dialog
         */
        /*
        // Layout that is the progression box
        RelativeLayout relativeLayout = new RelativeLayout(RobotLink.this);

        // parameters for relative layout
        LinearLayout.LayoutParams relParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        // setting margins for the layout to keep progressions separated
        relParams.setMargins(0, 0, 0, 75);

        // setting parameters for layout
        relativeLayout.setLayoutParams(relParams);

        //changing layouts background color to match theme
        relativeLayout.setBackgroundColor(Color.WHITE);

        // content view of the progression
        TextView contentView = new TextView(RobotLink.this);
        contentView.setText("Would you like me to stop?"); // textual content
        contentView.setTextSize(20); // size of text
        contentView.setTextColor(Color.BLACK); // color of text
        contentView.setId(View.generateViewId()); // generating random id for view

        //parameters for content view
        RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);

        // making text start at left hand side of progression box
        textParams.addRule(RelativeLayout.LEFT_OF);
        textParams.setMargins(20, 30, 0, 30); // margin for content to not be so close to edge
        contentView.setLayoutParams(textParams); // setting parameters for the content view
        relativeLayout.addView(contentView); // appending content into our progression box;


        // making horizontal linear layout to contain buttons
        LinearLayout buttonLayout = new LinearLayout(RobotLink.this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        RelativeLayout.LayoutParams buttonLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        buttonLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        buttonLayoutParams.addRule(RelativeLayout.BELOW, contentView.getId());
        buttonLayoutParams.bottomMargin = 30;
        buttonLayout.setLayoutParams(buttonLayoutParams);


        // Making buttons for each response
        //parameters for button view
        LinearLayout.LayoutParams buttonParams1 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        buttonParams1.rightMargin = 30;
        LinearLayout.LayoutParams buttonParams2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        Button responseButton1 = new Button(RobotLink.this);
        Button responseButton2 = new Button(RobotLink.this);
        responseButton1.setTransformationMethod(null); // remove all caps
        responseButton2.setTransformationMethod(null);
        responseButton1.setText("Yes"); // value of response
        responseButton2.setText("No"); // value of response
        responseButton1.setTextSize(20);
        responseButton2.setTextSize(20);
        responseButton1.setTextColor(Color.BLACK);
        responseButton2.setTextColor(Color.BLACK);
        responseButton1.setBackgroundColor(getResources().getColor(R.color.colorAccent));
        responseButton2.setBackgroundColor(getResources().getColor(R.color.colorAccent));
        responseButton1.setLayoutParams(buttonParams1);
        responseButton2.setLayoutParams(buttonParams2);
        responseButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //parameters for response view
                LinearLayout.LayoutParams responseParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL);

                responseParams.setMargins(20, 0, 0, 130);
                TextView responseText = new TextView(RobotLink.this);
                responseText.setTextSize(22);
                responseText.setTextColor(Color.BLACK);
                responseText.setLayoutParams(responseParams);
                responseText.setText("Your Response: " + ((Button)v).getText());
                ((LinearLayout)findViewById(R.id.scroll_layout)).addView(responseText);
            }
        });

        responseButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //parameters for response view
                LinearLayout.LayoutParams responseParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL);

                responseParams.setMargins(20, 0, 0, 130);
                TextView responseText = new TextView(RobotLink.this);
                responseText.setTextSize(22);
                responseText.setTextColor(Color.BLACK);
                responseText.setLayoutParams(responseParams);
                responseText.setText("Your Response: " + ((Button)v).getText());
                ((LinearLayout)findViewById(R.id.scroll_layout)).addView(responseText);
            }
        });
        buttonLayout.addView(responseButton1);
        buttonLayout.addView(responseButton2);



        // appending
        LinearLayout layout = (LinearLayout)findViewById(R.id.scroll_layout);
        relativeLayout.addView(buttonLayout);
        layout.addView(relativeLayout);
        */
        /**
         * END MOCK-UP
         */

    }

    @Override
    protected void onStart() {
        super.onStart();

        // setting progression dialog scroll view to end
        final ScrollView scrollView = ((ScrollView) findViewById(R.id.scroll_view));
        scrollView.postDelayed(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        }, 75);

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
        Robot robotPrime;

        @Override
        public void run() {
            while (keepAlive) {

                modelPrime = ControllerService.getModel();

                for (Robot bot : modelPrime) {
                    if (robot.getId().equals(bot.getId())) {
                        robotPrime = bot;
                        break;
                    }
                }

                // if this robot is now ignored exit activity
                if (!robotPrime.isVisible()) {
                    RobotLink.this.finish();
                    Log.i("RobotLink.Update", "Finishing, " + robotPrime.getId());
                    return;
                }

                // if the robots' progressions or status' don't match up
                if ((!(robotPrime.getCurrState().equals(robot.getCurrState())))
                        || (!(robotPrime.getProgression().toString().equals(robot.getProgression().toString())))) {

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
                                        statusImage.setImageResource(R.drawable.svg_ok);
                                    } else if (status.equals("safe")) {
                                        statusImage.setImageResource(R.drawable.svg_safe);
                                    } else if (status.equals("dangerous")) {
                                        statusImage.setImageResource(R.drawable.svg_dangerous);
                                    } else if (status.equals("help")) {
                                        statusImage.setImageResource(R.drawable.svg_help);
                                    } else if (status.equals("off")) {
                                        statusImage.setImageResource(R.drawable.svg_off);
                                    }

                                    scrollLayout.removeAllViews();

                                    displayProgression(robot.getProgression());
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

    private void displayProgression(JSONArray progression) {
        //if (progression == null) {
        //    return;
        //}

        /**
         * loop through all objects in progression
         */
        for (int i = 0; i < progression.length(); i++) {

            try {
                // get this element in the progression
                final JSONObject progressionElement = progression.getJSONObject(i);

                //display the content
                TextView contentView = new TextView(RobotLink.this);
                contentView.setText(progressionElement.getString("content")); // textual content
                contentView.setTextSize(20); // size of text
                contentView.setTextColor(Color.BLACK); // color of text
                contentView.setId(View.generateViewId()); // generating random id for view

                //parameters for content view
                LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

                textParams.setMargins(20, 50, 0, 10); // margin for content to not be so close to edge
                contentView.setLayoutParams(textParams); // setting parameters for the content view

                scrollLayout.addView(contentView);

                // get responses which will be turned into buttons
                JSONArray responses = progressionElement.getJSONArray("responses");


                // Layout that is the progression box
                RelativeLayout relativeLayout = new RelativeLayout(RobotLink.this);

                // parameters for relative layout
                LinearLayout.LayoutParams relParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

                // setting margins for the layout to keep progressions separated
                relParams.setMargins(0, 0, 0, 75);

                // setting parameters for layout
                relativeLayout.setLayoutParams(relParams);

                //changing layouts background color to match theme
                relativeLayout.setBackgroundColor(Color.WHITE);

                // making horizontal linear layout to contain buttons
                LinearLayout buttonLayout = new LinearLayout(RobotLink.this);
                buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
                RelativeLayout.LayoutParams buttonLayoutParams = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT
                );
                buttonLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
                buttonLayout.setLayoutParams(buttonLayoutParams);

                /**
                 * loop through all responses and make buttons for them
                 */
                for (int j = 0; j < responses.length(); j++) {
                    // get this element in the response
                    final JSONObject responseElement = responses.getJSONObject(j);

                    //parameters for button view
                    LinearLayout.LayoutParams buttonParams1 = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    buttonParams1.rightMargin = 10;
                    Button responseButton1 = new Button(RobotLink.this);
                    responseButton1.setTransformationMethod(null); // remove all caps
                    responseButton1.setText(responseElement.getString("value")); // value of response
                    responseButton1.setTextSize(20);
                    responseButton1.setTextColor(Color.BLACK);
                    responseButton1.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                    responseButton1.setLayoutParams(buttonParams1);
                    responseButton1.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                ControllerService.addToReplyQueue(robot.getId(),
                                        progressionElement.getString("msgid"), responseElement.getString("id"));
                            } catch (JSONException ex) {
                                StringWriter stringWriter = new StringWriter();
                                PrintWriter printWriter = new PrintWriter(stringWriter, true);
                                ex.printStackTrace(printWriter);
                                Log.e("RobotLink.Progression", stringWriter.toString());
                            }
                        }
                    });
                    buttonLayout.addView(responseButton1);
                }

                scrollLayout.addView(buttonLayout);

            } catch (JSONException ex) {
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter, true);
                ex.printStackTrace(printWriter);
                Log.e("RobotLink.Progression", stringWriter.toString());
            }

        }
    }
}
