package com.application.grocertaxistore;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.application.grocertaxistore.Utilities.Constants;
import com.application.grocertaxistore.Utilities.PreferenceManager;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.skyfishjy.library.RippleBackground;
import com.tapadoo.alerter.Alerter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import java.io.IOException;
import java.util.List;

import maes.tech.intentanim.CustomIntent;

public class StoreAddressActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnCameraIdleListener {

    private ImageView backBtn, pin, userCurrentLocationBtn;
    private TextView location;
    private CardView info;
    private RippleBackground rippleEffect;
    private ConstraintLayout confirmBtn;

    private GoogleMap googleMap;
    private Location currentLocation;
    private SupportMapFragment supportMapFragment;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private CollectionReference storeRef;

    private LocationCallback locationCallback;
    private final float DEFAULT_ZOOM = 17.5f;

    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_address);

        preferenceManager = new PreferenceManager(StoreAddressActivity.this);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            initViews();
            initFirebase();
            setActionOnViews();

            supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.google_map);
            supportMapFragment.getMapAsync(this);

            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        } else {
            startActivity(new Intent(getApplicationContext(), LocationPermissionActivity.class));
            CustomIntent.customType(StoreAddressActivity.this, "up-to-bottom");
            finish();
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        googleMap.setMyLocationEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.getUiSettings().setMapToolbarEnabled(false);

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);

        SettingsClient settingsClient = LocationServices.getSettingsClient(StoreAddressActivity.this);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());

        task.addOnSuccessListener(StoreAddressActivity.this, locationSettingsResponse -> getCurrentLocation());

        task.addOnFailureListener(StoreAddressActivity.this, e -> {
            if (e instanceof ResolvableApiException) {
                ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                try {
                    resolvableApiException.startResolutionForResult(StoreAddressActivity.this, 2021);
                } catch (IntentSender.SendIntentException sendIntentException) {
                    sendIntentException.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2021) {
            if (resultCode == RESULT_OK) {
                getCurrentLocation();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        fusedLocationProviderClient.getLastLocation()
                .addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            currentLocation = task.getResult();
                            if (currentLocation != null) {
                                LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, DEFAULT_ZOOM));

                                pin.setVisibility(View.VISIBLE);
                                info.setVisibility(View.VISIBLE);
                                rippleEffect.startRippleAnimation();

                                googleMap.setOnCameraIdleListener(StoreAddressActivity.this);
                            } else {
                                LocationRequest locationRequest = LocationRequest.create();
                                locationRequest.setInterval(10000);
                                locationRequest.setFastestInterval(5000);
                                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

                                locationCallback = new LocationCallback() {
                                    @Override
                                    public void onLocationResult(LocationResult locationResult) {
                                        super.onLocationResult(locationResult);

                                        if (locationResult == null) {
                                            return;
                                        }
                                        currentLocation = locationResult.getLastLocation();
                                        LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, DEFAULT_ZOOM));

                                        pin.setVisibility(View.VISIBLE);
                                        info.setVisibility(View.VISIBLE);
                                        rippleEffect.startRippleAnimation();

                                        googleMap.setOnCameraIdleListener(StoreAddressActivity.this);
                                        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
                                    }
                                };

                                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
                            }
                        } else {
                            Alerter.create(StoreAddressActivity.this)
                                    .setText("Unable to get location!")
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
                        }
                    }
                });
    }

    @Override
    public void onCameraIdle() {
        LatLng latLng = googleMap.getCameraPosition().target;
        Geocoder geocoder = new Geocoder(StoreAddressActivity.this);

        try {
            List<Address> addressList = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);

            if (addressList != null && addressList.size() > 0) {
                preferenceManager.putString(Constants.SUBLOCALITY, addressList.get(0).getSubLocality());
                preferenceManager.putString(Constants.LOCALITY, addressList.get(0).getLocality());
                preferenceManager.putString(Constants.COUNTRY, addressList.get(0).getCountryName());
                preferenceManager.putString(Constants.PINCODE, addressList.get(0).getPostalCode());
                preferenceManager.putString(Constants.LATITUDE, String.valueOf(addressList.get(0).getLatitude()));
                preferenceManager.putString(Constants.LONGITUDE, String.valueOf(addressList.get(0).getLongitude()));

                location.setText(String.format("%s, %s", preferenceManager.getString(Constants.SUBLOCALITY), preferenceManager.getString(Constants.LOCALITY)));
            }
        } catch (IOException e) {
            Alerter.create(StoreAddressActivity.this)
                    .setText(e.getMessage())
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
        }
    }

    private void initViews() {
        backBtn = findViewById(R.id.back_btn);
        pin = findViewById(R.id.pin);
        info = findViewById(R.id.info);
        rippleEffect = findViewById(R.id.ripple_effect);
        userCurrentLocationBtn = findViewById(R.id.use_current_location_btn);
        location = findViewById(R.id.location);
        confirmBtn = findViewById(R.id.confirm_btn);
    }

    private void initFirebase() {
        storeRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_CITY)).collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_LOCALITY)).collection(Constants.KEY_COLLECTION_STORES);
    }

    private void setActionOnViews() {
        backBtn.setOnClickListener(v -> {
            onBackPressed();
            finish();
        });

        pin.setVisibility(View.GONE);
        info.setVisibility(View.GONE);

        userCurrentLocationBtn.setOnClickListener(v -> getCurrentLocation());

        confirmBtn.setOnClickListener(v -> {
            if (!isConnectedToInternet(StoreAddressActivity.this)) {
                showConnectToInternetDialog();
                return;
            } else {
                BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(StoreAddressActivity.this);
                bottomSheetDialog.setContentView(R.layout.bottom_sheet_address);
                bottomSheetDialog.setCanceledOnTouchOutside(false);

                ImageView closeSheetBtn = bottomSheetDialog.findViewById(R.id.close_bottom_sheet_btn);
                closeSheetBtn.setOnClickListener(v12 -> bottomSheetDialog.dismiss());

                TextView location = bottomSheetDialog.findViewById(R.id.location);
                location.setText(String.format("%s, %s", preferenceManager.getString(Constants.SUBLOCALITY), preferenceManager.getString(Constants.LOCALITY)));

                TextInputLayout address = bottomSheetDialog.findViewById(R.id.address);
                TextInputLayout landmark = bottomSheetDialog.findViewById(R.id.landmark);

                CardView saveBtnContainer = bottomSheetDialog.findViewById(R.id.save_btn_container);
                ConstraintLayout saveBtn = bottomSheetDialog.findViewById(R.id.save_btn);
                ProgressBar progressBar = bottomSheetDialog.findViewById(R.id.progress_bar);

                KeyboardVisibilityEvent.setEventListener(StoreAddressActivity.this, isOpen -> {
                    if (!isOpen) {
                        address.clearFocus();
                        landmark.clearFocus();
                    }
                });

                saveBtn.setOnClickListener(v1 -> {
                    UIUtil.hideKeyboard(StoreAddressActivity.this);

                    final String address_value = address.getEditText().getText().toString().trim();
                    final String landmark_value = landmark.getEditText().getText().toString().trim();

                    if (address_value.isEmpty()) {
                        YoYo.with(Techniques.Shake).duration(700).repeat(1).playOn(address);
                    } else {
                        String userLocation = preferenceManager.getString(Constants.SUBLOCALITY) + ", " + preferenceManager.getString(Constants.LOCALITY);
                        String deliveryAddress = address_value + ", " + landmark_value + ", " +
                                preferenceManager.getString(Constants.SUBLOCALITY) + ", " +
                                preferenceManager.getString(Constants.LOCALITY) + ", " +
                                preferenceManager.getString(Constants.COUNTRY) + " - " +
                                preferenceManager.getString(Constants.PINCODE);

                        if (!isConnectedToInternet(StoreAddressActivity.this)) {
                            showConnectToInternetDialog();
                            return;
                        } else {
                            saveBtnContainer.setVisibility(View.INVISIBLE);
                            saveBtn.setEnabled(false);
                            progressBar.setVisibility(View.VISIBLE);

                            storeRef.document(preferenceManager.getString(Constants.KEY_STORE_ID))
                                    .update(Constants.KEY_STORE_LOCATION, userLocation,
                                            Constants.KEY_STORE_ADDRESS, deliveryAddress,
                                            Constants.KEY_STORE_LATITUDE, Double.parseDouble(preferenceManager.getString(Constants.LATITUDE)),
                                            Constants.KEY_STORE_LONGITUDE, Double.parseDouble(preferenceManager.getString(Constants.LONGITUDE)))
                                    .addOnSuccessListener(aVoid -> {
                                        progressBar.setVisibility(View.GONE);
                                        saveBtnContainer.setVisibility(View.VISIBLE);
                                        saveBtn.setEnabled(true);

                                        preferenceManager.putString(Constants.KEY_STORE_LOCATION, userLocation);
                                        preferenceManager.putString(Constants.KEY_STORE_ADDRESS, deliveryAddress);
                                        preferenceManager.putString(Constants.KEY_STORE_LATITUDE, preferenceManager.getString(Constants.LATITUDE));
                                        preferenceManager.putString(Constants.KEY_STORE_LONGITUDE, preferenceManager.getString(Constants.LONGITUDE));

                                        bottomSheetDialog.dismiss();

                                        preferenceManager.putString(Constants.SUBLOCALITY, "");
                                        preferenceManager.putString(Constants.LOCALITY, "");
                                        preferenceManager.putString(Constants.COUNTRY, "");
                                        preferenceManager.putString(Constants.PINCODE, "");
                                        preferenceManager.putString(Constants.LATITUDE, "");
                                        preferenceManager.putString(Constants.LONGITUDE, "");

                                        startActivity(new Intent(StoreAddressActivity.this, MainActivity.class));
                                        CustomIntent.customType(StoreAddressActivity.this, "fadein-to-fadeout");
                                        finish();
                                    }).addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                saveBtnContainer.setVisibility(View.VISIBLE);
                                saveBtn.setEnabled(true);
                                Toast.makeText(StoreAddressActivity.this, "Whoa! Something broke. Try again!", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                });

                bottomSheetDialog.show();
            }
        });
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean isConnectedToInternet(StoreAddressActivity storeAddressActivity) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) storeAddressActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (null != networkInfo &&
                (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(StoreAddressActivity.this)
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

    public static void setWindowFlag(StoreAddressActivity storeAddressActivity, final int bits, boolean on) {
        Window window = storeAddressActivity.getWindow();
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
        KeyboardVisibilityEvent.setEventListener(StoreAddressActivity.this, isOpen -> {
            if (isOpen) {
                UIUtil.hideKeyboard(StoreAddressActivity.this);
            }
        });
        CustomIntent.customType(StoreAddressActivity.this, "up-to-bottom");
    }
}