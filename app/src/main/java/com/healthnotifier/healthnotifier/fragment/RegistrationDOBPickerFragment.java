package com.healthnotifier.healthnotifier.fragment;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;

import com.squareup.otto.Bus;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.utility.GenericEvent;
import com.healthnotifier.healthnotifier.utility.Logcat;

/**
 * Created by charles on 4/18/16.
 */
public class RegistrationDOBPickerFragment extends DialogFragment
        implements DatePickerDialog.OnDateSetListener {

    private Date mSelectedDate;
    private Date mInitialDate = null;
    public String mAttribute;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // TODO: check to see if we have been passed some extra extra up in this bizzle sizzle
        if(getArguments() != null){
            String dString = getArguments().getString("INITIAL_DATE");
            if(dString != null){
                SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
                try {
                    Date date = formatter.parse(dString);
                    return new DatePickerDialog(getActivity(), this, date.getYear()+1900, date.getMonth(), date.getDate());
                } catch (ParseException e){

                }
            }
        }
        if(mAttribute != null && mAttribute.equals("birthdate")) {
            return new DatePickerDialog(getActivity(), this, 1980, 1, 1);
        }
        // NAIVE TOWN USA
        // TODO: improve this
        return new DatePickerDialog(getActivity(), this, 2017, 0, 1);
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        // WTF JAVA, seriously
        // https://docs.oracle.com/javase/7/docs/api/java/util/Date.html
        mSelectedDate = new Date(year-1900, month, day);
    }

    @Override
    public void onDismiss(DialogInterface dialog){
        if(mSelectedDate != null) {
            // TODO: refactor to use the "standard" ModelFieldChange event
            Bus bus = HealthNotifierApplication.bus;
            Map<String, Object> attributes = new HashMap<String, Object>();
            attributes.put("Date", mSelectedDate);
            attributes.put("Attribute", mAttribute);
            bus.post(new GenericEvent("onDOBSet", attributes));
        }
        super.onDismiss(dialog);
    }
}