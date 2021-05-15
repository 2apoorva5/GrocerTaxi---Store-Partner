package com.application.grocertaxistore;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.tapadoo.alerter.Alerter;

import java.util.HashMap;

import dmax.dialog.SpotsDialog;
import maes.tech.intentanim.CustomIntent;
import per.wsj.library.AndRatingBar;

public class StoreProfileActivity extends AppCompatActivity {

    private ImageView storeImage;
    private SwitchMaterial openCloseSwitch;
    private TextView storeName, textStoreStatus, ownerName, storeRating, storeAddress, storeEmail, storeMobile,
            storeTiming, storeMinimumOrderAmount;
    private AndRatingBar storeRatingBar;
    private ConstraintLayout viewRatings, manageOrdersBtn, manageProductsBtn, writeToUs, rateUs, inviteFriends, privacyPolicy,
            termsOfService, refundPolicy, appSettings, logout;
    private BottomNavigationView bottomBar;
    private FloatingActionButton addProductBtn;
    private CardView orderIndicator;

    private PreferenceManager preferenceManager;
    private Uri storePicUri = null;

    private FirebaseAuth firebaseAuth;
    private CollectionReference storeRef, storePendingOrdersRef;
    private StorageReference storageReference;

    private AlertDialog progressDialog1, progressDialog2;

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

        progressDialog1 = new SpotsDialog.Builder().setContext(StoreProfileActivity.this)
                .setMessage("Logging out...")
                .setCancelable(false)
                .setTheme(R.style.SpotsDialog)
                .build();

        progressDialog2 = new SpotsDialog.Builder().setContext(StoreProfileActivity.this)
                .setMessage("Hold on..")
                .setCancelable(false)
                .setTheme(R.style.SpotsDialog)
                .build();

        initViews();
        initFirebase();
        setActionOnViews();
    }

    private void initViews() {
        storeImage = findViewById(R.id.store_image);
        storeName = findViewById(R.id.store_name);
        ownerName = findViewById(R.id.store_owner);
        openCloseSwitch = findViewById(R.id.open_close_switch);
        textStoreStatus = findViewById(R.id.text_store_status);
        storeRating = findViewById(R.id.store_rating);
        storeRatingBar = findViewById(R.id.store_rating_bar);
        storeAddress = findViewById(R.id.store_address);
        storeEmail = findViewById(R.id.store_email);
        storeMobile = findViewById(R.id.store_mobile);
        storeTiming = findViewById(R.id.store_timing);
        storeMinimumOrderAmount = findViewById(R.id.minimum_order_amount);

        viewRatings = findViewById(R.id.view_ratings);
        manageOrdersBtn = findViewById(R.id.manage_orders);
        orderIndicator = findViewById(R.id.order_indicator);
        manageProductsBtn = findViewById(R.id.manage_products);

        writeToUs = findViewById(R.id.write_us);
        rateUs = findViewById(R.id.rate_us);
        inviteFriends = findViewById(R.id.invite_friend);
        privacyPolicy = findViewById(R.id.privacy_policy);
        termsOfService = findViewById(R.id.terms_of_service);
        refundPolicy = findViewById(R.id.refund_policy);
        appSettings = findViewById(R.id.app_settings);

        logout = findViewById(R.id.log_out);

        bottomBar = findViewById(R.id.bottom_bar);
        addProductBtn = findViewById(R.id.add_product_btn);
    }

    private void initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();

        storeRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_CITY)).collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_LOCALITY)).collection(Constants.KEY_COLLECTION_STORES);

        storageReference = FirebaseStorage.getInstance().getReference("StorePics/");

        storePendingOrdersRef = FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_CITY))
                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                .collection(Constants.KEY_COLLECTION_STORES)
                .document(preferenceManager.getString(Constants.KEY_STORE_ID))
                .collection(Constants.KEY_COLLECTION_PENDING_ORDERS);
    }

    private void setActionOnViews() {
        if (preferenceManager.getString(Constants.KEY_STORE_IMAGE).equals("") ||
                preferenceManager.getString(Constants.KEY_STORE_IMAGE).length() == 0 ||
                preferenceManager.getString(Constants.KEY_STORE_IMAGE).isEmpty() ||
                preferenceManager.getString(Constants.KEY_STORE_IMAGE) == null) {
            storeImage.setImageResource(R.drawable.img_store_placeholder);
        } else {
            Glide.with(StoreProfileActivity.this).load(preferenceManager.getString(Constants.KEY_STORE_IMAGE)).centerCrop().into(storeImage);
        }

        storeImage.setOnClickListener(v -> {
            if (!isConnectedToInternet(StoreProfileActivity.this)) {
                showConnectToInternetDialog();
                return;
            } else {
                selectImage();
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        storeName.setText(preferenceManager.getString(Constants.KEY_STORE_NAME));
        ownerName.setText(preferenceManager.getString(Constants.KEY_STORE_OWNER));

        ////////////////////////////////////////////////////////////////////////////////////////////

        openCloseSwitch.setChecked(preferenceManager.getBoolean(Constants.KEY_STORE_STATUS));

        if (preferenceManager.getBoolean(Constants.KEY_STORE_STATUS)) {
            textStoreStatus.setText("Store Opened");
            openCloseSwitch.setThumbTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchThumbOpened)));
            openCloseSwitch.setTrackTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchTrackOpened)));
        } else {
            textStoreStatus.setText("Store Closed");
            openCloseSwitch.setThumbTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchThumbClosed)));
            openCloseSwitch.setTrackTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchTrackClosed)));
        }

        openCloseSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isConnectedToInternet(StoreProfileActivity.this)) {
                showConnectToInternetDialog();
                return;
            } else {
                if (isChecked) {
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

        ////////////////////////////////////////////////////////////////////////////////////////////

        final DocumentReference storeDocumentRef = storeRef.document(preferenceManager.getString(Constants.KEY_STORE_ID));
        storeDocumentRef.addSnapshotListener((documentSnapshot, error) -> {
            if (error != null) {
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
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    double rating = documentSnapshot.getDouble(Constants.KEY_STORE_AVERAGE_RATING);

                    if(rating == 0) {
                        storeRating.setVisibility(View.GONE);
                        storeRatingBar.setVisibility(View.GONE);
                    } else {
                        storeRating.setVisibility(View.VISIBLE);
                        storeRatingBar.setVisibility(View.VISIBLE);
                        storeRating.setText(String.valueOf(rating));
                        storeRatingBar.setRating((float) rating);
                    }
                } else {
                    storeRating.setText(String.valueOf(0));
                    storeRatingBar.setRating(0);
                }
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        viewRatings.setOnClickListener(v -> {
            startActivity(new Intent(StoreProfileActivity.this, StoreReviewsActivity.class));
            CustomIntent.customType(StoreProfileActivity.this, "bottom-to-up");
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        storeAddress.setText(preferenceManager.getString(Constants.KEY_STORE_ADDRESS));
        storeEmail.setText(preferenceManager.getString(Constants.KEY_STORE_EMAIL));
        storeMobile.setText(preferenceManager.getString(Constants.KEY_STORE_MOBILE));

        storeTiming.setText(preferenceManager.getString(Constants.KEY_STORE_TIMING));
        storeMinimumOrderAmount.setText(String.format("₹ %s", preferenceManager.getString(Constants.KEY_STORE_MINIMUM_ORDER_VALUE)));

        ////////////////////////////////////////////////////////////////////////////////////////////

        storePendingOrdersRef.whereEqualTo(Constants.KEY_ORDER_STATUS, "Placed")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.size() == 0) {
                        orderIndicator.setVisibility(View.GONE);
                    } else {
                        orderIndicator.setVisibility(View.VISIBLE);
                        YoYo.with(Techniques.Pulse).duration(700).repeat(3).playOn(orderIndicator);
                    }
                }).addOnFailureListener(e -> {
            Alerter.create(StoreProfileActivity.this)
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
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        manageOrdersBtn.setOnClickListener(v -> {
            startActivity(new Intent(StoreProfileActivity.this, OrdersActivity.class));
            CustomIntent.customType(StoreProfileActivity.this, "left-to-right");
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        manageProductsBtn.setOnClickListener(v -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "");
            startActivity(new Intent(StoreProfileActivity.this, ProductsListActivity.class));
            CustomIntent.customType(StoreProfileActivity.this, "left-to-right");
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        writeToUs.setOnClickListener(v -> {
            Intent email = new Intent(Intent.ACTION_SENDTO);
            email.setData(Uri.parse("mailto:grocer.taxi@gmail.com"));
            startActivity(email);
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        rateUs.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=" + StoreProfileActivity.this.getPackageName())));
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=" + StoreProfileActivity.this.getPackageName())));
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        inviteFriends.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Grocer Taxi Store - Delivering Happiness!");
            String app_url = "https://play.google.com/store/apps/details?id=" + StoreProfileActivity.this.getPackageName();
            shareIntent.putExtra(Intent.EXTRA_TEXT, app_url);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        privacyPolicy.setOnClickListener(view -> {
            String privacyPolicyUrl = "https://grocertaxi.wixsite.com/privacy-policy";
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl));
            startActivity(browserIntent);
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        termsOfService.setOnClickListener(view -> {
            String privacyPolicyUrl = "https://grocertaxi.wixsite.com/terms-and-conditions";
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl));
            startActivity(browserIntent);
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        refundPolicy.setOnClickListener(view -> {
            String privacyPolicyUrl = "https://grocertaxi.wixsite.com/refund-policy";
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl));
            startActivity(browserIntent);
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        appSettings.setOnClickListener(v -> {
            Intent appInfoIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            appInfoIntent.addCategory(Intent.CATEGORY_DEFAULT);
            appInfoIntent.setData(Uri.parse("package:" + StoreProfileActivity.this.getPackageName()));
            startActivity(appInfoIntent);
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        logout.setOnClickListener(view -> {
            if (!isConnectedToInternet(StoreProfileActivity.this)) {
                showConnectToInternetDialog();
                return;
            } else {
                signOut();
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        bottomBar.setSelectedItemId(R.id.menu_profile);
        bottomBar.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_dashboard:
                    startActivity(new Intent(StoreProfileActivity.this, MainActivity.class));
                    CustomIntent.customType(StoreProfileActivity.this, "fadein-to-fadeout");
                    finish();
                    break;
                case R.id.menu_profile:
                    break;
            }
            return true;
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

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

            progressDialog2.show();

            if (storePicUri != null) {
                final StorageReference fileRef = storageReference.child(preferenceManager.getString(Constants.KEY_STORE_ID) + ".img");

                fileRef.putFile(storePicUri)
                        .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    final String imageValue = uri.toString();

                                    storeRef.document(preferenceManager.getString(Constants.KEY_STORE_ID))
                                            .update(Constants.KEY_STORE_IMAGE, imageValue)
                                            .addOnSuccessListener(aVoid -> {
                                                progressDialog2.dismiss();

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
                                                progressDialog2.dismiss();
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
                                    progressDialog2.dismiss();
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
                            progressDialog2.dismiss();
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
        if (!isConnectedToInternet(StoreProfileActivity.this)) {
            showConnectToInternetDialog();
            return;
        } else {
            MaterialDialog materialDialog = new MaterialDialog.Builder(StoreProfileActivity.this)
                    .setTitle("Log out of Grocer Taxi Store?")
                    .setMessage("Are you sure of logging out of Grocer Taxi Store?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", R.drawable.ic_dialog_okay, (dialogInterface, which) -> {
                        dialogInterface.dismiss();
                        progressDialog1.show();
                        DocumentReference documentReference = storeRef.document(preferenceManager.getString(Constants.KEY_STORE_ID));

                        HashMap<String, Object> updates = new HashMap<>();
                        updates.put(Constants.KEY_STORE_TOKEN, FieldValue.delete());
                        updates.put(Constants.KEY_STORE_STATUS, false);
                        documentReference.update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    progressDialog1.dismiss();
                                    firebaseAuth.signOut();
                                    preferenceManager.clearPreferences();
                                    Toast.makeText(StoreProfileActivity.this, "Logged Out!", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(getApplicationContext(), WelcomeActivity.class));
                                    CustomIntent.customType(StoreProfileActivity.this, "fadein-to-fadeout");
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    progressDialog1.dismiss();

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
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean isConnectedToInternet(StoreProfileActivity storeProfileActivity) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) storeProfileActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (null != networkInfo &&
                (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
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

        if (on) {
            layoutParams.flags |= bits;
        } else {
            layoutParams.flags &= ~bits;
        }
        window.setAttributes(layoutParams);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        CustomIntent.customType(StoreProfileActivity.this, "fadein-to-fadeout");
    }
}