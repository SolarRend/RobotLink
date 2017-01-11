package uml_robotics.robotnexus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * responsible for restarting the controller with the last instance of the The Model
 */

public class ServiceReanimate extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        // controller is only broadcasting to this receiver
        Intent serviceIntent = new Intent(context, ControllerService.class);
        serviceIntent.setAction("uml_robotics.reanimate.controller");
        serviceIntent.putStringArrayListExtra("EXTRA_MODEL", intent.getStringArrayListExtra("EXTRA_MODEL"));
        context.startService(serviceIntent);
    }
}
