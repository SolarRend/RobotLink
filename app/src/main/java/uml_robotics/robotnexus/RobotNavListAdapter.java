package uml_robotics.robotnexus;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * custom adapter for robot selector and navigation drawer lists
 */

public class RobotNavListAdapter extends ArrayAdapter<Robot> {
    private final Activity Context;
    private final Robot[] robots;
    private final Integer[] images;

    public RobotNavListAdapter(Activity context, Robot[] content, Integer[] images) {
        super(context, R.layout.robot_list_resource, content);

        this.Context = context;
        this.robots = content;
        this.images = images;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = Context.getLayoutInflater();
        View ListViewSingle = inflater.inflate(R.layout.robot_list_resource, null, true);

        TextView ListViewItems = (TextView) ListViewSingle.findViewById(R.id.robot_list_text);
        ImageView ListViewImage = (ImageView) ListViewSingle.findViewById(R.id.robot_list_image);

        ListViewItems.setText(robots[position].toString());
        //safety-net for when our first update is an "ack"
        if (images[position] != null) {
            ListViewImage.setImageResource(images[position]);
        }
        return ListViewSingle;
    }
}