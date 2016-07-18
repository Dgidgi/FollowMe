package com.dgidgi.followme;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by gpr on 08/06/2016.
 */
public class LoggedUsersListAdapter extends RecyclerView.Adapter<LoggedUsersListAdapter.ViewHolder>  {

    private static final String LOGTAG ="LoggedUsersListAdapter" ;



    // Interface de filtrage d'affichage des users dans la liste
    public interface IDisplayUserFilter {

        boolean showUser( LoggedUser user) ;
    } ;

    private Collection<LoggedUser> mUsersList ;

    private IDisplayUserFilter mUsersDisplayFilter = null ;

    public void setDisplayUserFilter( IDisplayUserFilter filter ) {
        mUsersDisplayFilter = filter ;
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {

        public static LoggedUsersListListener listener ;

        // each data item is just a string in this case
        public TextView name, distance;
        public LoggedUser user ;

        public static void setListener( LoggedUsersListListener l) {
            listener = l ;
        }

        public static LoggedUser mFollowingUser = null ;

        public static void setFollowingUser(LoggedUser user ) {
            mFollowingUser = user ;
        }

        public ViewHolder(View v) {
            super(v);

            name     = (TextView) v.findViewById(R.id.name);
            distance = (TextView) v.findViewById(R.id.distance);


            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if ( listener != null ) listener.onUserClick(user);
                }
            });
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public LoggedUsersListAdapter(Collection<LoggedUser> users, IDisplayUserFilter filter) {
        mUsersList = users;
        mUsersDisplayFilter = filter ;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public LoggedUsersListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.tracked_user_row, parent, false);

        return  new ViewHolder(v) ;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        try {

            int iNumLoggedUsers = -1 ;
            LoggedUser theLoggedUser = null;

            for (LoggedUser usr:mUsersList ) {
                if ( mUsersDisplayFilter == null || mUsersDisplayFilter.showUser(usr))
                    iNumLoggedUsers++ ;
                if (iNumLoggedUsers == position) {
                    holder.name.setText( usr.getUserName() );
                    holder.user = usr ;

                    if (ViewHolder.mFollowingUser != null && ViewHolder.mFollowingUser.getApplicationId().contains(usr.getApplicationId()) ) {
                        holder.name.setTypeface(null, Typeface.BOLD|Typeface.ITALIC);
                    }
                    else {
                        holder.name.setTypeface(null, Typeface.NORMAL);
                    }

                    return ;
                }
            }
        } catch (Exception e) {

            e.printStackTrace();
            holder.name.setText("" );
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {

        int iNbLoggedUsers = 0 ;

        for (LoggedUser usr:mUsersList ) {
            if ( mUsersDisplayFilter == null || mUsersDisplayFilter.showUser(usr))
                iNbLoggedUsers++ ;
        }

        return iNbLoggedUsers;
    }

}
