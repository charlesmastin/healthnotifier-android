package com.healthnotifier.healthnotifier.fragment;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.widget.DatePicker;

import com.squareup.otto.Bus;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.utility.GenericEvent;
import com.healthnotifier.healthnotifier.utility.Logcat;

/**
 * Created by charles on 4/18/16.
 */
public class CredentialsStatePickerFragment extends DialogFragment {

    private String mInitialState = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // TODO: set min and max dates
        if(getArguments() != null){
            String ts = getArguments().getString("INITIAL_STATE");
            if(ts != null){
                mInitialState = ts;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Select Licensing State")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .setItems(R.array.usStates, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String[] tA = getResources().getStringArray(R.array.usStates);
                        Bus bus = HealthNotifierApplication.bus;
                        Map<String, Object> attributes = new HashMap<String, Object>();
                        attributes.put("State", tA[which]);
                        bus.post(new GenericEvent("onCredentialsStateSet", attributes));
                    }});
        /*
        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // BUS YOUR NUTS OFF
                    }
                })
         */
        if(mInitialState != null){
            // TODO: change over to setSingleChoiceItems and balls, check with base design patterns.
        }

        return builder.create();

    }

}