package com.application.grocertaxistore;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.application.grocertaxistore.Utilities.Constants;
import com.application.grocertaxistore.Utilities.PreferenceManager;
import com.bumptech.glide.Glide;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.tapadoo.alerter.Alerter;

import java.util.HashMap;

import dmax.dialog.SpotsDialog;
import maes.tech.intentanim.CustomIntent;

public class StoreProfileActivity extends AppCompatActivity {

    private ImageView backBtn, storeImage, menuStoreDashboard, menuStoreProfile;
    private SwitchMaterial openCloseSwitch;
    private TextView storeName, textStoreStatus, ownerName, storeAddress, storeEmail, storeMobile,
            storeTiming, storeDeliveryCharges, storeRating;
    private RatingBar storeRatingBar;
    private ConstraintLayout logout;
    private FloatingActionButton addProductBtn;

    private PreferenceManager preferenceManager;
    private Uri storePicUri = null;

    private FirebaseAuth firebaseAuth;
    private CollectionReference storeRef;
    private StorageReference storageReference;

    private AlertDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_profile);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (!preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(StoreProfileActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(StoreProfileActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        progressDialog = new SpotsDialog.Builder().setContext(StoreProfileActivity.this)
                .setMessage("Logging out...")
                .setCancelable(false)
                .setTheme(R.style.SpotsDialog)
                .build();

        initViews();
        initFirebase();
        setActionOnViews();
    }

    private void initViews() {
        backBtn = findViewById(R.id.back_btn);
        openCloseSwitch = findViewById(R.id.open_close_switch);
        textStoreStatus = findViewById(R.id.text_store_status);
        storeImage = findViewById(R.id.store_image);
        storeName = findViewById(R.id.store_name);
        ownerName = findViewById(R.id.store_owner);
        storeAddress = findViewById(R.id.store_address);
        storeEmail = findViewById(R.id.store_email);
        storeMobile = findViewById(R.id.store_mobile);
        storeTiming = findViewById(R.id.store_timing);
        storeDeliveryCharges = findViewById(R.id.store_delivery_charges);
        storeRating = findViewById(R.id.store_rating);
        storeRatingBar = findViewById(R.id.store_rating_bar);
        logout = findViewById(R.id.log_out);
        menuStoreDashboard = findViewById(R.id.menu_store_dashboard);
        menuStoreProfile = findViewById(R.id.menu_store_profile);
        addProductBtn = findViewById(R.id.add_product_btn);
    }

    private void initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        storeRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_CITY)).collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_LOCALITY)).collection(Constants.KEY_COLLECTION_STORES);
        storageReference = FirebaseStorage.getInstance().getReference("StorePics/");
    }

    private void setActionOnViews() {
        backBtn.setOnClickListener(view -> onBackPressed());

        if(preferenceManager.getString(Constants.KEY_STORE_IMAGE).equals("") ||
                preferenceManager.getString(Constants.KEY_STORE_IMAGE).length() == 0 ||
                preferenceManager.getString(Constants.KEY_STORE_IMAGE).isEmpty() ||
                preferenceManager.getString(Constants.KEY_STORE_IMAGE) == null) {
            storeImage.setImageResource(R.drawable.img_store_placeholder);
        } else {
            Glide.with(StoreProfileActivity.this).load(preferenceManager.getString(Constants.KEY_STORE_IMAGE)).centerCrop().into(storeImage);
        }

        storeImage.setOnClickListener(v -> selectImage());

        storeName.setText(preferenceManager.getString(Constants.KEY_STORE_NAME));
        ownerName.setText(preferenceManager.getString(Constants.KEY_STORE_OWNER));

        openCloseSwitch.setChecked(preferenceManager.getBoolean(Constants.KEY_STORE_STATUS));

        if(preferenceManager.getBoolean(Constants.KEY_STORE_STATUS)) {
            textStoreStatus.setText("Store Opened");
            openCloseSwitch.setThumbTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchThumbOpened)));
            openCloseSwitch.setTrackTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchTrackOpened)));
        } else {
            textStoreStatus.setText("Store Closed");
            openCloseSwitch.setThumbTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchThumbClosed)));
            openCloseSwitch.setTrackTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchTrackClosed)));
        }

        openCloseSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(!isConnectedToInternet(StoreProfileActivity.this)) {
                showConnectToInternetDialog();
                return;
            } else {
                if(isChecked) {
                    openCloseSwitch.setThumbTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchThumbOpened)));
                    openCloseSwitch.setTrackTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchTrackOpened)));

                    storeRef.document(preferenceManager.getString(Constants.KEY_STORE_ID))
                            .update(Constants.KEY_STORE_STATUS, true)
                            .addOnSuccessListener(aVoid -> {
                                preferenceManager.putBoolean(Constants.KEY_STORE_STATUS, true);
                                textStoreStatus.setText("Store Opened");
                            })
                            .addOnFailureListener(e -> {
                                Alerter.create(StoreProfileActivity.this)
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
                    openCloseSwitch.setThumbTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchThumbClosed)));
                    openCloseSwitch.setTrackTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchTrackClosed)));

                    storeRef.document(preferenceManager.getString(Constants.KEY_STORE_ID))
                            .update(Constants.KEY_STORE_STATUS, false)
                            .addOnSuccessListener(aVoid -> {
                                preferenceManager.putBoolean(Constants.KEY_STORE_STATUS, false);
                                textStoreStatus.setText("Store Closed");
                            })
                            .addOnFailureListener(e -> {
                                Alerter.create(StoreProfileActivity.this)
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

        storeAddress.setText(preferenceManager.getString(Constants.KEY_STORE_ADDRESS));
        storeEmail.setText(preferenceManager.getString(Constants.KEY_STORE_EMAIL));
        storeMobile.setText(preferenceManager.getString(Constants.KEY_STORE_MOBILE));
        storeTiming.setText(preferenceManager.getString(Constants.KEY_STORE_TIMING));
        storeDeliveryCharges.setText(String.format("₹ %s", preferenceManager.getString(Constants.KEY_STORE_DELIVERY_CHARGES)));

        final DocumentReference storeDocumentRef = storeRef.document(preferenceManager.getString(Constants.KEY_STORE_ID));
        storeDocumentRef.addSnapshotListener((documentSnapshot, error) -> {
            if(error != null) {
                Alerter.create(StoreProfileActivity.this)
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
                if(documentSnapshot != null && documentSnapshot.exists()) {
                    double rating = documentSnapshot.getDouble(Constants.KEY_STORE_AVERAGE_RATING);

                    storeRating.setText(String.valueOf(rating));
                    storeRatingBar.setRating((float) rating);
                } else {
                    storeRating.setText(String.valueOf(0));
                    storeRatingBar.setRating(0);
                }
            }
        });

        logout.setOnClickListener(view -> {
            if (!isConnectedToInternet(StoreProfileActivity.this)) {
                showConnectToInternetDialog();
                return;
            } else {
                signOut();
            }
        });

        menuStoreDashboard.setOnClickListener(view -> {
            startActivity(new Intent(StoreProfileActivity.this, MainActivity.class));
            CustomIntent.customType(StoreProfileActivity.this, "fadein-to-fadeout");
            finish();
        });

        menuStoreProfile.setOnClickListener(view -> {
            return;
        });

        addProductBtn.setOnClickListener(v -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "");
            startActivity(new Intent(StoreProfileActivity.this, AddProductActivity.class));
            CustomIntent.customType(StoreProfileActivity.this, "bottom-to-up");
        });
    }

    private void selectImage() {
        ImagePicker.Companion.with(StoreProfileActivity.this)
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
            Glide.with(StoreProfileActivity.this).load(storePicUri).centerCrop().into(storeImage);

            if (storePicUri != null){
                final StorageReference fileRef = storageReference.child(preferenceManager.getString(Constants.KEY_STORE_ID) + ".img");

                fileRef.putFile(storePicUri)
                        .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    final String imageValue = uri.toString();

                                    storeRef.document(preferenceManager.getString(Constants.KEY_STORE_ID))
                                            .update(Constants.KEY_STORE_IMAGE, imageValue)
                                            .addOnSuccessListener(aVoid -> {
                                                preferenceManager.putString(Constants.KEY_STORE_IMAGE, imageValue);
                                                Alerter.create(StoreProfileActivity.this)
                                                        .setText("Success! Your store picture just got updated.")
                                                        .setTextAppearance(R.style.AlertText)
                                                        .setBackgroundColorRes(R.color.successColor)
                                                        .setIcon(R.drawable.ic_dialog_okay)
                                                        .setDuration(3000)
                                                        .enableIconPulse(true)
                                                        .enableVibration(true)
                                                        .disableOutsideTouch()
                                                        .enableProgress(true)
                                                        .setProgressColorInt(getColor(android.R.color.white))
                                                        .show();
                                                return;
                                            })
                                            .addOnFailureListener(e -> {
                                                Alerter.create(StoreProfileActivity.this)
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
                                })
                                .addOnFailureListener(e -> {
                                    Alerter.create(StoreProfileActivity.this)
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
                                }))
                        .addOnFailureListener(e -> {
                            Alerter.create(StoreProfileActivity.this)
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
        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Alerter.create(StoreProfileActivity.this)
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

    private void signOut() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(StoreProfileActivity.this)
                .setTitle("Log out of Grocer Taxi Store?")
                .setMessage("Are you sure of logging out of Grocer Taxi Store?")
                .setCancelable(false)
                .setPositiveButton("Yes", R.drawable.ic_dialog_okay, (dialogInterface, which) -> {
                    dialogInterface.dismiss();
                    progressDialog.show();
                    DocumentReference documentReference = storeRef.document(preferenceManager.getString(Constants.KEY_STORE_ID));

                    HashMap<String, Object> updates = new HashMap<>();
                    updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
                    updates.put(Constants.KEY_STORE_STATUS, false);
                    documentReference.update(updates)
                            .addOnSuccessListener(aVoid -> {
                                progressDialog.dismiss();
                                firebaseAuth.signOut();
                                preferenceManager.clearPreferences();
                                Toast.makeText(StoreProfileActivity.this, "Logged Out!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(getApplicationContext(), WelcomeActivity.class));
                                CustomIntent.customType(StoreProfileActivity.this, "fadein-to-fadeout");
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                progressDialog.dismiss();

                                Alerter.create(StoreProfileActivity.this)
                                        .setText("Whoa! Unable to log out. Try Again!")
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
                            });
                })
                .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss()).build();
        materialDialog.show();
    }

    private boolean isConnectedToInternet(StoreProfileActivity storeProfileActivity) {
        ConnectivityManager connectivityManager = (ConnectivityManager) storeProfileActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo wifiConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobileConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if ((wifiConn != null && wifiConn.isConnected()) || (mobileConn != null && mobileConn.isConnected())) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(StoreProfileActivity.this)
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

    public static void setWindowFlag(StoreProfileActivity storeProfileActivity, final int bits, boolean on) {
        Window window = storeProfileActivity.getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();

        if(on){
            layoutParams.flags |= bits;
        } else {
            layoutParams.flags &= ~bits;
        }
        window.setAttributes(layoutParams);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void finish() {
        super.finish();
        CustomIntent.customType(StoreProfileActivity.this, "fadein-to-fadeout");
    }
}