package com.dgidgi.followme;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Map;

/**
 * Created by gpr on 08/06/2016.
 */
public class TrackedUsersListAdapter  extends RecyclerView.Adapter<TrackedUsersListAdapter.ViewHolder>  {


    Map<String, TrackedUserStatus> mTrackedUsersDataset;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView name, distance;

        public ViewHolder(View v) {
            super(v);


            name = (TextView) v.findViewById(R.id.name);
            distance = (TextView) v.findViewById(R.id.distance);

        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public TrackedUsersListAdapter(Map<String, TrackedUserStatus> mMapTrackedUsers) {
        mTrackedUsersDataset = mMapTrackedUsers;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public TrackedUsersListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.tracked_user_row, parent, false);

        return  new ViewHolder(v) ;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        try {

            TrackedUserStatus tus = (TrackedUserStatus) mTrackedUsersDataset.values().toArray()[position] ;

            String st = "OTHER #"+position ;
            if (tus.isMyStatus()) st = "ME" ;

           // st += " ("+tus.getLocation().longitude+","+tus.getLocation().longitude+")";

            holder.name.setText(st );
            if ( tus.getDistance() > 0.0)
                holder.distance.setText( ""+ tus.getDistance() + "") ;

        } catch (Exception e) {

            e.printStackTrace();
            holder.name.setText("" );

        }


        // Recherche du niem



    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return  mTrackedUsersDataset.size();
    }

}
