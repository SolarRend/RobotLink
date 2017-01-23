package uml_robotics.robotnexus;

import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.NotificationCompat;
import android.text.LoginFilter;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static java.lang.Thread.sleep;

public class NotificationViewService extends Service {
    private NotificationManager notifManager; // manager object for notification related events
    private ArrayList<Robot> model; // view's copy of the model
    private ModelUpdate modelUpdate; // responsible for keeping view's model up to date
    private Dialog dialog = null; // emergency dialog for popups

    public NotificationViewService() {}

    @Override
    public void onCreate() {
        Log.i("NotifView.onCreate()", "Service Created");

        notifManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        model = ControllerService.getModel();

        // starting model update
        modelUpdate = new ModelUpdate();
        modelUpdate.start();


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        // clear all notifications
        notifManager.cancelAll();
        modelUpdate.close();
        Log.i("NotifView.onDestroy()", "Destroyed");
    }

    /**
     *returns null -> this is a pure started service
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
                    Log.i("NotifView.Update", "Model changed");

                    //reverse this copy of the model to ensure closest robot is the top notif
                    Collections.reverse(model);

                    // ***displaying push notifications***

                    int id = 0; // for content intent and notification
                    ArrayList<Notification> closestThree = new ArrayList<>(3);
                    for (Robot bot : model) {
                        // building notification
                        // builder object
                        Notification.Builder notif = new Notification.Builder(NotificationViewService.this);

                        // get our custom notification layout
                        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.custom_push_notification);

                        // set the img of the robot
                        if (bot.getModel() == null) {
                            // safety-net for ack
                            remoteViews.setImageViewResource(R.id.push_notif_img, R.mipmap.ic_launcher);
                        } else {
                            remoteViews.setImageViewResource(R.id.push_notif_img, bot.getImage());
                        }

                        //setting robot's name
                        remoteViews.setTextViewText(R.id.push_notif_bot_name, bot.getName());

                        // setting icon of notification
                        notif.setSmallIcon(bot.getImage());

                        notif.setContent(remoteViews);

                        //notif.setLargeIcon(BitmapFactory.decodeResource(getResources(), bot.getImage()));


                        // setting title of notification
                        //notif.setContentTitle(bot.getName());

                        // setting textual content of notification - when notif isnt expanded
                        //notif.setContentText("Autonomous system is nearby.");

                        // Big style notification text
                        //notif.setStyle((new Notification.BigTextStyle()).bigText(
                                //"Autonomous system is nearby.\nTap for more information"
                        //));

                        //notif.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(
                          //      BitmapFactory.decodeResource(getResources(), R.drawable.dangerous)
                        //).setSummaryText("Autonomous system is nearby.\nTap for more information"));


                        // setting clickable action of notification

                        Intent intent = new Intent(NotificationViewService.this, RobotLink.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("EXTRA_ROBOT_ID", bot.getId());
                        notif.setContentIntent(PendingIntent.getActivity(NotificationViewService.this,
                                id, intent,
                                PendingIntent.FLAG_UPDATE_CURRENT));


                        // Adding dismiss button
                        /*
                        Intent dismissIntent = new Intent();
                        dismissIntent.setAction(DISMISS);
                        notif.addAction(R.drawable.dismiss, "Dismiss", PendingIntent.getBroadcast(NotificationViewService.this,
                                0, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT));
                        *///notifManager.notify(id/*change*/, notif.build());


                        closestThree.add(notif.build());
                        id++;
                        if (id == 3) {
                            break;
                        }
                    }

                    // notify
                    for (int i = (closestThree.size()-1); i > -1; i--) {
                        notifManager.notify(i, closestThree.remove(i));
                    }
                    // *** end displaying push notifications ***

                    // ***displaying emergency dialog (pull notifications)***
                    for (final Robot bot : model) {

                        try {

                            JSONArray progression = bot.getProgression();
                            // make sure this bot has a progression
                            if (progression.length() > 0) {

                                // get the last dialog in the progression
                                final JSONObject lastProgressionElement = progression.getJSONObject(progression.length() - 1);

                                // check it's popup field
                                if (lastProgressionElement.getBoolean("popup")
                                        || progression.length() == 1) {
                                    // if here then make this progression element an emergency dialog
                                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (dialog == null || !dialog.isShowing()) {
                                                //displayDialog(bot, lastProgressionElement);
                                            }
                                        }
                                    });
                                    break; // just show one emergency dialog at a time
                                }
                            }

                        } catch (JSONException ex) {
                            StringWriter stringWriter = new StringWriter();
                            PrintWriter printWriter = new PrintWriter(stringWriter, true);
                            ex.printStackTrace(printWriter);
                            Log.e("NotifView.Update", stringWriter.toString());
                        }
                    }
                    // *** end displaying emergency dialog ***
                }

                try {
                    sleep(450);
                } catch (InterruptedException ex) {

                }
            }
        }
        public void close() {
            keepAlive = false;
        }
    }

    public void displayDialog(final Robot bot, final JSONObject progressionElement) {
        // creating dialog
        dialog = new Dialog(NotificationViewService.this);
        // allowing to write over an screen
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        // setting title of dialog
        dialog.setTitle(bot.getName() + " alert message.");

        // setting layout for dialog
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);

        try {
            // getting content of progression element
            TextView content = new TextView(this);
            content.setText(progressionElement.getString("content"));
            content.setTextSize(18);
            content.setTextColor(Color.WHITE);
            // layout rules for content view
            LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            contentParams.setMargins(20, 40, 0, 30);
            content.setLayoutParams(contentParams);
            dialogLayout.addView(content);

            /**
             * loop through all responses and make buttons for them
             */
            LinearLayout buttonLayout = new LinearLayout(this); // layout that holds buttons
            buttonLayout.setOrientation(LinearLayout.VERTICAL);
            // parameters for button container
            LinearLayout.LayoutParams buttonLayoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            buttonLayoutParams.setMargins(20, 30, 0, 20);
            buttonLayout.setLayoutParams(buttonLayoutParams);
            JSONArray responses = progressionElement.getJSONArray("responses");
            for (int i = 0; i < responses.length(); i++) {
                // get this element in the response
                final JSONObject responseElement = responses.getJSONObject(i);

                //parameters for button view
                LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                buttonParams.setMargins(0, 10, 0, 10);
                Button responseButton = new Button(this);
                responseButton.setTransformationMethod(null); // remove all caps
                responseButton.setText(responseElement.getString("value")); // value of response
                responseButton.setTextSize(20);
                responseButton.setTextColor(Color.BLACK);
                responseButton.setBackgroundColor(Color.WHITE);
                responseButton.setLayoutParams(buttonParams);
                responseButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            ControllerService.addToReplyQueue(bot.getId(),
                                    progressionElement.getString("msgid"), responseElement.getString("id"));

                            //end this dialog
                            dialog.cancel();
                        } catch (JSONException ex) {
                            StringWriter stringWriter = new StringWriter();
                            PrintWriter printWriter = new PrintWriter(stringWriter, true);
                            ex.printStackTrace(printWriter);
                            Log.e("RobotLink.Progression", stringWriter.toString());
                        }
                    }
                });
                buttonLayout.addView(responseButton);
            }
            // adding buttons to dialog
            dialogLayout.addView(buttonLayout);

            // add content to dialog
            dialog.setContentView(dialogLayout);

            // set overall dialog size
            Window window = dialog.getWindow();
            window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);

            //display dialog
            dialog.show();

        } catch (JSONException ex) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter, true);
            ex.printStackTrace(printWriter);
            Log.e("NotifView.Dialog", stringWriter.toString());
        }
    }
}

