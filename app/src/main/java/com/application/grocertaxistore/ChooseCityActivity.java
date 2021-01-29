package com.application.grocertaxistore;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.application.grocertaxistore.Model.City;
import com.application.grocertaxistore.Utilities.Constants;
import com.application.grocertaxistore.Utilities.PreferenceManager;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.firebase.ui.firestore.paging.FirestorePagingAdapter;
import com.firebase.ui.firestore.paging.FirestorePagingOptions;
import com.firebase.ui.firestore.paging.LoadingState;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.tapadoo.alerter.Alerter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import java.util.ArrayList;
import java.util.Locale;

import maes.tech.intentanim.CustomIntent;
import pl.droidsonroids.gif.GifImageView;

public class ChooseCityActivity extends AppCompatActivity {

    private ImageView closeBtn, speechToText;
    private TextView chooseCitySubtitle;
    private GifImageView chooseCityGif;
    private EditText inputCitySearchField;
    private RecyclerView recyclerCity;
    private ProgressBar chooseCityProgressBar;

    private CollectionReference citiesRef;
    private FirestorePagingAdapter<City, CityViewHolder> cityAdapter;

    private PreferenceManager preferenceManager;
    private static int LAST_POSITION = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_city);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(ChooseCityActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorBackground));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        initViews();
        initFirebase();
        setActionOnViews();

        loadCities();
    }

    private void initViews() {
        closeBtn = findViewById(R.id.close_btn);
        chooseCitySubtitle = findViewById(R.id.choose_city_subtitle);
        chooseCityGif = findViewById(R.id.choose_city_gif);
        inputCitySearchField = findViewById(R.id.input_city_search_field);
        speechToText = findViewById(R.id.speech_to_text);
        recyclerCity = findViewById(R.id.recycler_city);
        chooseCityProgressBar = findViewById(R.id.choose_city_progress_bar);
    }

    private void initFirebase() {
        citiesRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_CITIES);
    }

    private void setActionOnViews() {
        closeBtn.setOnClickListener(view -> onBackPressed());

        KeyboardVisibilityEvent.setEventListener(ChooseCityActivity.this, isOpen -> {
            if (!isOpen) {
                inputCitySearchField.clearFocus();
            }
        });

        chooseCityProgressBar.setVisibility(View.VISIBLE);

        if (preferenceManager.getString(Constants.KEY_SIGNIN_SIGNUP_ACTION).equals("setup")) {
            chooseCitySubtitle.setText("What city your store would be in?");
        } else if (preferenceManager.getString(Constants.KEY_SIGNIN_SIGNUP_ACTION).equals("getIn")) {
            chooseCitySubtitle.setText("What city your store is in?");
        }

        speechToText.setOnClickListener(view -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, String.format("Tell us your city name!"));

            try {
                startActivityForResult(intent, 123);
            } catch (ActivityNotFoundException e) {
                Alerter.create(ChooseCityActivity.this)
                        .setText("Whoa! Something broke. Try again!")
                        .setTextAppearance(R.style.AlertText)
                        .setBackgroundColorRes(R.color.errorColor)
                        .setIcon(R.drawable.ic_error)
                        .setDuration(3000)
                        .enableIconPulse(true)
                        .enableVibration(true)
                        .disableOutsideTouch()
                        .enableProgress(true)
                        .setProgressColorInt(getColor(android.R.color.white))
                        .show();
                return;
            }
        });

        inputCitySearchField.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                chooseCityGif.setForeground(new ColorDrawable(Color.parseColor("#80000000")));
            } else {
                chooseCityGif.setForeground(new ColorDrawable(Color.parseColor("#00000000")));
            }
        });

        inputCitySearchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                Query updatedQuery;
                if (s.toString().isEmpty()) {
                    updatedQuery = citiesRef.orderBy("name", Query.Direction.ASCENDING);
                } else {
                    updatedQuery = citiesRef.orderBy("searchKeyword", Query.Direction.ASCENDING)
                            .startAt(s.toString().toLowerCase().trim()).endAt(s.toString().toLowerCase().trim() + "\uf8ff");
                }

                PagedList.Config updatedConfig = new PagedList.Config.Builder()
                        .setInitialLoadSizeHint(8)
                        .setPageSize(4)
                        .build();

                FirestorePagingOptions<City> updatedOptions = new FirestorePagingOptions.Builder<City>()
                        .setLifecycleOwner(ChooseCityActivity.this)
                        .setQuery(updatedQuery, updatedConfig, City.class)
                        .build();

                cityAdapter.updateOptions(updatedOptions);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 123:
                if (resultCode == RESULT_OK && data != null) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    inputCitySearchField.setText(result.get(0));
                    inputCitySearchField.clearFocus();
                }
                break;
        }
    }

    private void loadCities() {
        Query query = citiesRef.orderBy("name", Query.Direction.ASCENDING);

        PagedList.Config config = new PagedList.Config.Builder()
                .setInitialLoadSizeHint(8)
                .setPageSize(4)
                .build();

        FirestorePagingOptions<City> options = new FirestorePagingOptions.Builder<City>()
                .setLifecycleOwner(ChooseCityActivity.this)
                .setQuery(query, config, City.class)
                .build();

        cityAdapter = new FirestorePagingAdapter<City, CityViewHolder>(options) {

            @NonNull
            @Override
            public CityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_city_item, parent, false);
                return new CityViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull CityViewHolder holder, int position, @NonNull City model) {
                holder.cityName.setText(model.getName());

                holder.clickListener.setOnClickListener(view -> {
                    UIUtil.hideKeyboard(ChooseCityActivity.this);
                    notifyDataSetChanged();

                    preferenceManager.putString(Constants.KEY_CITY, model.getName());

                    startActivity(new Intent(ChooseCityActivity.this, ChooseLocalityActivity.class));
                    CustomIntent.customType(ChooseCityActivity.this, "left-to-right");
                });

                setAnimation(holder.itemView, position);
            }

            public void setAnimation(View viewToAnimate, int position) {
                if (position > LAST_POSITION) {
                    ScaleAnimation scaleAnimation = new ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f,
                            Animation.RELATIVE_TO_SELF, 0.5f,
                            Animation.RELATIVE_TO_SELF, 0.5f);
                    scaleAnimation.setDuration(1000);

                    viewToAnimate.setAnimation(scaleAnimation);
                    LAST_POSITION = position;
                }
            }

            @Override
            protected void onLoadingStateChanged(@NonNull LoadingState state) {
                super.onLoadingStateChanged(state);
                switch (state) {
                    case LOADING_INITIAL:
                    case LOADING_MORE:
                        chooseCityProgressBar.setVisibility(View.VISIBLE);
                        break;
                    case LOADED:
                    case FINISHED:
                        chooseCityProgressBar.setVisibility(View.GONE);
                        break;
                    case ERROR:
                        Alerter.create(ChooseCityActivity.this)
                                .setText("Whoa! Something Broke. Try again!")
                                .setTextAppearance(R.style.AlertText)
                                .setBackgroundColorRes(R.color.errorColor)
                                .setIcon(R.drawable.ic_error)
                                .setDuration(3000)
                                .enableIconPulse(true)
                                .enableVibration(true)
                                .disableOutsideTouch()
                                .enableProgress(true)
                                .setProgressColorInt(getColor(android.R.color.white))
                                .show();
                        break;
                }
            }
        };

        cityAdapter.notifyDataSetChanged();

        recyclerCity.setHasFixedSize(true);
        recyclerCity.setLayoutManager(new LinearLayoutManager(ChooseCityActivity.this));
        recyclerCity.setAdapter(cityAdapter);
    }

    public static class CityViewHolder extends RecyclerView.ViewHolder {

        ConstraintLayout clickListener;
        TextView cityName;

        public CityViewHolder(@NonNull View itemView) {
            super(itemView);

            clickListener = itemView.findViewById(R.id.click_listener);
            cityName = itemView.findViewById(R.id.item_city_name);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }
}