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
    private String id;

    public Robot(String name, int rssi, String id) {
        this.name = name;
        this.proximity = rssi;
        this.id = id;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public Bitmap getImage() {
        return image;
    }

    @Override
    public String toString() {
        return "Hello, I am " + (name == null? "Null" : name);
     }
}
