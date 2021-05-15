package com.application.grocertaxistore;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.application.grocertaxistore.Utilities.Constants;
import com.application.grocertaxistore.Utilities.PreferenceManager;
import com.chaos.view.PinView;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.tapadoo.alerter.Alerter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import maes.tech.intentanim.CustomIntent;

public class VerifyOTPActivity extends AppCompatActivity {

    private ImageView closeBtn;
    private TextView verifyOtpSubtitle, resendOtpBtn;
    private PinView otpPinView;
    private ConstraintLayout verifyBtn;
    private CardView verifyBtnContainer;
    private ProgressBar progressBar;

    private FirebaseAuth firebaseAuth;
    private CollectionReference storeRef;
    private StorageReference storageReference;
    private PhoneAuthProvider.ForceResendingToken resendingToken;

    private String storeID, image, storeName, ownerName, email, mobile, password, totalTime, minimumOrderAmount;
    private String codeBySystem;
    private Timer timer;
    private int count = 60;
    private Uri imageUri = null;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(VerifyOTPActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorBackground));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        Intent intent = getIntent();
        storeID = intent.getStringExtra("store_id");
        storeName = intent.getStringExtra("store_name");
        ownerName = intent.getStringExtra("owner_name");
        email = intent.getStringExtra("email");
        mobile = intent.getStringExtra("mobile");
        password = intent.getStringExtra("password");
        totalTime = intent.getStringExtra("total_time");
        minimumOrderAmount = intent.getStringExtra("min_order_amount");
        image = intent.getStringExtra("image");

        initViews();
        initFirebase();
        setActionOnViews();

        KeyboardVisibilityEvent.setEventListener(VerifyOTPActivity.this, isOpen -> {
            if (!isOpen) {
                otpPinView.clearFocus();
            }
        });

        sendVerificationCodeToUser(mobile);
    }

    private void initViews() {
        closeBtn = findViewById(R.id.close_btn);
        verifyOtpSubtitle = findViewById(R.id.verify_otp_subtitle);
        resendOtpBtn = findViewById(R.id.resend_otp_btn);
        otpPinView = findViewById(R.id.verify_otp_pinview);
        verifyBtn = findViewById(R.id.verify_btn);
        verifyBtnContainer = findViewById(R.id.verify_btn_container);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        storeRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_CITY)).collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_LOCALITY)).collection(Constants.KEY_COLLECTION_STORES);
        storageReference = FirebaseStorage.getInstance().getReference("StorePics/");
    }

    private void setActionOnViews() {
        closeBtn.setOnClickListener(v -> onBackPressed());

        verifyOtpSubtitle.setText(String.format("Enter below the One Time Password\nsent to %s.", mobile));

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                VerifyOTPActivity.this.runOnUiThread(() -> {
                    if (count == 0) {
                        resendOtpBtn.setText("Resend OTP");
                        resendOtpBtn.setTextColor(getColor(R.color.colorAccent));
                        resendOtpBtn.setEnabled(true);
                        resendOtpBtn.setOnClickListener(v -> {
                            if (!isConnectedToInternet(VerifyOTPActivity.this)) {
                                showConnectToInternetDialog();
                                return;
                            } else {
                                resendOTP();
                                resendOtpBtn.setEnabled(false);
                                resendOtpBtn.setAlpha(0.5f);
                                count = 60;
                            }
                        });
                    } else {
                        resendOtpBtn.setText(String.format("Resend OTP in %d", count));
                        resendOtpBtn.setTextColor(getColor(R.color.colorInactive));
                        resendOtpBtn.setEnabled(false);
                        count--;
                    }
                });
            }
        }, 0, 1000);

        verifyBtn.setOnClickListener(view -> {
            if (!isConnectedToInternet(VerifyOTPActivity.this)) {
                showConnectToInternetDialog();
                return;
            } else {
                if (String.valueOf(otpPinView.getText()).isEmpty() || String.valueOf(otpPinView.getText()).length() != 6) {
                    progressBar.setVisibility(View.INVISIBLE);
                    verifyBtnContainer.setVisibility(View.VISIBLE);
                    verifyBtn.setEnabled(true);

                    UIUtil.hideKeyboard(VerifyOTPActivity.this);
                    YoYo.with(Techniques.Shake).duration(700).repeat(0).playOn(otpPinView);
                    Alerter.create(VerifyOTPActivity.this)
                            .setText("Enter the valid OTP received on " + mobile + "!")
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
                    verifyCode(String.valueOf(otpPinView.getText()));
                }
            }
        });
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                    super.onCodeSent(s, forceResendingToken);
                    resendingToken = forceResendingToken;
                    codeBySystem = s;
                }

                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                    String code = phoneAuthCredential.getSmsCode();
                    if (code != null) {
                        otpPinView.setText(code);
                        verifyCode(code);
                    }
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    if (e instanceof FirebaseAuthInvalidCredentialsException) {
                        progressBar.setVisibility(View.INVISIBLE);
                        verifyBtnContainer.setVisibility(View.VISIBLE);
                        verifyBtn.setEnabled(true);

                        Alerter.create(VerifyOTPActivity.this)
                                .setText("Whoa! It seems you've got an invalid code!")
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
                    } else if (e instanceof FirebaseTooManyRequestsException) {
                        progressBar.setVisibility(View.INVISIBLE);
                        verifyBtnContainer.setVisibility(View.VISIBLE);
                        verifyBtn.setEnabled(true);

                        Alerter.create(VerifyOTPActivity.this)
                                .setText("Too many requests at the moment. Try again after 5 hours now!")
                                .setTextAppearance(R.style.AlertText)
                                .setBackgroundColorRes(R.color.infoColor)
                                .setIcon(R.drawable.ic_info)
                                .setDuration(3000)
                                .enableIconPulse(true)
                                .enableVibration(true)
                                .disableOutsideTouch()
                                .enableProgress(true)
                                .setProgressColorInt(getColor(android.R.color.white))
                                .show();
                        return;
                    }
                }
            };

    private void sendVerificationCodeToUser(String mobile) {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(firebaseAuth)
                        .setPhoneNumber(mobile)                            // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS)           // Timeout and unit
                        .setActivity(this)                                 // Activity (for callback binding)
                        .setCallbacks(mCallbacks)                          // OnVerificationStateChangedCallbacks
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void resendOTP() {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(firebaseAuth)
                        .setPhoneNumber(mobile)                            // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS)           // Timeout and unit
                        .setActivity(this)                                 // Activity (for callback binding)
                        .setCallbacks(mCallbacks)                          // OnVerificationStateChangedCallbacks
                        .setForceResendingToken(resendingToken)
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyCode(String code) {
        verifyBtnContainer.setVisibility(View.INVISIBLE);
        verifyBtn.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(codeBySystem, code);
        signInUsingCredential(credential);
    }

    private void signInUsingCredential(PhoneAuthCredential credential) {
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = task.getResult().getUser();
                        AuthCredential authCredential = EmailAuthProvider.getCredential(email, password);
                        assert user != null;
                        user.linkWithCredential(authCredential).addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                if (!image.isEmpty() && image.length() != 0 && image != null && !image.equals("")) {
                                    imageUri = Uri.parse(image);

                                    final StorageReference fileRef = storageReference.child(storeID + ".img");

                                    fileRef.putFile(imageUri)
                                            .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl()
                                                    .addOnSuccessListener(uri -> {

                                                        final String imageValue = uri.toString();

                                                        HashMap<String, Object> newStore = new HashMap<>();
                                                        newStore.put(Constants.KEY_UID, user.getUid());
                                                        newStore.put(Constants.KEY_STORE_ID, storeID);
                                                        newStore.put(Constants.KEY_STORE_TIMESTAMP, FieldValue.serverTimestamp());
                                                        newStore.put(Constants.KEY_STORE_IMAGE, imageValue);
                                                        newStore.put(Constants.KEY_STORE_NAME, storeName);
                                                        newStore.put(Constants.KEY_STORE_OWNER, ownerName);
                                                        newStore.put(Constants.KEY_STORE_EMAIL, email);
                                                        newStore.put(Constants.KEY_STORE_MOBILE, mobile);
                                                        newStore.put(Constants.KEY_STORE_LOCATION, "");
                                                        newStore.put(Constants.KEY_STORE_ADDRESS, "");
                                                        newStore.put(Constants.KEY_STORE_LATITUDE, 0);
                                                        newStore.put(Constants.KEY_STORE_LONGITUDE, 0);
                                                        newStore.put(Constants.KEY_STORE_TIMING, totalTime);
                                                        newStore.put(Constants.KEY_STORE_MINIMUM_ORDER_VALUE, Double.valueOf(minimumOrderAmount));
                                                        newStore.put(Constants.KEY_STORE_STATUS, true);
                                                        newStore.put(Constants.KEY_STORE_AVERAGE_RATING, 0);
                                                        newStore.put(Constants.KEY_STORE_SEARCH_KEYWORD, storeName.toLowerCase());

                                                        storeRef.document(storeID).set(newStore)
                                                                .addOnSuccessListener(aVoid -> {
                                                                    timer.cancel();

                                                                    progressBar.setVisibility(View.GONE);
                                                                    verifyBtnContainer.setVisibility(View.VISIBLE);
                                                                    verifyBtn.setEnabled(true);

                                                                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                                                                    preferenceManager.putString(Constants.KEY_UID, user.getUid());
                                                                    preferenceManager.putString(Constants.KEY_STORE_ID, storeID);
                                                                    preferenceManager.putString(Constants.KEY_STORE_IMAGE, imageValue);
                                                                    preferenceManager.putString(Constants.KEY_STORE_NAME, storeName);
                                                                    preferenceManager.putString(Constants.KEY_STORE_OWNER, ownerName);
                                                                    preferenceManager.putString(Constants.KEY_STORE_EMAIL, email);
                                                                    preferenceManager.putString(Constants.KEY_STORE_MOBILE, mobile);
                                                                    preferenceManager.putString(Constants.KEY_STORE_LOCATION, "");
                                                                    preferenceManager.putString(Constants.KEY_STORE_ADDRESS, "");
                                                                    preferenceManager.putString(Constants.KEY_STORE_LATITUDE, String.valueOf(0));
                                                                    preferenceManager.putString(Constants.KEY_STORE_LONGITUDE, String.valueOf(0));
                                                                    preferenceManager.putString(Constants.KEY_STORE_TIMING, totalTime);
                                                                    preferenceManager.putString(Constants.KEY_STORE_MINIMUM_ORDER_VALUE, minimumOrderAmount);
                                                                    preferenceManager.putBoolean(Constants.KEY_STORE_STATUS, true);
                                                                    preferenceManager.putString(Constants.KEY_CITY, preferenceManager.getString(Constants.KEY_CITY));
                                                                    preferenceManager.putString(Constants.KEY_LOCALITY, preferenceManager.getString(Constants.KEY_LOCALITY));

                                                                    Intent intent = new Intent(getApplicationContext(), LocationPermissionActivity.class);
                                                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                                    startActivity(intent);
                                                                    CustomIntent.customType(VerifyOTPActivity.this, "bottom-to-up");
                                                                    finish();
                                                                }).addOnFailureListener(e -> {
                                                            progressBar.setVisibility(View.GONE);
                                                            verifyBtnContainer.setVisibility(View.VISIBLE);
                                                            verifyBtn.setEnabled(true);

                                                            Alerter.create(VerifyOTPActivity.this)
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
                                                    }).addOnFailureListener(e -> {
                                                        progressBar.setVisibility(View.GONE);
                                                        verifyBtnContainer.setVisibility(View.VISIBLE);
                                                        verifyBtn.setEnabled(true);

                                                        Alerter.create(VerifyOTPActivity.this)
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
                                                    })).addOnFailureListener(e -> {
                                        progressBar.setVisibility(View.GONE);
                                        verifyBtnContainer.setVisibility(View.VISIBLE);
                                        verifyBtn.setEnabled(true);

                                        Alerter.create(VerifyOTPActivity.this)
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
                                    startActivity(new Intent(VerifyOTPActivity.this, SetupStoreActivity.class));
                                    CustomIntent.customType(VerifyOTPActivity.this, "right-to-left");
                                    finish();
                                }
                            } else {
                                progressBar.setVisibility(View.GONE);
                                verifyBtnContainer.setVisibility(View.VISIBLE);
                                verifyBtn.setEnabled(true);

                                Alerter.create(VerifyOTPActivity.this)
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
                            verifyBtnContainer.setVisibility(View.VISIBLE);
                            verifyBtn.setEnabled(true);

                            Alerter.create(VerifyOTPActivity.this)
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
                        verifyBtnContainer.setVisibility(View.VISIBLE);
                        verifyBtn.setEnabled(true);

                        Alerter.create(VerifyOTPActivity.this)
                                .setText("Whoa! OTP verification failed. Try again!")
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
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    verifyBtnContainer.setVisibility(View.VISIBLE);
                    verifyBtn.setEnabled(true);

                    Alerter.create(VerifyOTPActivity.this)
                            .setText("Whoa! OTP verification failed. Try again!")
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

    private boolean isConnectedToInternet(VerifyOTPActivity verifyOTPActivity) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) verifyOTPActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (null != networkInfo &&
                (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(VerifyOTPActivity.this)
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
    protected void onStop() {
        super.onStop();
        timer.cancel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timer.cancel();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }
}