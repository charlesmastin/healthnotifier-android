package com.healthnotifier.healthnotifier.fragment;

import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.R;

import com.healthnotifier.healthnotifier.adapter.PatientsCardAdapter;
import com.healthnotifier.healthnotifier.utility.Logcat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

// Heavily inspired by
// http://www.androidhive.info/2016/05/android-working-with-card-view-and-recycler-view/

public class PatientsFragment extends Fragment {

    private View mRootView;
    private SwipeRefreshLayout swipeContainer;
    private RecyclerView recyclerView;
    private PatientsCardAdapter mAdapter;
    private JSONArray mPatients = new JSONArray();
    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler(Looper.getMainLooper());
        setHasOptionsMenu(true); // naa son
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_patients, container, false);
        return mRootView;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAdapter = new PatientsCardAdapter(getActivity(), mPatients);

        recyclerView = (RecyclerView) mRootView.findViewById(R.id.recycler_view);
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(getActivity(), 2);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(2, dpToPx(5), true));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        swipeContainer = (SwipeRefreshLayout) mRootView.findViewById(R.id.swipeContainer);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadData(false);
            }
        });

        loadData(true);
    }

    private int dpToPx(int dp) {
        Resources r = getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }

    private void loadData(Boolean showLoader) {
        // THIS IS COMEDY TIMES
        HealthNotifierAPI.getInstance().getPatients(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // we couldn't get a response or we timed out, offline, etc
                Logcat.d( "onFailure" + e.toString());
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                        swipeContainer.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                if (response.isSuccessful()) { // exactly what status codes determine this?
                    try {
                        JSONObject reader = new JSONObject(response.body().string());
                        JSONArray items = reader.getJSONArray("Patients");

                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mPatients = items;
                                mAdapter.mPatientsList = items; // PUBLIC TO THE FACE SON
                                // MAY NO LONGER BE NECESSARY SON
                                mAdapter.notifyDataSetChanged();

                                mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                                swipeContainer.setRefreshing(false);
                            }
                        });
                    } catch (Exception e) {
                        //
                    }
                } else {
                    Logcat.d( "onResponseError" + response.toString());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                            swipeContainer.setRefreshing(false);
                        }
                    });
                }
            }
        });

        if (showLoader)
            mRootView.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
    }

    public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

        private int spanCount;
        private int spacing;
        private boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view); // item position
            int column = position % spanCount; // item column

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount; // spacing - column * ((1f / spanCount) * spacing)
                outRect.right = (column + 1) * spacing / spanCount; // (column + 1) * ((1f / spanCount) * spacing)

                if (position < spanCount) { // top edge
                    outRect.top = spacing;
                }
                outRect.bottom = spacing; // item bottom
            } else {
                outRect.left = column * spacing / spanCount; // column * ((1f / spanCount) * spacing)
                outRect.right = spacing - (column + 1) * spacing / spanCount; // spacing - (column + 1) * ((1f /    spanCount) * spacing)
                if (position >= spanCount) {
                    outRect.top = spacing; // item top
                }
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_patients, menu);
        // mMenu = menu; I guess this is so we can config it later???
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void createProfile(){
        mRootView.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        HealthNotifierAPI.getInstance().createProfile(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // onlock da UI son
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), "Error creating profile!", Toast.LENGTH_SHORT).show();
                        mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // check if success
                // for now, we don't even care about the response body son , because we're just reloading this here view
                if(response.isSuccessful()){
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), "New profile created!", Toast.LENGTH_SHORT).show();
                            loadData(true);
                        }
                    });
                } else {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), "Error creating profile!", Toast.LENGTH_SHORT).show();
                            mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
        // show dat spinner
        // lock dat UI
        // call the networking
        // on success, unlock , hide and balls, reload da patients son
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // remember this is not search submit, this is launch the search activity
            case R.id.action_create_profile:
                createProfile();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
