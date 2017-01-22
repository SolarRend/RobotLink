package uml_robotics.robotnexus;

import android.graphics.Bitmap;

import org.json.JSONArray;

/**
 * Robot object used by the model and controller
 */


public class Robot {
    private enum State {ok, safe, help, dangerous, off, NOT_SET} // a robot is in one of these states at all times
    private String name; // name of robot
    private Integer image; // image id of what robot looks like
    private boolean visibile = true; // for dismissed robots
    private int proximity; // how close is this robot
    private String id; // hidden identifier for a bot
    private String model; // the make of a robot
    private State currState;
    private JSONArray progression = new JSONArray(); // most recent progression for this robot


    public Robot(int rssi, String id) {
        this.proximity = rssi;
        this.id = id;
        this.currState = State.NOT_SET;
    }

    public void setVisibile(boolean visibile) {
        this.visibile = visibile;
    }

    public boolean isVisibile() {
        return visibile;
    }

    public void setProgression(JSONArray progression) {
        this.progression = progression;
    }

    public JSONArray getProgression() {
        return progression;
    }

    public void setProximity(int proximity) {
        this.proximity = proximity;
    }

    public int getProximity() {
        return proximity;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getModel() {
        return model;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setCurrState(String currState) {
        this.currState = State.valueOf(currState);
    }

    public String getCurrState() {
        return currState.toString();
    }

    public void setImage(Integer image) {
        this.image = image;
    }

    public Integer getImage() {
        return image;
    }

    public String getId() {
        return id;
    }

    // how to compare robots to each other
    @Override
    public boolean equals(Object robot) {
        Robot robotPrime = (Robot)robot;
        Robot robotSelf = this;

        if (!(robotSelf.getId().equals(robotPrime.getId()))) {
            return false;
        }
        if (robotSelf.getProximity() != robotPrime.getProximity()) {
            return false;
        }
        if (!(robotSelf.getCurrState().equals(robotPrime.getCurrState()))) {
            return false;
        }

        return true;
    }

    // returns contents of a complete robot
    @Override
    public Object clone() {
        Robot robot = new Robot(this.getProximity(), this.getId());
        robot.setName(this.getName());
        robot.setCurrState(this.getCurrState());
        robot.setModel(this.getModel());
        robot.setImage(this.getImage());
        robot.setProgression(this.getProgression());
        robot.setVisibile(this.isVisibile());
        return robot;
    }

    @Override
    public String toString() {
        return (name == null? "Null" : name) /*+ " - " + model +
                "\nStatus: " + currState.toString() + "\nProximity: " +
                proximity + "\nID: " + id*/;
     }
}
