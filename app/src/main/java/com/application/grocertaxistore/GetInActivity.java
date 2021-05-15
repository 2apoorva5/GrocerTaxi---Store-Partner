package com.application.grocertaxistore;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.application.grocertaxistore.Utilities.Constants;
import com.application.grocertaxistore.Utilities.PreferenceManager;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.tapadoo.alerter.Alerter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import java.util.List;

import maes.tech.intentanim.CustomIntent;

public class GetInActivity extends AppCompatActivity {

    private ImageView closeBtn;
    private TextInputLayout emailOrMobile, password;
    private TextView forgotPassword, setupBtn;
    private ConstraintLayout getInBtn;
    private CardView getInBtnContainer;
    private ProgressBar progressBar;

    private FirebaseAuth firebaseAuth;
    private CollectionReference storeRef;

    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_in);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(GetInActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorBackground));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        initViews();
        initFirebase();
        setActionOnViews();
    }

    private void initViews() {
        closeBtn = findViewById(R.id.close_btn);
        emailOrMobile = findViewById(R.id.email_or_mobile);
        password = findViewById(R.id.password);
        forgotPassword = findViewById(R.id.forgot_password);
        getInBtnContainer = findViewById(R.id.get_in_btn_container);
        getInBtn = findViewById(R.id.get_in_btn);
        progressBar = findViewById(R.id.progress_bar);
        setupBtn = findViewById(R.id.set_up_btn);
    }

    private void initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        storeRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_CITY)).collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_LOCALITY)).collection(Constants.KEY_COLLECTION_STORES);
    }

    private void setActionOnViews() {
        closeBtn.setOnClickListener(view -> onBackPressed());

        KeyboardVisibilityEvent.setEventListener(GetInActivity.this, isOpen -> {
            if (!isOpen) {
                emailOrMobile.clearFocus();
                password.clearFocus();
            }
        });

        forgotPassword.setOnClickListener(view -> {
            progressBar.setVisibility(View.GONE);
            getInBtnContainer.setVisibility(View.VISIBLE);
            getInBtn.setEnabled(true);

            startActivity(new Intent(GetInActivity.this, ForgotPasswordActivity.class));
            CustomIntent.customType(GetInActivity.this, "bottom-to-up");
        });

        getInBtn.setOnClickListener(v -> {
            UIUtil.hideKeyboard(GetInActivity.this);

            final String emailOrMobileValue = emailOrMobile.getEditText().getText().toString().toLowerCase().trim();

            if (!validateEmailOrMobile() | !validatePassword()) {
                return;
            } else {
                if (Patterns.EMAIL_ADDRESS.matcher(emailOrMobileValue).matches()) {
                    emailOrMobile.setError(null);
                    emailOrMobile.setErrorEnabled(false);

                    if (!isConnectedToInternet(GetInActivity.this)) {
                        showConnectToInternetDialog();
                        return;
                    } else {
                        login(emailOrMobileValue);
                    }
                } else if (emailOrMobileValue.matches("\\d{10}")) {
                    emailOrMobile.setError(null);
                    emailOrMobile.setErrorEnabled(false);

                    if (!isConnectedToInternet(GetInActivity.this)) {
                        showConnectToInternetDialog();
                        return;
                    } else {
                        getInBtnContainer.setVisibility(View.INVISIBLE);
                        getInBtn.setEnabled(false);
                        progressBar.setVisibility(View.VISIBLE);

                        storeRef.whereEqualTo(Constants.KEY_STORE_MOBILE, "+91" + emailOrMobileValue)
                                .get().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                List<DocumentSnapshot> documentSnapshots = task.getResult().getDocuments();
                                if (documentSnapshots.isEmpty()) {
                                    progressBar.setVisibility(View.GONE);
                                    getInBtnContainer.setVisibility(View.VISIBLE);
                                    getInBtn.setEnabled(true);

                                    YoYo.with(Techniques.Shake).duration(700).repeat(0).playOn(emailOrMobile);
                                    Alerter.create(GetInActivity.this)
                                            .setText("We couldn't retrieve any store with that mobile in the chosen city and locality.")
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
                                } else {
                                    String email = documentSnapshots.get(0).getString(Constants.KEY_STORE_EMAIL);
                                    login(email);
                                }
                            } else {
                                progressBar.setVisibility(View.GONE);
                                getInBtnContainer.setVisibility(View.VISIBLE);
                                getInBtn.setEnabled(true);

                                Alerter.create(GetInActivity.this)
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
                        }).addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE);
                            getInBtnContainer.setVisibility(View.VISIBLE);
                            getInBtn.setEnabled(true);

                            Alerter.create(GetInActivity.this)
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
                        });
                    }
                } else {
                    YoYo.with(Techniques.Shake).duration(700).repeat(0).playOn(emailOrMobile);
                    emailOrMobile.setError("Enter a valid email or mobile!");
                    emailOrMobile.requestFocus();
                }
            }
        });

        setupBtn.setOnClickListener(view -> {
            progressBar.setVisibility(View.GONE);
            getInBtnContainer.setVisibility(View.VISIBLE);
            getInBtn.setEnabled(true);

            startActivity(new Intent(GetInActivity.this, SetupStoreActivity.class));
            CustomIntent.customType(GetInActivity.this, "bottom-to-up");
            finish();
        });
    }

    private boolean validateEmailOrMobile() {
        String emailOrMobileValue = emailOrMobile.getEditText().getText().toString().toLowerCase().trim();

        if (emailOrMobileValue.isEmpty()) {
            emailOrMobile.setError("Enter your email or mobile!");
            emailOrMobile.requestFocus();
            return false;
        } else {
            emailOrMobile.setError(null);
            emailOrMobile.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validatePassword() {
        String passwordValue = password.getEditText().getText().toString().trim();

        if (passwordValue.isEmpty()) {
            password.setError("Without a password? Eh!");
            password.requestFocus();
            return false;
        } else {
            password.setError(null);
            password.setErrorEnabled(false);
            return true;
        }
    }

    private void login(final String email) {
        getInBtnContainer.setVisibility(View.INVISIBLE);
        getInBtn.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        firebaseAuth.signInWithEmailAndPassword(email, password.getEditText().getText().toString().trim())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        storeRef.whereEqualTo(Constants.KEY_STORE_EMAIL, email)
                                .get()
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                                        preferenceManager.putString(Constants.KEY_UID, task1.getResult().getDocuments().get(0).getString(Constants.KEY_UID));
                                        preferenceManager.putString(Constants.KEY_STORE_ID, task1.getResult().getDocuments().get(0).getString(Constants.KEY_STORE_ID));
                                        preferenceManager.putBoolean(Constants.KEY_STORE_STATUS, task1.getResult().getDocuments().get(0).getBoolean(Constants.KEY_STORE_STATUS));
                                        preferenceManager.putString(Constants.KEY_STORE_NAME, task1.getResult().getDocuments().get(0).getString(Constants.KEY_STORE_NAME));
                                        preferenceManager.putString(Constants.KEY_STORE_OWNER, task1.getResult().getDocuments().get(0).getString(Constants.KEY_STORE_OWNER));
                                        preferenceManager.putString(Constants.KEY_STORE_EMAIL, task1.getResult().getDocuments().get(0).getString(Constants.KEY_STORE_EMAIL));
                                        preferenceManager.putString(Constants.KEY_STORE_MOBILE, task1.getResult().getDocuments().get(0).getString(Constants.KEY_STORE_MOBILE));
                                        preferenceManager.putString(Constants.KEY_STORE_LOCATION, task1.getResult().getDocuments().get(0).getString(Constants.KEY_STORE_LOCATION));
                                        preferenceManager.putString(Constants.KEY_STORE_ADDRESS, task1.getResult().getDocuments().get(0).getString(Constants.KEY_STORE_ADDRESS));
                                        preferenceManager.putString(Constants.KEY_STORE_LATITUDE, String.valueOf(task1.getResult().getDocuments().get(0).getDouble(Constants.KEY_STORE_LATITUDE)));
                                        preferenceManager.putString(Constants.KEY_STORE_LONGITUDE, String.valueOf(task1.getResult().getDocuments().get(0).getDouble(Constants.KEY_STORE_LONGITUDE)));
                                        preferenceManager.putString(Constants.KEY_STORE_TIMING, task1.getResult().getDocuments().get(0).getString(Constants.KEY_STORE_TIMING));
                                        preferenceManager.putString(Constants.KEY_STORE_MINIMUM_ORDER_VALUE, String.valueOf(task1.getResult().getDocuments().get(0).getDouble(Constants.KEY_STORE_MINIMUM_ORDER_VALUE)));
                                        preferenceManager.putString(Constants.KEY_STORE_IMAGE, task1.getResult().getDocuments().get(0).getString(Constants.KEY_STORE_IMAGE));
                                        preferenceManager.putString(Constants.KEY_CITY, preferenceManager.getString(Constants.KEY_CITY));
                                        preferenceManager.putString(Constants.KEY_LOCALITY, preferenceManager.getString(Constants.KEY_LOCALITY));

                                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        CustomIntent.customType(GetInActivity.this, "fadein-to-fadeout");
                                        finish();
                                    } else {
                                        progressBar.setVisibility(View.GONE);
                                        getInBtnContainer.setVisibility(View.VISIBLE);
                                        getInBtn.setEnabled(true);

                                        Alerter.create(GetInActivity.this)
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
                                }).addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE);
                            getInBtnContainer.setVisibility(View.VISIBLE);
                            getInBtn.setEnabled(true);

                            Alerter.create(GetInActivity.this)
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
                        });
                    } else {
                        progressBar.setVisibility(View.GONE);
                        getInBtnContainer.setVisibility(View.VISIBLE);
                        getInBtn.setEnabled(true);

                        Alerter.create(GetInActivity.this)
                                .setText("Whoa! It seems you've got invalid credentials. Try again!")
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
                }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            getInBtnContainer.setVisibility(View.VISIBLE);
            getInBtn.setEnabled(true);

            Alerter.create(GetInActivity.this)
                    .setText("Whoa! It seems you've got invalid credentials. Try again!")
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
        });
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean isConnectedToInternet(GetInActivity getInActivity) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getInActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (null != networkInfo &&
                (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(GetInActivity.this)
                .setTitle("No Internet Connection!")
                .setMessage("Please connect to a network first to proceed from here!")
                .setCancelable(false)
                .setAnimation(R.raw.no_internet_connection)
                .setPositiveButton("Connect", R.drawable.ic_dialog_connect, (dialogInterface, which) -> {
                    dialogInterface.dismiss();
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                })
                .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss()).build();
        materialDialog.show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }
}