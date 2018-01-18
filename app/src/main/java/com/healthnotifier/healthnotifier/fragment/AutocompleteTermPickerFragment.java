package com.healthnotifier.healthnotifier.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.adapter.AutocompleteTermsAdapter;
import com.healthnotifier.healthnotifier.utility.JSONHelper;
import com.healthnotifier.healthnotifier.utility.LayoutHelper;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by charles on 2/5/17.
 */

public class AutocompleteTermPickerFragment extends DialogFragment {

    public String mAutocompleteId;
    public String mPatientId;
    public String mInitialValue = null;
    private View mRootView;
    private Handler mHandler;

    private ArrayList<JSONObject> mResults = new ArrayList<JSONObject>();
    private AutocompleteTermsAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mHandler = new Handler(Looper.getMainLooper());
        return inflater.inflate(R.layout.fragment_autocomplete_term_picker, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRootView = view;

        mAdapter = new AutocompleteTermsAdapter(getContext(), mResults, mAutocompleteId);
        ListView listView = (ListView) mRootView.findViewById(R.id.lvTermResults);
        listView.setAdapter(mAdapter);

        // Get field from view
        EditText et = (EditText) mRootView.findViewById(R.id.etSearch);
        // Fetch arguments from bundle and set title
        getDialog().setTitle(mAutocompleteId);
        // Show soft keyboard automatically and request focus to field
        et.requestFocus();
        getDialog().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    hideKeyboard();
                    searchTerms(v.getText().toString());
                }
                return handled;
            }
        });
    }

    // TODO: put this in da static forms library son
    private void hideKeyboard() {
        InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private void updateItems(){
        // slap the adapter some
        mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);

        mAdapter.replaceItems(mResults);
        mAdapter.notifyDataSetChanged();
        ListView listView = (ListView) mRootView.findViewById(R.id.lvTermResults);
        LayoutHelper.setListViewHeightBasedOnChildren(listView);

    }

    private void searchTerms(String keywords){
        mRootView.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);

        HealthNotifierAPI.getInstance().termsSearch(mAutocompleteId, keywords, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    mResults = JSONHelper.jsonArrayToArrayList(json.getJSONArray("combinations"));
                } catch(Exception e){

                }
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        updateItems();
                    }
                });
            }
        });
    }

    // override the constructor to then kick off and load the data son buns
    // build the adpator up in this bitch

    // auto focus the input upon load


}
