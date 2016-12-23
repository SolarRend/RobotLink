package uml_robotics.robotnexus;

import android.graphics.Bitmap;

/**
 * Robot object used by the model and controller
 */

public class Robot {
    private String name; // name of robot
    private Bitmap image; //image a robot may want to transfer
    private boolean dismissed = false; // for dismissed robots
    private int proximity; // how close is this robot

    public Robot(String name) {
        this.name = name;
    }
}
