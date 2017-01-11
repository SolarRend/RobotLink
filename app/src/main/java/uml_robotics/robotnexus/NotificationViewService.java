package uml_robotics.robotnexus;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.ImageView;

import java.util.ArrayList;

public class NotificationViewService extends Service {
    private NotificationManager notifManager; // manager object for notification related events
    private ArrayList<Robot> model; // view's copy of the model
    private ModelUpdate modelUpdate; // responsible for keeping view's model up to date

    public NotificationViewService() {}

    @Override
    public void onCreate() {
        Log.i("NotifView.onCreate()", "Service Created");
        notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
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

                    for (Robot bot : model) {
                        // building notification
                        // builder object
                        NotificationCompat.Builder notif = new NotificationCompat.Builder(NotificationViewService.this);

                        // setting icon of notification
                        notif.setSmallIcon(bot.getImage());

                        // setting title of notification
                        notif.setContentTitle("A robot has been found.");

                        // setting textual content of notification
                        notif.setContentText(bot.getName() + " wants to talk.");

                        // Making notification non removable by swiping or clearing
                        notif.setOngoing(true);

                        // Big style notification
                        notif.setStyle((new NotificationCompat.BigTextStyle()).bigText(
                                bot.getName() + " wants to talk!"
                        ));


                        // setting clickable action of notification
                        Intent intent = new Intent(NotificationViewService.this, RobotLink.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("EXTRA_ROBOT_ID", bot.getId());
                        notif.setContentIntent(PendingIntent.getActivity(NotificationViewService.this,
                                0, intent,
                                PendingIntent.FLAG_UPDATE_CURRENT));

                        // Adding dismiss button
                        /*
                        Intent dismissIntent = new Intent();
                        dismissIntent.setAction(DISMISS);
                        notif.addAction(R.drawable.dismiss, "Dismiss", PendingIntent.getBroadcast(NotificationViewService.this,
                                0, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT));
                        */
                        notifManager.notify(1, notif.build());
                    }
                }

                try {
                    sleep(500);
                } catch (InterruptedException ex) {

                }
            }
        }
        public void close() {
            keepAlive = false;
        }
    }
}
