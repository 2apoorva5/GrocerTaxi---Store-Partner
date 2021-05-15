package com.application.grocertaxistore;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.application.grocertaxistore.Utilities.Constants;
import com.application.grocertaxistore.Utilities.PreferenceManager;

import maes.tech.intentanim.CustomIntent;

public class WelcomeActivity extends AppCompatActivity {

    private ConstraintLayout setupStoreBtn, alreadyRegisteredBtn;

    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(WelcomeActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorPrimary));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        initViews();
        setActionOnViews();
    }

    private void initViews() {
        setupStoreBtn = findViewById(R.id.setup_store_btn);
        alreadyRegisteredBtn = findViewById(R.id.already_registered_btn);
    }

    private void setActionOnViews() {
        setupStoreBtn.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_SIGNIN_SIGNUP_ACTION, "setUp");
            startActivity(new Intent(WelcomeActivity.this, ChooseCityActivity.class));
            CustomIntent.customType(WelcomeActivity.this, "fadein-to-fadeout");
            finish();
        });

        alreadyRegisteredBtn.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_SIGNIN_SIGNUP_ACTION, "getIn");
            startActivity(new Intent(WelcomeActivity.this, ChooseCityActivity.class));
            CustomIntent.customType(WelcomeActivity.this, "fadein-to-fadeout");
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }
}