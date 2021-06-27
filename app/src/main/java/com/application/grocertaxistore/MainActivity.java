package com.application.grocertaxistore;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.ColorStateList;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
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
import com.baoyz.widget.PullRefreshLayout;
import com.bumptech.glide.Glide;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.tapadoo.alerter.Alerter;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.HashMap;

import maes.tech.intentanim.CustomIntent;

public class MainActivity extends AppCompatActivity {

    private TextView today, storeName, storeAddress, textStoreStatus;
    private SwitchMaterial openCloseSwitch;
    private ImageView fruits, vegetables, foodGrains, dairy, bakery, beverages, dryFruits, meatBacon,
            noodlesPasta, snacks, kitchenOil, spices, sweets, babyCare, household, personalCare, petCare, stationary,
            hardware, medical, sports;
    private ConstraintLayout layoutContent, layoutNoInternet, retryBtn,
            storeProfileBtn, manageOrdersBtn, manageProductsBtn, cancelRequestsBtn, categoryFruits, categoryVegetables, categoryFoodGrains,
            categoryDairy, categoryBakery, categoryBeverages, categoryDryFruits, categoryMeatBacon, categoryNoodlesPasta,
            categorySnacks, categoryKitchenOil, categorySpices, categorySweets, categoryBabyCare, categoryHousehold,
            categoryPersonalCare, categoryPetCare, categoryStationary, categoryHardware, categoryMedical, categorySports;
    private BottomNavigationView bottomBar;
    private FloatingActionButton addProductBtn;
    private CardView orderIndicator, cancelRequestIndicator;
    private PullRefreshLayout pullRefreshLayout;

    private CollectionReference storeRef, storePendingOrdersRef, cancelRequestsRef;

    private PreferenceManager preferenceManager;

    private AppUpdateManager appUpdateManager;
    private static final int RC_APP_UPDATE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (!preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(MainActivity.this, "fadein-to-fadeout");
            finish();
        } else {
            if (preferenceManager.getString(Constants.KEY_STORE_ADDRESS).isEmpty() ||
                    preferenceManager.getString(Constants.KEY_STORE_ADDRESS).equals("") ||
                    preferenceManager.getString(Constants.KEY_STORE_ADDRESS).length() == 0 ||
                    preferenceManager.getString(Constants.KEY_STORE_ADDRESS) == null) {
                Intent intent = new Intent(getApplicationContext(), LocationPermissionActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                CustomIntent.customType(MainActivity.this, "bottom-to-up");
                finish();
            }
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorBackground));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        ////////////////////////////////////////////////////////////////////////////////////////////

        appUpdateManager = AppUpdateManagerFactory.create(this);
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(result -> {
            if (result.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && result.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                try {
                    appUpdateManager.startUpdateFlowForResult(result, AppUpdateType.IMMEDIATE, MainActivity.this, RC_APP_UPDATE);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        layoutContent = findViewById(R.id.layout_content);
        layoutNoInternet = findViewById(R.id.layout_no_internet);
        retryBtn = findViewById(R.id.retry_btn);

        pullRefreshLayout = findViewById(R.id.pull_refresh_layout);

        today = findViewById(R.id.today);
        storeName = findViewById(R.id.store_name);
        storeAddress = findViewById(R.id.store_address);
        storeProfileBtn = findViewById(R.id.store_profile_btn);
        textStoreStatus = findViewById(R.id.text_store_status);
        openCloseSwitch = findViewById(R.id.open_close_switch);

        manageOrdersBtn = findViewById(R.id.manage_orders_btn);
        orderIndicator = findViewById(R.id.order_indicator);
        manageProductsBtn = findViewById(R.id.manage_products_btn);
        cancelRequestsBtn = findViewById(R.id.cancellation_requests_btn);
        cancelRequestIndicator = findViewById(R.id.cancel_request_indicator);

        fruits = findViewById(R.id.category_fruits_img);
        vegetables = findViewById(R.id.category_vegetables_img);
        foodGrains = findViewById(R.id.category_foodgrains_img);
        dairy = findViewById(R.id.category_dairy_img);
        bakery = findViewById(R.id.category_bakery_img);
        beverages = findViewById(R.id.category_beverages_img);
        dryFruits = findViewById(R.id.category_dry_fruits_img);
        meatBacon = findViewById(R.id.category_meat_bacon_img);
        noodlesPasta = findViewById(R.id.category_noodles_pasta_img);
        snacks = findViewById(R.id.category_snacks_img);
        kitchenOil = findViewById(R.id.category_kitchen_oil_img);
        spices = findViewById(R.id.category_spices_img);
        sweets = findViewById(R.id.category_sweets_img);
        babyCare = findViewById(R.id.category_baby_care_img);
        household = findViewById(R.id.category_household_img);
        personalCare = findViewById(R.id.category_personal_care_img);
        petCare = findViewById(R.id.category_pet_care_img);
        stationary = findViewById(R.id.category_stationary_img);
        hardware = findViewById(R.id.category_hardware_img);
        medical = findViewById(R.id.category_medical_img);
        sports = findViewById(R.id.category_sports_img);

        categoryFruits = findViewById(R.id.category_fruits);
        categoryVegetables = findViewById(R.id.category_vegetables);
        categoryFoodGrains = findViewById(R.id.category_foodgrains);
        categoryDairy = findViewById(R.id.category_dairy);
        categoryBakery = findViewById(R.id.category_bakery);
        categoryBeverages = findViewById(R.id.category_beverages);
        categoryDryFruits = findViewById(R.id.category_dry_fruits);
        categoryMeatBacon = findViewById(R.id.category_meat_bacon);
        categoryNoodlesPasta = findViewById(R.id.category_noodles_pasta);
        categorySnacks = findViewById(R.id.category_snacks);
        categoryKitchenOil = findViewById(R.id.category_kitchen_oil);
        categorySpices = findViewById(R.id.category_spices);
        categorySweets = findViewById(R.id.category_sweets);
        categoryBabyCare = findViewById(R.id.category_baby_care);
        categoryHousehold = findViewById(R.id.category_household);
        categoryPersonalCare = findViewById(R.id.category_personal_care);
        categoryPetCare = findViewById(R.id.category_pet_care);
        categoryStationary = findViewById(R.id.category_stationary);
        categoryHardware = findViewById(R.id.category_hardware);
        categoryMedical = findViewById(R.id.category_medical);
        categorySports = findViewById(R.id.category_sports);

        bottomBar = findViewById(R.id.bottom_bar);
        addProductBtn = findViewById(R.id.add_product_btn);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == RC_APP_UPDATE) {
            if(resultCode != RESULT_OK) {
                Toast.makeText(MainActivity.this, "App Update Cancelled!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkNetworkConnection();
    }

    private void checkNetworkConnection() {
        if (!isConnectedToInternet(MainActivity.this)) {
            layoutContent.setVisibility(View.GONE);
            layoutNoInternet.setVisibility(View.VISIBLE);
            retryBtn.setOnClickListener(v -> checkNetworkConnection());
        } else {
            initFirebase();
            sendFCMTokenToDatabase();
            setActionOnViews();
        }
    }

    private void initFirebase() {
        storeRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_CITY)).collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_LOCALITY)).collection(Constants.KEY_COLLECTION_STORES);

        storePendingOrdersRef = FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_CITY))
                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                .collection(Constants.KEY_COLLECTION_STORES)
                .document(preferenceManager.getString(Constants.KEY_STORE_ID))
                .collection(Constants.KEY_COLLECTION_PENDING_ORDERS);

        cancelRequestsRef = FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_CITY))
                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                .collection(Constants.KEY_COLLECTION_STORES)
                .document(preferenceManager.getString(Constants.KEY_STORE_ID))
                .collection(Constants.KEY_COLLECTION_CANCELLATION_REQUESTS);
    }

    private void sendFCMTokenToDatabase() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String refreshToken = task.getResult();
                        HashMap<String, Object> token = new HashMap<>();
                        token.put(Constants.KEY_STORE_TOKEN, refreshToken);

                        DocumentReference documentReference = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_CITIES)
                                .document(preferenceManager.getString(Constants.KEY_CITY)).collection(Constants.KEY_COLLECTION_LOCALITIES)
                                .document(preferenceManager.getString(Constants.KEY_LOCALITY)).collection(Constants.KEY_COLLECTION_STORES)
                                .document(preferenceManager.getString(Constants.KEY_STORE_ID));
                        documentReference.set(token, SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                })
                                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Some ERROR occurred!", Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(MainActivity.this, "Some ERROR occurred!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setActionOnViews() {
        layoutNoInternet.setVisibility(View.GONE);
        layoutContent.setVisibility(View.VISIBLE);
        pullRefreshLayout.setRefreshing(false);

        ////////////////////////////////////////////////////////////////////////////////////////////

        pullRefreshLayout.setColor(getColor(R.color.colorAccent));
        pullRefreshLayout.setBackgroundColor(getColor(R.color.colorBackground));
        pullRefreshLayout.setOnRefreshListener(this::checkNetworkConnection);

        ////////////////////////////////////////////////////////////////////////////////////////////

        Calendar calendar = Calendar.getInstance();
        String currentDate = DateFormat.getDateInstance(DateFormat.FULL).format(calendar.getTime());
        today.setText(currentDate);

        ////////////////////////////////////////////////////////////////////////////////////////////

        storeName.setText(preferenceManager.getString(Constants.KEY_STORE_NAME));
        storeAddress.setText(preferenceManager.getString(Constants.KEY_STORE_ADDRESS));

        ////////////////////////////////////////////////////////////////////////////////////////////

        storeProfileBtn.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, StoreProfileActivity.class));
            CustomIntent.customType(MainActivity.this, "fadein-to-fadeout");
        });

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
            if (!isConnectedToInternet(MainActivity.this)) {
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
                                Alerter.create(MainActivity.this)
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
                                Alerter.create(MainActivity.this)
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
            Alerter.create(MainActivity.this)
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
            startActivity(new Intent(MainActivity.this, OrdersActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        manageProductsBtn.setOnClickListener(v -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        cancelRequestsRef.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.size() == 0) {
                        cancelRequestIndicator.setVisibility(View.GONE);
                    } else {
                        cancelRequestIndicator.setVisibility(View.VISIBLE);
                        YoYo.with(Techniques.Pulse).duration(700).repeat(3).playOn(cancelRequestIndicator);
                    }
                }).addOnFailureListener(e -> {
            Alerter.create(MainActivity.this)
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

        cancelRequestsBtn.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CancellationRequestsActivity.class));
            CustomIntent.customType(MainActivity.this, "bottom-to-up");
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        String cat_fruits = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Ffruits.png?alt=media&token=e682c6a7-16fe-47f1-b607-89b9b888b5d3";
        String cat_vegetables = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fvegetables.png?alt=media&token=5794ffa7-f88e-4477-9068-cd1d9ab4b247";
        String cat_foodgrains = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Ffoodgrains.png?alt=media&token=213bfe18-5525-4b3b-b1e0-83f55de8709e";
        String cat_dairy = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fdairy.png?alt=media&token=3623ac1a-8117-40c6-a13e-ec4be5e2518a";
        String cat_bakery = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fbakery.png?alt=media&token=786111f9-605c-4275-b0b5-901b6df68ec1";
        String cat_beverages = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fbeverages.png?alt=media&token=834a8b60-43df-4ccd-9e2f-559448c895d2";
        String cat_dryfruits = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fdry_fruits.png?alt=media&token=0e8afbf9-6cb5-42d0-ae74-e20b406b9113";
        String cat_meat = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fmeat_bacon.png?alt=media&token=b644e1af-2155-45ae-9d8f-0fc73c610997";
        String cat_noodles = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fnoodles_pasta.png?alt=media&token=e0c37743-9869-4fe9-a2f6-cd3e0ac908ad";
        String cat_snacks = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fsnacks.png?alt=media&token=e3b909e6-68bc-4dd9-bb77-092b7ec2ef7b";
        String cat_oil = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Foil.png?alt=media&token=2d22afd3-5978-4908-b8fe-8d06f9983664";
        String cat_spices = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fspices.png?alt=media&token=00b11823-bb04-4949-999f-f6860496e415";
        String cat_sweets = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fsweets.png?alt=media&token=2e4f6b55-ba63-4e90-8e86-0d3076b3e076";
        String cat_babycare = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fbaby_care.png?alt=media&token=f594f05b-92eb-433f-9cfa-fec19cf80923";
        String cat_household = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fcleaning_household.png?alt=media&token=d89ec809-bd96-42ee-920b-f8c25eefdf74";
        String cat_personalcare = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fpersonal_care.png?alt=media&token=3aa267d3-7764-47cb-ae68-f0694a4982ca";
        String cat_petcare = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fpet_care.png?alt=media&token=b5ce1d43-f34f-4008-88eb-5c81690fdc1f";
        String cat_stationary = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fstationary.png?alt=media&token=dac5dbd6-3235-42c2-9ec5-1615b4b63b44";
        String cat_hardware = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fhardware.png?alt=media&token=2a69540d-c6da-43b2-9358-564cb68a5431";
        String cat_medical = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fmedical.png?alt=media&token=83120872-249b-459a-ae93-8d29dad6bb98";
        String cat_sports = "https://firebasestorage.googleapis.com/v0/b/grocer-taxi.appspot.com/o/Categories%2Fsports.png?alt=media&token=cd124d9c-2421-4e93-8e6a-d6e13b4933de";

        Glide.with(MainActivity.this).load(cat_fruits).centerCrop().into(fruits);
        Glide.with(MainActivity.this).load(cat_vegetables).centerCrop().into(vegetables);
        Glide.with(MainActivity.this).load(cat_foodgrains).centerCrop().into(foodGrains);
        Glide.with(MainActivity.this).load(cat_dairy).centerCrop().into(dairy);
        Glide.with(MainActivity.this).load(cat_bakery).centerCrop().into(bakery);
        Glide.with(MainActivity.this).load(cat_beverages).centerCrop().into(beverages);
        Glide.with(MainActivity.this).load(cat_dryfruits).centerCrop().into(dryFruits);
        Glide.with(MainActivity.this).load(cat_meat).centerCrop().into(meatBacon);
        Glide.with(MainActivity.this).load(cat_noodles).centerCrop().into(noodlesPasta);
        Glide.with(MainActivity.this).load(cat_snacks).centerCrop().into(snacks);
        Glide.with(MainActivity.this).load(cat_oil).centerCrop().into(kitchenOil);
        Glide.with(MainActivity.this).load(cat_spices).centerCrop().into(spices);
        Glide.with(MainActivity.this).load(cat_sweets).centerCrop().into(sweets);
        Glide.with(MainActivity.this).load(cat_babycare).centerCrop().into(babyCare);
        Glide.with(MainActivity.this).load(cat_household).centerCrop().into(household);
        Glide.with(MainActivity.this).load(cat_personalcare).centerCrop().into(personalCare);
        Glide.with(MainActivity.this).load(cat_petcare).centerCrop().into(petCare);
        Glide.with(MainActivity.this).load(cat_stationary).centerCrop().into(stationary);
        Glide.with(MainActivity.this).load(cat_hardware).centerCrop().into(hardware);
        Glide.with(MainActivity.this).load(cat_medical).centerCrop().into(medical);
        Glide.with(MainActivity.this).load(cat_sports).centerCrop().into(sports);

        categoryFruits.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Fruits");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");

        });

        categoryVegetables.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Vegetables");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");

        });

        categoryFoodGrains.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Food Grains");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");

        });

        categoryDairy.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Dairy Items");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");

        });

        categoryBakery.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Bakery Items");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");

        });

        categoryBeverages.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Beverages");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");

        });

        categoryDryFruits.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Dry Fruits");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");

        });

        categoryMeatBacon.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Meat & Bacon");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");

        });

        categoryNoodlesPasta.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Noodles & Pasta");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");

        });

        categorySnacks.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Snacks");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");

        });

        categoryKitchenOil.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Kitchen Oil");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");

        });

        categorySpices.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Spices");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");

        });

        categorySweets.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Sweets");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");

        });

        categoryBabyCare.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Baby Care");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");

        });

        categoryHousehold.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Household");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");

        });

        categoryPersonalCare.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Personal Care");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");

        });

        categoryPetCare.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Pet Care");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");

        });

        categoryStationary.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Stationary");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");

        });

        categoryHardware.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Hardware");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");

        });

        categoryMedical.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Medical");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");

        });

        categorySports.setOnClickListener(view -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "Sports");
            startActivity(new Intent(MainActivity.this, ProductsListActivity.class));
            CustomIntent.customType(MainActivity.this, "left-to-right");

        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        bottomBar.setSelectedItemId(R.id.menu_dashboard);
        bottomBar.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_dashboard:
                    break;
                case R.id.menu_profile:
                    startActivity(new Intent(MainActivity.this, StoreProfileActivity.class));
                    CustomIntent.customType(MainActivity.this, "fadein-to-fadeout");
                    break;
            }
            return true;
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        addProductBtn.setOnClickListener(v -> {
            preferenceManager.putString(Constants.KEY_CATEGORY, "");
            startActivity(new Intent(MainActivity.this, AddProductActivity.class));
            CustomIntent.customType(MainActivity.this, "bottom-to-up");
        });
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean isConnectedToInternet(MainActivity mainActivity) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) mainActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (null != networkInfo &&
                (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(MainActivity.this)
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

    @Override
    protected void onResume() {
        super.onResume();
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(result -> {
            if (result.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                try {
                    appUpdateManager.startUpdateFlowForResult(result, AppUpdateType.IMMEDIATE, MainActivity.this, RC_APP_UPDATE);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}