package com.application.grocertaxistore;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.application.grocertaxistore.Utilities.Constants;
import com.application.grocertaxistore.Utilities.PreferenceManager;
import com.bumptech.glide.Glide;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.makeramen.roundedimageview.RoundedImageView;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.tapadoo.alerter.Alerter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import java.util.Calendar;
import java.util.Random;
import java.util.regex.Pattern;

import maes.tech.intentanim.CustomIntent;

public class SetupStoreActivity extends AppCompatActivity {

    private ImageView closeBtn;
    private RoundedImageView storeImage;
    private TextInputLayout storeName, owner, email, mobile, createPassword, minimumOrderAmount;
    private CardView completeSetupBtnContainer;
    private ConstraintLayout completeSetupBtn;
    private ProgressBar progressBar;
    private TextView getInBtn, openingTime, closingTime;

    private FirebaseAuth firebaseAuth;
    private CollectionReference userRef, storeRef;

    private PreferenceManager preferenceManager;
    private Uri storePicUri = null;
    int openingHour, openingMinute, closingHour, closingMinute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_store);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(SetupStoreActivity.this, "fadein-to-fadeout");
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
        storeImage = findViewById(R.id.store_image);
        storeName = findViewById(R.id.store_name);
        owner = findViewById(R.id.owner);
        email = findViewById(R.id.email);
        mobile = findViewById(R.id.mobile);
        createPassword = findViewById(R.id.create_password);
        openingTime = findViewById(R.id.opening_time);
        closingTime = findViewById(R.id.closing_time);
        minimumOrderAmount = findViewById(R.id.minimum_order_amount);
        completeSetupBtnContainer = findViewById(R.id.complete_setup_btn_container);
        completeSetupBtn = findViewById(R.id.complete_setup_btn);
        progressBar = findViewById(R.id.progress_bar);
        getInBtn = findViewById(R.id.get_in_btn);
    }

    private void initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        userRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS);
        storeRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_CITY)).collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_LOCALITY)).collection(Constants.KEY_COLLECTION_STORES);
    }

    private void setActionOnViews() {
        closeBtn.setOnClickListener(view -> onBackPressed());

        KeyboardVisibilityEvent.setEventListener(SetupStoreActivity.this, isOpen -> {
            if (!isOpen) {
                storeName.clearFocus();
                owner.clearFocus();
                mobile.clearFocus();
                email.clearFocus();
                createPassword.clearFocus();
                openingTime.clearFocus();
                closingTime.clearFocus();
            }
        });

        storeImage.setOnClickListener(v -> selectImage());

        openingTime.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    SetupStoreActivity.this,
                    (view, hourOfDay, minute) -> {
                        openingHour = hourOfDay;
                        openingMinute = minute;
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(0, 0, 0, openingHour, openingMinute);
                        openingTime.setText(DateFormat.format("hh:mm aa", calendar));
                    }, 12, 0, false
            );
            timePickerDialog.updateTime(openingHour, openingMinute);
            timePickerDialog.show();
        });

        closingTime.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    SetupStoreActivity.this,
                    (view, hourOfDay, minute) -> {
                        closingHour = hourOfDay;
                        closingMinute = minute;
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(0, 0, 0, closingHour, closingMinute);
                        closingTime.setText(DateFormat.format("hh:mm aa", calendar));
                    }, 12, 0, false
            );
            timePickerDialog.updateTime(closingHour, closingMinute);
            timePickerDialog.show();
        });

        completeSetupBtn.setOnClickListener(v -> {
            UIUtil.hideKeyboard(SetupStoreActivity.this);

            final String email_id = email.getEditText().getText().toString().toLowerCase().trim();
            final String mobile_no = mobile.getPrefixText().toString().trim() + mobile.getEditText().getText().toString().trim();

            if (!validateStoreName() | !validateOwnerName() | !validateEmail() | !validateMobile()
                    | !validatePassword() | !validateOpeningTime() | !validateClosingTime() | !validateMinimumOrderAmount()) {
                return;
            } else {
                if (!isConnectedToInternet(SetupStoreActivity.this)) {
                    showConnectToInternetDialog();
                    return;
                } else {
                    completeSetupBtnContainer.setVisibility(View.INVISIBLE);
                    completeSetupBtn.setEnabled(false);
                    progressBar.setVisibility(View.VISIBLE);

                    firebaseAuth.fetchSignInMethodsForEmail(email_id)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    if (task.getResult().getSignInMethods().isEmpty() &&
                                            task.getResult().getSignInMethods().size() == 0) {
                                        userRef.whereEqualTo(Constants.KEY_USER_MOBILE, mobile_no)
                                                .get().addOnCompleteListener(task1 -> {
                                            if (task1.isSuccessful()) {
                                                if (task1.getResult().getDocuments().isEmpty() &&
                                                        task1.getResult().getDocuments().size() == 0) {
                                                    storeRef.whereEqualTo(Constants.KEY_STORE_MOBILE, mobile_no)
                                                            .get().addOnCompleteListener(task2 -> {
                                                        if (task2.isSuccessful()) {
                                                            if (task2.getResult().getDocuments().isEmpty() &&
                                                                    task2.getResult().getDocuments().size() == 0) {
                                                                openVerifyOTP();
                                                            } else {
                                                                progressBar.setVisibility(View.GONE);
                                                                completeSetupBtnContainer.setVisibility(View.VISIBLE);
                                                                completeSetupBtn.setEnabled(true);

                                                                YoYo.with(Techniques.Shake).duration(700).repeat(0).playOn(mobile);
                                                                Alerter.create(SetupStoreActivity.this)
                                                                        .setText("That mobile has already been registered. Try another!")
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
                                                        } else {
                                                            progressBar.setVisibility(View.GONE);
                                                            completeSetupBtnContainer.setVisibility(View.VISIBLE);
                                                            completeSetupBtn.setEnabled(true);

                                                            Alerter.create(SetupStoreActivity.this)
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
                                                        completeSetupBtnContainer.setVisibility(View.VISIBLE);
                                                        completeSetupBtn.setEnabled(true);

                                                        Alerter.create(SetupStoreActivity.this)
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
                                                    completeSetupBtnContainer.setVisibility(View.VISIBLE);
                                                    completeSetupBtn.setEnabled(true);

                                                    YoYo.with(Techniques.Shake).duration(700).repeat(0).playOn(mobile);
                                                    Alerter.create(SetupStoreActivity.this)
                                                            .setText("That mobile has already been registered. Try another!")
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
                                            } else {
                                                progressBar.setVisibility(View.GONE);
                                                completeSetupBtnContainer.setVisibility(View.VISIBLE);
                                                completeSetupBtn.setEnabled(true);

                                                Alerter.create(SetupStoreActivity.this)
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
                                            completeSetupBtnContainer.setVisibility(View.VISIBLE);
                                            completeSetupBtn.setEnabled(true);

                                            Alerter.create(SetupStoreActivity.this)
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
                                        completeSetupBtnContainer.setVisibility(View.VISIBLE);
                                        completeSetupBtn.setEnabled(true);

                                        YoYo.with(Techniques.Shake).duration(700).repeat(0).playOn(email);
                                        Alerter.create(SetupStoreActivity.this)
                                                .setText("That email has already been registered. Try another!")
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
                                } else {
                                    progressBar.setVisibility(View.GONE);
                                    completeSetupBtnContainer.setVisibility(View.VISIBLE);
                                    completeSetupBtn.setEnabled(true);

                                    Alerter.create(SetupStoreActivity.this)
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
                        completeSetupBtnContainer.setVisibility(View.VISIBLE);
                        completeSetupBtn.setEnabled(true);

                        Alerter.create(SetupStoreActivity.this)
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
            }
        });

        getInBtn.setOnClickListener(view -> {
            progressBar.setVisibility(View.GONE);
            completeSetupBtnContainer.setVisibility(View.VISIBLE);
            completeSetupBtn.setEnabled(true);

            startActivity(new Intent(SetupStoreActivity.this, GetInActivity.class));
            CustomIntent.customType(SetupStoreActivity.this, "bottom-to-up");
            finish();
        });
    }

    private boolean validateStoreName() {
        String store_name = storeName.getEditText().getText().toString().trim();

        if (store_name.isEmpty()) {
            storeName.setError("Enter the store name!");
            storeName.requestFocus();
            return false;
        } else {
            storeName.setError(null);
            storeName.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validateOwnerName() {
        String owner_name = owner.getEditText().getText().toString().trim();

        if (owner_name.isEmpty()) {
            owner.setError("Enter the owner's name!");
            owner.requestFocus();
            return false;
        } else {
            owner.setError(null);
            owner.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validateEmail() {
        String email_id = email.getEditText().getText().toString().toLowerCase().trim();

        if (email_id.isEmpty()) {
            email.setError("Enter an email to set up account!");
            email.requestFocus();
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email_id).matches()) {
            email.setError("Invalid email. Try again!");
            email.requestFocus();
            return false;
        } else {
            email.setError(null);
            email.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validateMobile() {
        String mobile_no = mobile.getEditText().getText().toString().trim();

        if (mobile_no.isEmpty()) {
            mobile.setError("Enter a mobile to verify account!");
            mobile.requestFocus();
            return false;
        } else if (mobile_no.length() != 10) {
            mobile.setError("Invalid mobile. Try Again!");
            mobile.requestFocus();
            return false;
        } else {
            mobile.setError(null);
            mobile.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validatePassword() {
        String password = createPassword.getEditText().getText().toString().trim();

        Pattern PASSWORD_PATTERN = Pattern.compile("^" +
                "(?=.*[0-9])" +                 //at least 1 digit
                "(?=.*[a-z])" +                 //at least 1 lowercase letter
                "(?=.*[A-Z])" +                 //at least 1 uppercase letter
                "(?=.*[!@#$%^&*+=_])" +         //at least 1 special character
                "(?=\\S+$)" +                   //no white spaces
                ".{6,}" +                       //at least 6-character long
                "$");

        if (password.isEmpty()) {
            createPassword.setError("Create a password for the account!");
            createPassword.requestFocus();
            return false;
        } else if (!PASSWORD_PATTERN.matcher(password).matches()) {
            createPassword.setError("Invalid! Requires a minimum of 6 characters with no white spaces, at least 1 digit, 1 lowercase letter, 1 uppercase letter and 1 special character.");
            createPassword.requestFocus();
            return false;
        } else {
            createPassword.setError(null);
            createPassword.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validateOpeningTime() {
        String opening_time = openingTime.getText().toString().trim();

        if (opening_time.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            completeSetupBtnContainer.setVisibility(View.VISIBLE);
            completeSetupBtn.setEnabled(true);

            Alerter.create(SetupStoreActivity.this)
                    .setText("Enter an opening time for the store!")
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
            return false;
        } else {
            return true;
        }
    }

    private boolean validateClosingTime() {
        String closing_time = closingTime.getText().toString().trim();

        if (closing_time.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            completeSetupBtnContainer.setVisibility(View.VISIBLE);
            completeSetupBtn.setEnabled(true);

            Alerter.create(SetupStoreActivity.this)
                    .setText("Enter a closing time for the store!")
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
            return false;
        } else {
            return true;
        }
    }

    private boolean validateMinimumOrderAmount() {
        String min_order_amount = minimumOrderAmount.getEditText().getText().toString().trim();

        if (min_order_amount.isEmpty()) {
            minimumOrderAmount.setError("Enter a minimum order limit for home delivery!");
            minimumOrderAmount.requestFocus();
            return false;
        } else {
            minimumOrderAmount.setError(null);
            minimumOrderAmount.setErrorEnabled(false);
            return true;
        }
    }

    private void selectImage() {
        ImagePicker.Companion.with(SetupStoreActivity.this)
                .crop()
                .compress(1024)
                .maxResultSize(1080, 1080)
                .start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            storePicUri = data.getData();
            Glide.with(SetupStoreActivity.this).load(storePicUri).centerCrop().into(storeImage);
        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Alerter.create(SetupStoreActivity.this)
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
        } else {
            return;
        }
    }

    private void openVerifyOTP() {
        final String store_name = storeName.getEditText().getText().toString().trim();
        final String owner_name = owner.getEditText().getText().toString().trim();
        final String email_id = email.getEditText().getText().toString().toLowerCase().trim();
        final String mobile_no = mobile.getPrefixText().toString().trim() + mobile.getEditText().getText().toString().trim();
        final String create_password = createPassword.getEditText().getText().toString().trim();

        final String opening_time = openingTime.getText().toString().trim();
        final String closing_time = closingTime.getText().toString().trim();
        final String total_time = opening_time + " - " + closing_time;

        final String min_order_amount = minimumOrderAmount.getEditText().getText().toString().trim();

        Random random = new Random();
        int number1 = random.nextInt(9000) + 1000;
        int number2 = random.nextInt(9000) + 1000;
        int number3 = random.nextInt(9000) + 1000;

        if (storePicUri != null) {
            progressBar.setVisibility(View.GONE);
            completeSetupBtnContainer.setVisibility(View.VISIBLE);
            completeSetupBtn.setEnabled(true);

            Intent intent = new Intent(SetupStoreActivity.this, VerifyOTPActivity.class);
            intent.putExtra("store_id", String.format("STORE-%d%d%d", number1, number2, number3));
            intent.putExtra("store_name", store_name);
            intent.putExtra("owner_name", owner_name);
            intent.putExtra("email", email_id);
            intent.putExtra("mobile", mobile_no);
            intent.putExtra("password", create_password);
            intent.putExtra("total_time", total_time);
            intent.putExtra("min_order_amount", min_order_amount);
            intent.putExtra("image", storePicUri.toString());
            startActivity(intent);
            CustomIntent.customType(SetupStoreActivity.this, "left-to-right");
            finish();
        } else {
            progressBar.setVisibility(View.GONE);
            completeSetupBtnContainer.setVisibility(View.VISIBLE);
            completeSetupBtn.setEnabled(true);

            Alerter.create(SetupStoreActivity.this)
                    .setText("Choose an image for your store first!")
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
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean isConnectedToInternet(SetupStoreActivity setupStoreActivity) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) setupStoreActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (null != networkInfo &&
                (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(SetupStoreActivity.this)
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