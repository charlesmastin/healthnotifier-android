package com.healthnotifier.healthnotifier.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.healthnotifier.healthnotifier.R;

/**
 * Created by charles on 2/3/17.
 */

public class InboxFragment extends Fragment {

    private View mRootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_inbox, container, false);

        // TODO: reformat the data, grouped by patient
        // TODO: then a totally dyanmic UI going in here
        // with whatever separater and then the listview placeholder for our PatientNetworkInvitesAdapter

        return mRootView;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}
