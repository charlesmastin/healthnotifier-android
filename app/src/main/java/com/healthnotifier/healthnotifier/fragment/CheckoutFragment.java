package com.healthnotifier.healthnotifier.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.healthnotifier.healthnotifier.network.HealthNotifierAPI;
import com.healthnotifier.healthnotifier.HealthNotifierApplication;
import com.healthnotifier.healthnotifier.Config;
import com.healthnotifier.healthnotifier.R;
import com.healthnotifier.healthnotifier.activity.CaptureLifesquareActivity;
import com.healthnotifier.healthnotifier.activity.PatientActivity;
import com.healthnotifier.healthnotifier.form.FieldValidation;
import com.healthnotifier.healthnotifier.form.FormValidation;
import com.healthnotifier.healthnotifier.form.Forms;
import com.healthnotifier.healthnotifier.adapter.LinkedHashMapAdapter;
import com.healthnotifier.healthnotifier.utility.Formatters;
import com.healthnotifier.healthnotifier.utility.LayoutHelper;
import com.healthnotifier.healthnotifier.utility.Logcat;

import com.healthnotifier.healthnotifier.utility.Validators;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;

/**
 * Created by charles on 1/31/17.
 */

// TODO: implement more than "assign"

public class CheckoutFragment extends Fragment {
    private View mRootView;
    private Menu mMenu;
    private Handler mHandler;
    private String mPatientRawJson;
    private JSONObject mPatientJson;
    private JSONObject mValidationResults = null;

    private String mMode = "assign";
    private String mAssignMethod = null;

    private Integer mShippingId = null;
    private Boolean mShippingRequired = true;

    private String mCardId = null; // existing card
    private String mTokenId = null; // on-demand gen card
    private Integer mAmountDue = 0; //total due in cents init later from meta

    // f yea son
    private String mLifesquareCode = null;
    private Boolean mLifesquareValid = false;
    private Boolean mLifesquareHasFocus = false;
    private Boolean mPromoValid = false;
    private Boolean mPromoHasFocus = false; // meh

    private Boolean mLocked = false;

    private DisplayImageOptions mDisplayImageOptions;
    private ImageLoader mImageLoader = ImageLoader.getInstance();

    private LinkedHashMapAdapter<String, String> mPaymentMethodAdaptor; // class variable because we can add cards or methods lol


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true); // naa son
        setRetainInstance(true);

        mHandler = new Handler(Looper.getMainLooper());
        // keep it honest, only one at a time in mobile for this release
        mPatientRawJson = getActivity().getIntent().getStringExtra("PATIENT_JSON");
        mMode = getActivity().getIntent().getStringExtra("MODE"); // assign, renew, replace
        try {
            mPatientJson = new JSONObject(mPatientRawJson);
        } catch (Exception e) {
            mPatientJson = null;
            // YOU'RE F'D ANYHOW, might as well crash the app now!
        }

        mDisplayImageOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .displayer(new SimpleBitmapDisplayer())
                .build();

        // init amount due from meta here, ish
    }

    Runnable delayedValiation = new Runnable() {
        @Override
        public void run() {
            remoteValidate();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_checkout, container, false);
        LayoutHelper.initActionBar(getActivity(), mRootView, "Checkout");
        return mRootView;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // TODO: events
        if(mMode.equals("assign")) {
            // radio toggles son
            ((RadioButton) mRootView.findViewById(R.id.rbCheckoutProvision)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mAssignMethod = "new";
                    doubleSecretInit();
                    remoteValidate();
                };
            });
            ((RadioButton) mRootView.findViewById(R.id.rbCheckoutClaim)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mAssignMethod = "claim";
                    doubleSecretInit();

                    Intent intent = new Intent(getContext(), CaptureLifesquareActivity.class);
                    intent.putExtra("REQUESTOR", "checkout");
                    startActivity(intent);

                };
            });

            // promo code change handler son
            EditText promoCode = (EditText) mRootView.findViewById(R.id.etCheckoutPromoCode);
            promoCode.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    mPromoHasFocus = hasFocus;
                    if(!mPromoHasFocus){
                        // if we were being optimal, we would just call double secret init, since the last status
                        doubleSecretInit();
                    }
                }
            });

            // promo code change handler son
            promoCode.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    mHandler.removeCallbacks(delayedValiation);
                    mHandler.postDelayed(delayedValiation, 500);
                }
            });


            // provisionally handle the enterprise email clicker
            ((TextView) mRootView.findViewById(R.id.tvEnterpriseMeta)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String url = "mailto:enterprise@domain.com";
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:"));
                    String email = url.replace("mailto:", "");
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Enterprise Connect");
                    if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                        startActivity(intent);
                    }
                };
            });

        }

        // TODO: load all the addresses for all da profiles
        // TODO: wire change handler on address and credit card to set relevant state variables

        try {
            JSONArray addresses = mPatientJson.getJSONArray("addresses");
            if(addresses.length() > 1){
                LinkedHashMap<String, String> addressHash = new LinkedHashMap<String, String>();
                for (int i = 0; i < addresses.length(); i++) {
                    JSONObject tO = addresses.getJSONObject(i);
                    addressHash.put(Integer.toString(tO.getInt("patient_residence_id")), tO.getString("title"));
                }
                Spinner spinnerAddress = (Spinner) mRootView.findViewById(R.id.spCheckoutAddress);
                LinkedHashMapAdapter<String, String> spinnerAdaptor = new LinkedHashMapAdapter<String, String>(getContext(),
                        R.layout.spinner_item, addressHash);
                spinnerAddress.setAdapter(spinnerAdaptor);
                // set selection on first - aka, mailing address blbobobobob
                mShippingId = addresses.getJSONObject(0).getInt("patient_residence_id");



                // hide the summary
                ((TextView) mRootView.findViewById(R.id.tvCheckoutShippingSummary)).setVisibility(View.GONE);
            } else {
                if(addresses.length() == 1){
                    mShippingId = addresses.getJSONObject(0).getInt("patient_residence_id");
                    // HIDE THE SELECT
                    ((Spinner) mRootView.findViewById(R.id.spCheckoutAddress)).setVisibility(View.GONE);
                    // HIDE THE SELECT LABEL
                    ((TextView) mRootView.findViewById(R.id.tvCheckoutAddress)).setVisibility(View.GONE);
                    // SHOW THE SUMMARY TEXT
                    ((TextView) mRootView.findViewById(R.id.tvCheckoutShippingSummary)).setVisibility(View.VISIBLE);
                    ((TextView) mRootView.findViewById(R.id.tvCheckoutShippingSummary)).setText("Your stickers will be sent to " + addresses.getJSONObject(0).getString("title") + ".");
                }
            }


        } catch(Exception e){
            // basically we have nowhere to ship, so if in fact you umm attempt to get LifeStickers, no bueno
        }

        // prefil any pre-saved credit cards
        try {
            LinkedHashMap<String, String> cardsHash = new LinkedHashMap<String, String>();
            JSONObject meta = mPatientJson.getJSONObject("meta");
            cardsHash.put(null, "New Card");
            mAmountDue = meta.getInt("coverage_cost"); // we init that sucker

            JSONArray cards = meta.getJSONArray("available_cards");
            for (int i = 0; i < cards.length(); i++) {
                JSONObject tO = cards.getJSONObject(i);
                String summary = tO.getString("brand");
                summary += " ending in ";
                summary += tO.getString("last4");
                summary += " exp ";
                summary += Integer.toString(tO.getInt("exp_month"));
                summary += "/";
                summary += Integer.toString(tO.getInt("exp_year"));
                cardsHash.put(tO.getString("id"), summary);
            }
            // HANDLE 0 cards
            Spinner spinnerCards = (Spinner) mRootView.findViewById(R.id.spCheckoutCC);
            mPaymentMethodAdaptor = new LinkedHashMapAdapter<String, String>(getContext(),
                    R.layout.spinner_item, cardsHash);
            spinnerCards.setAdapter(mPaymentMethodAdaptor);

            if(cards.length() > 0){
                mCardId = cards.getJSONObject(0).getString("id");
                // set da selection of da spinner though son
                spinnerCards.setSelection(1);// aka the first brizzle
                // wire up da Add Button son
                ((Button) mRootView.findViewById(R.id.btCheckoutAddCard)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((Spinner) mRootView.findViewById(R.id.spCheckoutCC)).setSelection(0);
                    }
                });

                // TODO: change handler on dat card so we can toggle da viz SON
                spinnerCards.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if(position == 0){
                            // new card bro
                            mRootView.findViewById(R.id.llCheckoutPaymentCardForm).setVisibility(View.VISIBLE);
                            ((Button) mRootView.findViewById(R.id.btCheckoutAddCard)).setVisibility(View.GONE);
                            mCardId = null;
                        }else {
                            // existing card
                            mRootView.findViewById(R.id.llCheckoutPaymentCardForm).setVisibility(View.GONE);
                            ((Button) mRootView.findViewById(R.id.btCheckoutAddCard)).setVisibility(View.VISIBLE);
                            mCardId = mPaymentMethodAdaptor.getItem(position).getKey();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        Logcat.d("NOTHING SELECTED CREDIT CARD BLUNDER NUTS");
                    }
                });

                // card FORM hiddne son
                mRootView.findViewById(R.id.llCheckoutPaymentCardForm).setVisibility(View.GONE);


            } else {
                // hide dat add Button brizzle
                ((Button) mRootView.findViewById(R.id.btCheckoutAddCard)).setVisibility(View.GONE);
            }

        } catch(Exception e){
            // NO cards on file son, no meta son
            // might as well hide your nuts
            Logcat.d( e.toString());
        }
        // check again for da length son
        // Hide if 0 cards, blablabla, crash town down
        if(mPaymentMethodAdaptor != null && mPaymentMethodAdaptor.getCount() == 0){
            Logcat.d("HIDE THAT SHIT SO MUCH");
            ((TextView) mRootView.findViewById(R.id.tvCheckoutCC)).setVisibility(View.GONE);
            ((Spinner) mRootView.findViewById(R.id.spCheckoutCC)).setVisibility(View.GONE);
            // consider inline form for credit card son
        }

        // CREDIT CARD FORM INIT SON BUNS
        Spinner spinnerExpMonth = (Spinner) mRootView.findViewById(R.id.spCheckoutFormExpirationMonth);
        Spinner spinnerExpYear = (Spinner) mRootView.findViewById(R.id.spCheckoutFormExpirationYear);

        LinkedHashMap<String, String> valuesMonth = new LinkedHashMap<String, String>();
        LinkedHashMap<String, String> valuesYear = new LinkedHashMap<String, String>();

        // yea with a string format it could be in a loop FML
        valuesMonth.put("1", "01");
        valuesMonth.put("2", "02");
        valuesMonth.put("3", "03");
        valuesMonth.put("4", "04");
        valuesMonth.put("5", "05");
        valuesMonth.put("6", "06");
        valuesMonth.put("7", "07");
        valuesMonth.put("8", "08");
        valuesMonth.put("9", "09");
        valuesMonth.put("10", "10");
        valuesMonth.put("11", "11");
        valuesMonth.put("12", "12");

        // FJAVA APIS FROM HELL
        Date date = new Date();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);

        // TODO: calculate the current month

        for(int i = year; i < year + 30; i++){
            valuesYear.put(Integer.toString(i), Integer.toString(i));
        }

        LinkedHashMapAdapter<String, String> monthAdaptor = new LinkedHashMapAdapter<String, String>(mRootView.getContext(),
                R.layout.spinner_item, valuesMonth);
        spinnerExpMonth.setAdapter(monthAdaptor);
        LinkedHashMapAdapter<String, String> yearAdaptor = new LinkedHashMapAdapter<String, String>(mRootView.getContext(),
                R.layout.spinner_item, valuesYear);
        spinnerExpYear.setAdapter(yearAdaptor);

        // bro, initial state on this seems dangerous here so let's defer son
        doubleSecretInit();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_checkout, menu);
        mMenu = menu;
        // mMenu = menu; I guess this is so we can config it later???
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // TODO: check if we have an in-flight guy
                if(!mLocked) {
                    getActivity().finish();
                }
                return true;
            case R.id.action_checkout:
                onSubmit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onChangePromoCode(){

    }

    private void doubleSecretInit(){
        // only thing we do here is react to differing states
        // so we hide / show things
        // update values, etc, that is all
        // all event binding and adapters are created elseqhere

        if(mMode.equals("assign")){
            RadioButton buttonNew = (RadioButton) mRootView.findViewById(R.id.rbCheckoutProvision);
            RadioButton buttonClaim = (RadioButton) mRootView.findViewById(R.id.rbCheckoutClaim);


            if(mAssignMethod == null){
                // wipe both radio buttons bra, in case we null it out for the user, say, on a failed validation via scan or whatnot
                buttonNew.setChecked(false);
                buttonClaim.setChecked(false);
                ((LinearLayout) mRootView.findViewById(R.id.llCheckoutLifesquareValidated)).setVisibility(View.GONE);

                // hide some cards bro, like shipping, and subscription
                mRootView.findViewById(R.id.cardShipping).setVisibility(View.GONE);
                mRootView.findViewById(R.id.cardSubscription).setVisibility(View.GONE);
                mRootView.findViewById(R.id.cardPayment).setVisibility(View.GONE);
                mRootView.findViewById(R.id.cardPromoCode).setVisibility(View.GONE);
                mRootView.findViewById(R.id.cardPlanLevel).setVisibility(View.GONE);
                mRootView.findViewById(R.id.cardAmountDue).setVisibility(View.GONE);

            } else {
                // general state
                mRootView.findViewById(R.id.cardAmountDue).setVisibility(View.VISIBLE);
                mRootView.findViewById(R.id.cardPromoCode).setVisibility(View.VISIBLE);

                if (mAmountDue > 0) {
                    mRootView.findViewById(R.id.cardPayment).setVisibility(View.VISIBLE);
                    mRootView.findViewById(R.id.cardSubscription).setVisibility(View.VISIBLE);
                    // TODO: and if promo is not entered / valid
                    if(mAssignMethod.equals("new") && !mPromoValid) {
                        mRootView.findViewById(R.id.cardPlanLevel).setVisibility(View.VISIBLE);
                    }
                } else {
                    // handle animation
                    mRootView.findViewById(R.id.cardPayment).setVisibility(View.GONE);
                    mRootView.findViewById(R.id.cardSubscription).setVisibility(View.GONE);
                    mRootView.findViewById(R.id.cardPlanLevel).setVisibility(View.GONE);
                }

                if (mAssignMethod.equals("new")) {

                    buttonNew.setChecked(true);
                    buttonClaim.setChecked(false);
                    ((LinearLayout) mRootView.findViewById(R.id.llCheckoutLifesquareValidated)).setVisibility(View.GONE);
                    // zero out any input lifesquare code, or perhaps just the UI element, LOLZONES
                    mLifesquareCode = null;
                    mLifesquareValid = false;
                    ((TextView) mRootView.findViewById(R.id.tvCheckoutLifesquareValidated)).setText("");
                    ((ImageView) mRootView.findViewById(R.id.ivCheckoutLifesquareValidated)).setImageBitmap(null);

                    // yes on shipping
                    mRootView.findViewById(R.id.cardShipping).setVisibility(View.VISIBLE);

                }
                if (mAssignMethod.equals("claim")) {
                    // show
                    buttonNew.setChecked(false);
                    buttonClaim.setChecked(true);

                    // we will fill with the relevant icon later, and show if not editing, etc

                    if(mLifesquareValid){
                        ((LinearLayout) mRootView.findViewById(R.id.llCheckoutLifesquareValidated)).setVisibility(View.VISIBLE);
                        // text view
                        ((TextView) mRootView.findViewById(R.id.tvCheckoutLifesquareValidated)).setText(mLifesquareCode);
                        // load image, ideally cached though

                        final int size = 96;
                        ImageView lifesquarePhoto = (ImageView) mRootView.findViewById(R.id.ivCheckoutLifesquareValidated);
                        String url = Config.API_ROOT + "lifesquares/" + mLifesquareCode + "/image?width=" + (size*2) + "&height=" + (size*2);
                        mImageLoader.displayImage(url, lifesquarePhoto, mDisplayImageOptions, new SimpleImageLoadingListener() {
                            @Override
                            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                                ((ImageView) view).setImageBitmap(loadedImage);
                                ((ImageView) view).setScaleType(ImageView.ScaleType.CENTER_CROP);
                            }
                        });

                        // naaa on shipping
                        mRootView.findViewById(R.id.cardShipping).setVisibility(View.GONE);
                    } else {
                        ((LinearLayout) mRootView.findViewById(R.id.llCheckoutLifesquareValidated)).setVisibility(View.GONE);
                        mRootView.findViewById(R.id.cardShipping).setVisibility(View.VISIBLE);

                    }

                }
            }
            ImageView promoStatusView = (ImageView) mRootView.findViewById(R.id.ivPromoStatus);
            String promoCode = ((EditText) mRootView.findViewById(R.id.etCheckoutPromoCode)).getText().toString();

            // promo state son
            if(mPromoHasFocus){
                // only turn on if promo valid
                if(mPromoValid){
                    promoStatusView.setVisibility(View.VISIBLE);
                    promoStatusView.setImageResource(R.drawable.ic_check_circle_black_24dp);
                    promoStatusView.setColorFilter(getContext().getResources().getColor(R.color.ColorGreen), PorterDuff.Mode.SRC_ATOP);
                } else {
                    promoStatusView.setVisibility(View.GONE);
                }
                // else show spinner (blabla)
            } else {
                // valid or not
                // but only if there is some content up in there son
                if(!promoCode.equals("")) {
                    promoStatusView.setVisibility(View.VISIBLE);
                    if (mPromoValid) {
                        promoStatusView.setImageResource(R.drawable.ic_check_circle_black_24dp);
                        promoStatusView.setColorFilter(getContext().getResources().getColor(R.color.ColorGreen), PorterDuff.Mode.SRC_ATOP);
                    } else {
                        promoStatusView.setImageResource(R.drawable.ic_report_problem_black_24dp);
                        promoStatusView.setColorFilter(getContext().getResources().getColor(R.color.ColorPrimaryDark), PorterDuff.Mode.SRC_ATOP);
                    }
                } else {
                    promoStatusView.setVisibility(View.GONE);
                }
            }
        }


        // general catch all
        // set amount due, always, keep in mind Total card may be GONE
        ((TextView) mRootView.findViewById(R.id.tvCheckoutTotalDue)).setText(Formatters.centsToDollars(mAmountDue));// TODO: currency formatters son

        if(!mMode.equals("assign")) {
            if (mAmountDue > 0) {
                mRootView.findViewById(R.id.cardPayment).setVisibility(View.VISIBLE);
            } else {
                mRootView.findViewById(R.id.cardPayment).setVisibility(View.GONE);
            }
        }
        String promoCode = ((EditText) mRootView.findViewById(R.id.etCheckoutPromoCode)).getText().toString();
        // basically we never touched it + we don't need to…so we don't need to see it
        if(mAmountDue <= 0 && promoCode.equals("")){
            // TODO: crashing based on strange case with promo change listenered
            mRootView.findViewById(R.id.cardPromoCode).setVisibility(View.GONE);
        }else if (mAssignMethod != null){
            mRootView.findViewById(R.id.cardPromoCode).setVisibility(View.VISIBLE);
        }


    }

    private JSONObject generatePayload() {
        JSONObject json = new JSONObject();
        try {
            JSONArray patients = new JSONArray();
            JSONObject patientNode = new JSONObject();
            patientNode.put("PatientId", mPatientJson.getJSONObject("profile").getString("uuid"));
            if(mMode.equals("assign")) {
                if(mAssignMethod != null) {
                    if (mAssignMethod.equals("new")) {
                        patientNode.put("LifesquareId", JSONObject.NULL);
                    }
                    if (mAssignMethod.equals("claim")) {
                        patientNode.put("LifesquareId", mLifesquareCode);
                    }
                }
            }
            patients.put(patientNode);
            JSONObject paymentNode = new JSONObject();
            paymentNode.put("AuthorizedTotal", mAmountDue);
            if(mTokenId != null){
                paymentNode.put("Token", mTokenId);
            } else if(mCardId != null){
                paymentNode.put("CardId", mCardId);
            }
            json.put("Patients", patients);

            if(mShippingId != null) {
                JSONObject shippingNode = new JSONObject();
                shippingNode.put("ResidenceId", mShippingId);
                // actual assembly now
                json.put("Shipping", shippingNode); // TODO: need the current value, because we need an onChange for the address selector
            }
            json.put("PromoCode", ((EditText) mRootView.findViewById(R.id.etCheckoutPromoCode)).getText().toString());
            // only if amount due bro
            if(mAmountDue > 0) {
                json.put("Subscription", ((CheckBox) mRootView.findViewById(R.id.cbCheckoutSubscription)).isChecked());
            } else {
                json.put("Subscription", false);
            }
            json.put("Payment", paymentNode);
        } catch(Exception e){
            Logcat.d( "json generation error, it's never gonna work now" + e.toString());
        }
        return json;
    }

    private void remoteValidate(){
        // this is the remote validation, typically handled onChange for relevant fields, and on setInterval while validating promos
        JSONObject payload = generatePayload();
        HealthNotifierAPI.getInstance().validateLifesquares(payload, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // we couldn't get a response or we timed out, offline, etc
                mValidationResults = null;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Logcat.d( "onFailure" + e.toString());
                        Toast.makeText(getActivity(), "Server Offline" + e.toString(), Toast.LENGTH_LONG).show();
                        handleRemoteValidation();
                    }
                });
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                if (response.isSuccessful()) { // exactly what status codes determine this?
                    try {
                        mValidationResults = new JSONObject(response.body().string());
                    } catch(Exception e){

                    }
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            handleRemoteValidation();
                        }
                    });
                } else {
                    // well not much we should do here
                    mValidationResults = null;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            handleRemoteValidation();
                        }
                    });
                }
            }
        });
    }

    private void handleRemoteValidation(){
        if(mValidationResults != null){
            // respond to the specific results
            // iterate patients and validate lifesquare status
            try {
                mAmountDue = mValidationResults.getInt("Total");
                // lol on not storing the promocode locally and going back to the textfield, but whatever
                String promoCode = ((EditText) mRootView.findViewById(R.id.etCheckoutPromoCode)).getText().toString();
                if(!promoCode.equals("")){
                    if(mValidationResults.getJSONObject("Promo").getBoolean("Valid")){
                        mPromoValid = true;
                    } else {
                        mPromoValid = false;
                    }
                }
                if(mAssignMethod.equals("claim") && mLifesquareCode != null){

                    mLifesquareValid = false;
                    // try to read patients [0]
                    JSONObject pJson = mValidationResults.getJSONArray("Patients").getJSONObject(0);
                    if(mLifesquareCode.equals(pJson.getString("LifesquareId"))){
                        if(pJson.getBoolean("Valid")){
                            mLifesquareValid = true;
                        }
                    }
                    if(!mLifesquareValid){
                        // if we're not doing our QR capture flow this is OTT SON
                        AlertDialog alertDialog = new AlertDialog.Builder(getContext()).create();
                        alertDialog.setTitle("Invalid LifeSticker " + mLifesquareCode);
                        alertDialog.setMessage("We couldn’t find a claimable LifeSticker. Please try again or select 'I need LifeStickers'");
                        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Try Again",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        //simulate dat click, and see what happens bro
                                        ((RadioButton) mRootView.findViewById(R.id.rbCheckoutClaim)).performClick();
                                        dialog.dismiss();
                                    }
                                });

                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Ok",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });

                        alertDialog.show();
                        mLifesquareCode = null;
                        mAssignMethod = "new";
                    }
                }
            } catch(Exception e){

            }
            // promo code status

        } else {
            // total failzone bro, reset all the things
        }
        doubleSecretInit();
    }

    private Card getCard(){
        try {
            String cardNumber = ((EditText) mRootView.findViewById(R.id.etCheckoutFormCardNumber)).getText().toString();
            Map.Entry<String, String> selectedMonth = (Map.Entry<String, String>) ((Spinner) mRootView.findViewById(R.id.spCheckoutFormExpirationMonth)).getSelectedItem();
            int cardExpMonth = Integer.valueOf(selectedMonth.getKey());
            Map.Entry<String, String> selectedYear = (Map.Entry<String, String>) ((Spinner) mRootView.findViewById(R.id.spCheckoutFormExpirationYear)).getSelectedItem();
            int cardExpYear = Integer.valueOf(selectedYear.getKey());
            String cardCVC = ((EditText) mRootView.findViewById(R.id.etCheckoutFormCVC)).getText().toString();
            Card card = new Card(
                    cardNumber,
                    cardExpMonth,
                    cardExpYear,
                    cardCVC
            );
            return card;
        } catch(Exception e){
            return null;
        }
    }

    private void onSubmit(){

        // use our new standard of payment validation though

        // the "press" handler yea?
        // logical validation times
        // no need to validate the server again
        // state of tokenizing card and then getting a server error, for another reason, unlikely but w/e
        // assume server state has been validated, so simply check client state again, and assemble dat payload son

        // first step, validation yea son, not to be confused with server validation which is data integrity checking
        FormValidation validationResults = new FormValidation();

        if(mLocked){
            return;
        }

        // quick double check on our captain obvious lifesquare request part
        // just say for example, we never validated the lifesquare or w/e, ok straw man but still
        // last line of defense
        Boolean requiresPaymentValidation = false; // a loose variable to check if we halt the submission process on behalf of payment, nothing is final, until we get back from the server

        // PERIOD END OF STORY
        if(mAmountDue > 0){
            requiresPaymentValidation = true;
        }

        if(mMode.equals("assign")){
            if(mAssignMethod == null){
                FieldValidation fieldError = new FieldValidation();
                fieldError.message = "Please choose if you need a new LifeSticker or are looking to claim/redeem a LifeSticker";
                validationResults.errors.add(fieldError);
                Forms.defaultDisplayFormValidation(mRootView, getContext(), validationResults);
                return;
            } else {
                if (mAssignMethod.equals("claim") && !mLifesquareValid) {
                    FieldValidation fieldError = new FieldValidation();
                    fieldError.message = "Attempting to claim invalid LifeSticker";
                    validationResults.errors.add(fieldError);
                    Forms.defaultDisplayFormValidation(mRootView, getContext(), validationResults);
                    return;
                }
                if(mAssignMethod.equals("new") && mShippingId == null) {
                    FieldValidation fieldError = new FieldValidation();
                    fieldError.message = "No shipping address found. Please return to your profile, add an address and try again.";
                    validationResults.errors.add(fieldError);
                    return;
                }
            }
        }


        if(mCardId != null && mTokenId == null){
            // and assume nothing "DIRTY" for a new credit card
            // this logic is slightly more complex than iOS since we have an inline form to contend with
            requiresPaymentValidation = false;
        }
        if(mTokenId != null){
            // for some strange reason mid abort cycle, but with a valid token?
            requiresPaymentValidation = false;
        }
        //
        if(mCardId != null){
            // TODO: mop it up son
            String cardNumber = ((EditText) mRootView.findViewById(R.id.etCheckoutFormCardNumber)).getText().toString();
            if(!cardNumber.equals("")){
                requiresPaymentValidation = true; // basically a cheap "dirty" check on new card entry, until we adjust our shizzle
            }
        }

        Logcat.d( "requiresPayment:" + requiresPaymentValidation.toString());
        //
        // check the card, if we haven't found valid payment yet
        // note, the interplay between starting to enter a new card and having existing cards
        // TODO: SLOOPPPPY fest, all this because "simplicity" and saving time to not have a dialog to enter the card
        Card card = null;
        if(requiresPaymentValidation) {
            card = getCard();
            if(card != null){
                if (!card.validateCard()) {
                    FieldValidation fieldError1 = new FieldValidation();
                    // fieldError.field = object;
                    fieldError1.message = "Payment required!";
                    validationResults.errors.add(fieldError1);
                    // ok now we have to look at each one? WTF SON
                    if (!card.validateNumber()) {
                        FieldValidation fieldError = new FieldValidation();
                        fieldError.message = "Invalid credit card number found!";
                        validationResults.errors.add(fieldError);
                    }
                    if (!card.validateExpiryDate()) {
                        FieldValidation fieldError = new FieldValidation();
                        fieldError.message = "Invalid credit card expiration found!";
                        validationResults.errors.add(fieldError);
                    }
                    if (!card.validateCVC()) {
                        FieldValidation fieldError = new FieldValidation();
                        fieldError.message = "Invalid credit card CVC found!";
                        validationResults.errors.add(fieldError);
                    }
                } else {
                    // we have a valid card entered
                    requiresPaymentValidation = false;
                    // now if we don't have a token, we need to go do that along the way, but that's server side, not client validation
                }
            } else {
                // something bombed
            }
        }


        if(validationResults.isValid()){
            if(mTokenId == null && card != null && card.validateCard()){
                // card is good, do tokenization
                //Logcat.d( "About to tokenizing card:" + card.toString());
                tokenizeCard(card);
            } else {
                submit();
            }

        } else {
            Forms.defaultDisplayFormValidation(mRootView, getContext(), validationResults);
        }
    }

    private void finishToSummary(){
        Toast.makeText(getActivity(), "Checkout Success! Please check your email for a receipt.", Toast.LENGTH_LONG).show();
        try {
            Intent intent;
            intent = new Intent(getContext(), PatientActivity.class);
            // intent.putExtra("PATIENT_JSON", mPatientJson.toString()); this will cause the Patient ACtivity to reload itself, or so we think
            // we can dry hump test this anyhow
            intent.putExtra("PATIENT_ID", mPatientJson.getJSONObject("profile").getString("uuid"));
            startActivity(intent);
        } catch(Exception e){
            getActivity().finish();
        }
    }

    private void submit(){
        mLocked = true;
        mMenu.findItem(R.id.action_checkout).setEnabled(false);
        JSONObject payload = generatePayload();
        mRootView.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        // when all is well and done, do the submission
        HealthNotifierAPI.getInstance().processLifesquares(mMode, payload, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // we couldn't get a response or we timed out, offline, etc
                mLocked = false;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Logcat.d( "onFailure" + e.toString());
                        mMenu.findItem(R.id.action_checkout).setEnabled(true);
                        mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                        Toast.makeText(getActivity(), "Server Offline" + e.toString(), Toast.LENGTH_LONG).show();
                    }
                });

            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                if (response.isSuccessful()) { // exactly what status codes determine this?
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            // very underwhelming, lololobro
                            // TODO: send to the success screen with some big thumbs up son, mad props on your props
                            finishToSummary();
                        }
                    });
                } else {
                    mLocked = false;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mMenu.findItem(R.id.action_checkout).setEnabled(true);
                            mRootView.findViewById(R.id.progressBar).setVisibility(View.GONE);
                            if(response.code() == 400){
                                Toast.makeText(getActivity(), "Invalid LifeStickers (not available). Please double check or choose \"I need LifeStickers\"", Toast.LENGTH_LONG).show();
                            }
                            if(response.code() == 402){
                                // TODO: parse response json
                                /*
                                if response.data != nil {
                            let json: JSON = JSON(data:response.data!)
                            if json["errors"].exists() {
                                for obj in json["errors"].arrayValue {
                                    messages.append(obj["message"].string!)
                                }
                            }
                            // what kind of error are we son
                            title = "Payment Error"
                        }
                                 */
                                Toast.makeText(getActivity(), "Payment Errors: ", Toast.LENGTH_LONG).show();
                            }
                            if(response.code() == 500){
                                Toast.makeText(getActivity(), "Server Error", Toast.LENGTH_SHORT).show();
                            }



                        }
                    });
                }
            }
        });
    }

    // TO simplify we're using the synchronous API
    private void tokenizeCard(Card card){
        Logcat.d( "token time for card:" + card.toString());
        mTokenId = null;
        try {
            String key;
            // TODO: deal with this in the config class though
            if(Config.API_ROOT.equals(Config.RELEASE_API_ROOT)){
                key = Config.RELEASE_STRIPE_PUBLISHABLE_KEY;
            } else {
                key = Config.DEBUG_STRIPE_PUBLISHABLE_KEY;
            }
            Stripe stripe = new Stripe(key);
            stripe.createToken(
                card,
                new TokenCallback() {
                    public void onSuccess(Token token) {
                        mCardId = null; // zero it out because we just changed priority
                        mTokenId = token.getId();
                        submit();
                    }
                    public void onError(Exception error) {
                        // Show localized error message
                        Toast.makeText(getContext(),
                                error.toString(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
            );
        } catch(Exception e){
            // PRESUMABLEY
            Toast.makeText(getContext(),
                    "Unknown Payment Error: Card Not Charged",
                    Toast.LENGTH_SHORT
            ).show();
            // TODO: LOG the authentication error, this is serious business like
            // TODO: dump a bucket of water on the devops
            // we has a production app with a key that is no longer valid or something, HUGE PROBLEM

        }
    }

    public void handleCaptureResult(String code){
        if(Validators.isValidLifesquare(code)) {
            mLifesquareCode = code;
            remoteValidate();
        } else {
            // generally speaking the only thing coming in here not being valid is the "" hack for cancelling the operation
            mLifesquareCode = null;
            Logcat.d("BAILED ON YOUR SHIT");
            mAssignMethod = "new";
            doubleSecretInit();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        HealthNotifierApplication.bus.register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        HealthNotifierApplication.bus.unregister(this);
    }

}
