package com.dgidgi.followme;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import org.json.JSONException;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FollowMeTrackingFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FollowMeTrackingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FollowMeTrackingFragment extends Fragment implements OnMapReadyCallback, LoggedUsersListAdapter.IDisplayUserFilter {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private static final String LOGTAG = "FMTFragment";
    private static final int SEND_PERIOD = 1000;

    FollowMeMap     mFollowMeMap ;
    RecyclerView mTrackedUsersListView;


    LoggedUsersListAdapter  mLoggedUserListAdapter ;
    CheckBox mAUtoCenter ;
    Button mSwitchTrackingButton ;

    TextView    mTextMessage ;
    Button      mButtonSendMessage ;

    private OnFragmentInteractionListener mListener;


    private LoggedUser mFollowingUser = null ;


    public FollowMeTrackingFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FollowMeTrackingFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static FollowMeTrackingFragment newInstance(String param1, String param2) {
        FollowMeTrackingFragment fragment = new FollowMeTrackingFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view =  inflater.inflate(R.layout.fragment_follow_me_tracking, container, false);

        // Initialisation de la listes des tracked Users
        mTrackedUsersListView = (RecyclerView) view.findViewById(R.id.tracked_users_list);
        mTrackedUsersListView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this.getContext());
        mTrackedUsersListView.setLayoutManager(mLayoutManager);

        mLoggedUserListAdapter = new LoggedUsersListAdapter( FollowMeMainActivity.getLoggedUsersMap().values(), this);
        mTrackedUsersListView.setAdapter(mLoggedUserListAdapter);

        LoggedUsersListAdapter.ViewHolder.setListener(new LoggedUsersListListener() {
            @Override
            public void onUserClick(LoggedUser user) {

                if ( mFollowingUser != null && mFollowingUser.getApplicationId() == user.getApplicationId()) {
                    mFollowingUser = null ;
                }
                else {
                    mFollowingUser = user;
                }

                if(mFollowingUser != null )
                    mFollowMeMap.centerTo(mFollowingUser.getApplicationId());

                LoggedUsersListAdapter.ViewHolder.setFollowingUser(mFollowingUser);
                mLoggedUserListAdapter.notifyDataSetChanged();
            }
        });

        // Initialisation de la Map
        SupportMapFragment mapFragment = (SupportMapFragment) this.getChildFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        // Récupération du checkbox d'auto center
         mAUtoCenter = (CheckBox) view.findViewById(R.id.cbAutoCenter);


        mAUtoCenter.setOnClickListener( new  View.OnClickListener()
        {
            @Override
            public void onClick(View v)  {
              onToggleAutoCenter(v);
            }
        }
        );


        mSwitchTrackingButton = (Button) view.findViewById(R.id.switchTrackingButton) ;

        mSwitchTrackingButton.setOnClickListener( new  View.OnClickListener()
            {
                @Override
                public void onClick(View v)  {
                    onClickSwitchTrackingButton(v);
                }
            }
        );

        mSwitchTrackingButton.setVisibility(View.GONE);


        mTextMessage  = (TextView) view.findViewById(R.id.txtMessage) ;

        mTextMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mButtonSendMessage.setEnabled(s.length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mButtonSendMessage = (Button) view.findViewById(R.id.btSendMessageToRunner)  ;

        mButtonSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mFollowingUser != null)
                    mListener.sendMessageToRunner(mFollowingUser, mTextMessage.getText().toString());

            }
        });


        // Inflate the layout for this fragment
        return view ;
    }

    //
    // Mise à jour de la liste des utilisateurs connectés
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void loggedUsersListChanged() {

        mLoggedUserListAdapter.notifyDataSetChanged();
    }

    //
    // Mise à jour de la localisation sur la carte
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void  updateUserLocation( LoggedUser loggedUser, UserPosition userPosition ) {

        boolean bAutoCenter = false ;

        // Si ça corresponds à l'utilisateur en cours de suivi, on auto centre
        if ( mFollowingUser != null && loggedUser.getApplicationId() == this.mFollowingUser.getApplicationId()) {
            bAutoCenter = true ;
        }
        if ( showUser( loggedUser ) )
            this.mFollowMeMap.updateUserLocationMarker( loggedUser.getApplicationId(),
                                                        loggedUser.getUserName(),
                                                        loggedUser.getUserKindOf(),
                                                        userPosition.getLatitude(),
                                                        userPosition.getLongitude(),
                                                        loggedUser.itsMe(), bAutoCenter);
    }

    //
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void enableTracking( boolean b){
        mSwitchTrackingButton.setEnabled(b);
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mFollowMeMap = new FollowMeMap(googleMap)  ;
    }

    // Mise à jour de la localisation courante de l'utilisateur
    public void currentUserLocationChanged(Location mLastLocation) {

        LoggedUser me = FollowMeMainActivity.getCurrentUser() ;

        if (me == null )
            return ;

        mFollowMeMap.updateUserLocationMarker(
                me.getApplicationId(),
                me.getUserName(),
                me.getUserKindOf(),
                mLastLocation.getLatitude(),
                mLastLocation.getLongitude(),
                true,
                isAutoCenterCamera()
        );
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentStartTracking();
        void onFragmentStopTracking();
        void sendMessageToRunner( LoggedUser followingUser, String sMessage);
    }

    //
    //Toggle AutoCenter
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void onToggleAutoCenter(View view) {

        if (isAutoCenterCamera() ) {

            LoggedUser usr = FollowMeMainActivity.getCurrentUser() ;
            if ( usr != null)
                mFollowMeMap.centerTo(usr.getApplicationId());
        }

    }

    //
    // Retourne true si on est en mode autocenter
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private boolean isAutoCenterCamera(){
        return mAUtoCenter.isChecked() ;
    }

    //
    // Toggle Tracking
    ///////////////////////////////////////////////////////////////////////////////////////////////
    public void onClickSwitchTrackingButton(View view) {

        ToggleButton toggleButton = (ToggleButton) view.findViewById(R.id.switchTrackingButton);

        if (toggleButton.isChecked()) {

            mListener.onFragmentStartTracking();

        } else {

            mListener.onFragmentStopTracking();

        }
    }

    //
    // Returbn true si l'utilisateur doit être visible
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public boolean showUser(LoggedUser usr) {

        LoggedUser me = FollowMeMainActivity.getCurrentUser() ;

        if ( me == null)
            return false ;

        return (usr.getUserKindOf() != me.getUserKindOf() && !usr.itsMe()) ;

    }

}
