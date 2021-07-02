package com.application.grocertaxistore;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
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
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.makeramen.roundedimageview.RoundedImageView;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.tapadoo.alerter.Alerter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import java.util.HashMap;
import java.util.Random;

import maes.tech.intentanim.CustomIntent;

public class AddProductActivity extends AppCompatActivity {

    private ImageView closeBtn;
    private RoundedImageView productImage;
    private TextView productCategory, productType;
    private SwitchMaterial productTypeSwitch;
    private TextInputLayout productName, productUnit, productMRP, productPrice, productUnitsInStock,
            productDescription, productBrand, productMFGDate, productExpiryDate;
    private CardView addProductBtnContainer;
    private ConstraintLayout layoutContent, layoutNoInternet, retryBtn, addProductBtn;
    private ProgressBar progressBar;

    private StorageReference storageReference;

    private Uri productPicUri = null;
    private PreferenceManager preferenceManager;

    //For Select Category Dialog
    TextView fruits, vegetables, foodGrains, dairyItems, bakeryItems, beverages, dryFruits, meatBacon, noodlesPasta,
            snacks, kitchenOil, spices, sweets, babyCare, household, personalCare, petCare, stationary, hardware, medical, sports;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (!preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(AddProductActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(AddProductActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorBackground));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        ////////////////////////////////////////////////////////////////////////////////////////////

        layoutContent = findViewById(R.id.layout_content);
        layoutNoInternet = findViewById(R.id.layout_no_internet);
        retryBtn = findViewById(R.id.retry_btn);

        closeBtn = findViewById(R.id.close_btn);

        productCategory = findViewById(R.id.product_category);
        productImage = findViewById(R.id.product_image);
        productName = findViewById(R.id.product_name);
        productType = findViewById(R.id.product_type);
        productTypeSwitch = findViewById(R.id.product_type_switch);
        productUnit = findViewById(R.id.product_unit);
        productMRP = findViewById(R.id.product_mrp);
        productPrice = findViewById(R.id.product_price);
        productUnitsInStock = findViewById(R.id.product_unit_in_stock);
        productDescription = findViewById(R.id.product_description);
        productBrand = findViewById(R.id.product_brand);
        productMFGDate = findViewById(R.id.product_mfg_date);
        productExpiryDate = findViewById(R.id.product_expiry_date);

        addProductBtnContainer = findViewById(R.id.add_product_btn_container);
        addProductBtn = findViewById(R.id.add_product_btn);
        progressBar = findViewById(R.id.progress_bar);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkNetworkConnection();
    }

    private void checkNetworkConnection() {
        if (!isConnectedToInternet(AddProductActivity.this)) {
            layoutContent.setVisibility(View.GONE);
            layoutNoInternet.setVisibility(View.VISIBLE);
            retryBtn.setOnClickListener(v -> checkNetworkConnection());
        } else {
            initFirebase();
            setActionOnViews();
        }
    }

    private void initFirebase() {
        storageReference = FirebaseStorage.getInstance().getReference("ProductPics/");
    }

    private void setActionOnViews() {
        layoutNoInternet.setVisibility(View.GONE);
        layoutContent.setVisibility(View.VISIBLE);

        ////////////////////////////////////////////////////////////////////////////////////////////

        closeBtn.setOnClickListener(v -> {
            onBackPressed();
            finish();
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        KeyboardVisibilityEvent.setEventListener(AddProductActivity.this, isOpen -> {
            if (!isOpen) {
                productName.clearFocus();
                productUnit.clearFocus();
                productMRP.clearFocus();
                productPrice.clearFocus();
                productDescription.clearFocus();
                productBrand.clearFocus();
                productMFGDate.clearFocus();
                productExpiryDate.clearFocus();
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        if (!preferenceManager.getString(Constants.KEY_CATEGORY).isEmpty()) {
            productCategory.setText(preferenceManager.getString(Constants.KEY_CATEGORY));
        } else {
            productCategory.setText("");
        }
        productCategory.setOnClickListener(v -> showSelectCategoryDialog());

        ////////////////////////////////////////////////////////////////////////////////////////////

        productImage.setOnClickListener(v -> selectImage());

        ////////////////////////////////////////////////////////////////////////////////////////////

        productTypeSwitch.setChecked(true);
        productTypeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                productTypeSwitch.setThumbTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchThumbOpened)));
                productTypeSwitch.setTrackTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchTrackOpened)));
                productType.setText("Vegetarian");
            } else {
                productTypeSwitch.setThumbTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchThumbClosed)));
                productTypeSwitch.setTrackTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchTrackClosed)));
                productType.setText("Non-Vegetarian");
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        addProductBtn.setOnClickListener(v -> {
            UIUtil.hideKeyboard(AddProductActivity.this);

            if (!validateProductName() | !validateProductUnit() | !validateProductMRP() | !validateProductPrice() | !validateProductUnitsInStock()) {
                return;
            } else {
                if (productCategory.getText().toString().trim().isEmpty()) {
                    YoYo.with(Techniques.Shake).duration(700).repeat(0).playOn(productCategory);
                    Alerter.create(AddProductActivity.this)
                            .setText("Select a category for the product!")
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
                } else {
                    if (!isConnectedToInternet(AddProductActivity.this)) {
                        showConnectToInternetDialog();
                        return;
                    } else {
                        addProductBtnContainer.setVisibility(View.INVISIBLE);
                        addProductBtn.setEnabled(false);
                        progressBar.setVisibility(View.VISIBLE);

                        uploadProductDataToFirebase();
                    }
                }
            }
        });
    }

    private void showSelectCategoryDialog() {
        final AlertDialog alertDialog = new AlertDialog.Builder(AddProductActivity.this).create();

        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_select_category, null);

        fruits = view.findViewById(R.id.fruits);
        vegetables = view.findViewById(R.id.vegetables);
        foodGrains = view.findViewById(R.id.food_grains);
        dairyItems = view.findViewById(R.id.dairy_items);
        bakeryItems = view.findViewById(R.id.bakery_items);
        beverages = view.findViewById(R.id.beverages);
        dryFruits = view.findViewById(R.id.dry_fruits);
        meatBacon = view.findViewById(R.id.meat_bacon);
        noodlesPasta = view.findViewById(R.id.noodles_pasta);
        snacks = view.findViewById(R.id.snacks);
        kitchenOil = view.findViewById(R.id.kitchen_oil);
        spices = view.findViewById(R.id.spices);
        sweets = view.findViewById(R.id.sweets);
        babyCare = view.findViewById(R.id.baby_care);
        household = view.findViewById(R.id.household);
        personalCare = view.findViewById(R.id.personal_care);
        petCare = view.findViewById(R.id.pet_care);
        stationary = view.findViewById(R.id.stationary);
        hardware = view.findViewById(R.id.hardware);
        medical = view.findViewById(R.id.medical);
        sports = view.findViewById(R.id.sports);

        alertDialog.setView(view);

        fruits.setOnClickListener(view1 -> {
            alertDialog.dismiss();
            productCategory.setText(fruits.getText().toString().trim());
            preferenceManager.putString(Constants.KEY_CATEGORY, fruits.getText().toString().trim());
        });

        vegetables.setOnClickListener(view1 -> {
            alertDialog.dismiss();
            productCategory.setText(vegetables.getText().toString().trim());
            preferenceManager.putString(Constants.KEY_CATEGORY, vegetables.getText().toString().trim());
        });

        foodGrains.setOnClickListener(view1 -> {
            alertDialog.dismiss();
            productCategory.setText(foodGrains.getText().toString().trim());
            preferenceManager.putString(Constants.KEY_CATEGORY, foodGrains.getText().toString().trim());
        });

        dairyItems.setOnClickListener(view1 -> {
            alertDialog.dismiss();
            productCategory.setText(dairyItems.getText().toString().trim());
            preferenceManager.putString(Constants.KEY_CATEGORY, dairyItems.getText().toString().trim());
        });

        bakeryItems.setOnClickListener(view1 -> {
            alertDialog.dismiss();
            productCategory.setText(bakeryItems.getText().toString().trim());
            preferenceManager.putString(Constants.KEY_CATEGORY, bakeryItems.getText().toString().trim());
        });

        beverages.setOnClickListener(view1 -> {
            alertDialog.dismiss();
            productCategory.setText(beverages.getText().toString().trim());
            preferenceManager.putString(Constants.KEY_CATEGORY, beverages.getText().toString().trim());
        });

        dryFruits.setOnClickListener(view1 -> {
            alertDialog.dismiss();
            productCategory.setText(dryFruits.getText().toString().trim());
            preferenceManager.putString(Constants.KEY_CATEGORY, dryFruits.getText().toString().trim());
        });

        meatBacon.setOnClickListener(view1 -> {
            alertDialog.dismiss();
            productCategory.setText(meatBacon.getText().toString().trim());
            preferenceManager.putString(Constants.KEY_CATEGORY, meatBacon.getText().toString().trim());
        });

        noodlesPasta.setOnClickListener(view1 -> {
            alertDialog.dismiss();
            productCategory.setText(noodlesPasta.getText().toString().trim());
            preferenceManager.putString(Constants.KEY_CATEGORY, noodlesPasta.getText().toString().trim());
        });

        snacks.setOnClickListener(view1 -> {
            alertDialog.dismiss();
            productCategory.setText(snacks.getText().toString().trim());
            preferenceManager.putString(Constants.KEY_CATEGORY, snacks.getText().toString().trim());
        });

        kitchenOil.setOnClickListener(view1 -> {
            alertDialog.dismiss();
            productCategory.setText(kitchenOil.getText().toString().trim());
            preferenceManager.putString(Constants.KEY_CATEGORY, kitchenOil.getText().toString().trim());
        });

        spices.setOnClickListener(view1 -> {
            alertDialog.dismiss();
            productCategory.setText(spices.getText().toString().trim());
            preferenceManager.putString(Constants.KEY_CATEGORY, spices.getText().toString().trim());
        });

        sweets.setOnClickListener(view1 -> {
            alertDialog.dismiss();
            productCategory.setText(sweets.getText().toString().trim());
            preferenceManager.putString(Constants.KEY_CATEGORY, sweets.getText().toString().trim());
        });

        babyCare.setOnClickListener(view1 -> {
            alertDialog.dismiss();
            productCategory.setText(babyCare.getText().toString().trim());
            preferenceManager.putString(Constants.KEY_CATEGORY, babyCare.getText().toString().trim());
        });

        household.setOnClickListener(view1 -> {
            alertDialog.dismiss();
            productCategory.setText(household.getText().toString().trim());
            preferenceManager.putString(Constants.KEY_CATEGORY, household.getText().toString().trim());
        });

        personalCare.setOnClickListener(view1 -> {
            alertDialog.dismiss();
            productCategory.setText(personalCare.getText().toString().trim());
            preferenceManager.putString(Constants.KEY_CATEGORY, personalCare.getText().toString().trim());
        });

        petCare.setOnClickListener(view1 -> {
            alertDialog.dismiss();
            productCategory.setText(petCare.getText().toString().trim());
            preferenceManager.putString(Constants.KEY_CATEGORY, petCare.getText().toString().trim());
        });

        stationary.setOnClickListener(view1 -> {
            alertDialog.dismiss();
            productCategory.setText(stationary.getText().toString().trim());
            preferenceManager.putString(Constants.KEY_CATEGORY, stationary.getText().toString().trim());
        });

        hardware.setOnClickListener(view1 -> {
            alertDialog.dismiss();
            productCategory.setText(hardware.getText().toString().trim());
            preferenceManager.putString(Constants.KEY_CATEGORY, hardware.getText().toString().trim());
        });

        medical.setOnClickListener(view1 -> {
            alertDialog.dismiss();
            productCategory.setText(medical.getText().toString().trim());
            preferenceManager.putString(Constants.KEY_CATEGORY, medical.getText().toString().trim());
        });

        sports.setOnClickListener(view1 -> {
            alertDialog.dismiss();
            productCategory.setText(sports.getText().toString().trim());
            preferenceManager.putString(Constants.KEY_CATEGORY, sports.getText().toString().trim());
        });

        alertDialog.show();
    }

    private void selectImage() {
        ImagePicker.Companion.with(AddProductActivity.this)
                .crop()
                .compress(1024)
                .maxResultSize(1080, 1080)
                .start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            productPicUri = data.getData();
            Glide.with(AddProductActivity.this).load(productPicUri).centerCrop().into(productImage);
        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Alerter.create(AddProductActivity.this)
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
            return;
        } else {
            return;
        }
    }

    private boolean validateProductName() {
        String productNameValue = productName.getEditText().getText().toString().trim();

        if (productNameValue.isEmpty()) {
            productName.setError("Enter product's name here!");
            productName.requestFocus();
            return false;
        } else {
            productName.setError(null);
            productName.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validateProductUnit() {
        String productUnitValue = productUnit.getEditText().getText().toString().trim();

        if (productUnitValue.isEmpty()) {
            productUnit.setError("Enter product's unit quantity!");
            productUnit.requestFocus();
            return false;
        } else {
            productUnit.setError(null);
            productUnit.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validateProductMRP() {
        String productMRPValue = productMRP.getEditText().getText().toString().trim();

        if (productMRPValue.isEmpty()) {
            productMRP.setError("Enter product's MRP!");
            productMRP.requestFocus();
            return false;
        } else {
            productMRP.setError(null);
            productMRP.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validateProductPrice() {
        String productPriceValue = productPrice.getEditText().getText().toString().trim();

        if (productPriceValue.isEmpty()) {
            productPrice.setError("Enter product's retail price!");
            productPrice.requestFocus();
            return false;
        } else {
            productPrice.setError(null);
            productPrice.setErrorEnabled(false);
            return true;
        }
    }

    private boolean validateProductUnitsInStock() {
        String productUnitsInStockValue = productUnitsInStock.getEditText().getText().toString().trim();

        if (productUnitsInStockValue.isEmpty()) {
            productUnitsInStock.setError("How many units you have in the stock?");
            productUnitsInStock.requestFocus();
            return false;
        } else if (productUnitsInStockValue.equals("0")) {
            productUnitsInStock.setError("Can't have 0 here!");
            productUnitsInStock.requestFocus();
            return false;
        } else {
            productUnitsInStock.setError(null);
            productUnitsInStock.setErrorEnabled(false);
            return true;
        }
    }

    private void uploadProductDataToFirebase() {
        final String product_store_id = preferenceManager.getString(Constants.KEY_STORE_ID);
        final String product_store_name = preferenceManager.getString(Constants.KEY_STORE_NAME);
        final String product_category = productCategory.getText().toString().trim();
        final String product_name = productName.getEditText().getText().toString().trim();
        final boolean product_is_veg = productTypeSwitch.isChecked();
        final String product_unit = productUnit.getEditText().getText().toString().trim();
        final String product_mrp = productMRP.getEditText().getText().toString().trim();
        final String product_price = productPrice.getEditText().getText().toString().trim();
        final String product_units_in_stock = productUnitsInStock.getEditText().getText().toString().trim();
        final String product_desc = productDescription.getEditText().getText().toString().trim();
        final String product_brand = productBrand.getEditText().getText().toString().trim();
        final String product_mfg_date = productMFGDate.getEditText().getText().toString().trim();
        final String product_expiry_date = productExpiryDate.getEditText().getText().toString().trim();

        float offer = (float) (((Double.parseDouble(product_mrp) - Double.parseDouble(product_price)) / Double.parseDouble(product_mrp)) * 100);
        int product_offer = (int) offer;

        Random random = new Random();
        int number1 = random.nextInt(9000) + 1000;
        int number2 = random.nextInt(9000) + 1000;

        final String product_id = String.format("PRODUCT-%d%d", number1, number2);

        if (productPicUri != null) {
            final StorageReference fileRef = storageReference.child(product_id + ".img");

            fileRef.putFile(productPicUri)
                    .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                final String product_image = uri.toString();

                                HashMap<String, Object> newCategory = new HashMap<>();
                                newCategory.put(Constants.KEY_CATEGORY_NAME, product_category);

                                HashMap<String, Object> newProduct = new HashMap<>();
                                newProduct.put(Constants.KEY_PRODUCT_ID, product_id);
                                newProduct.put(Constants.KEY_PRODUCT_TIMESTAMP, FieldValue.serverTimestamp());
                                newProduct.put(Constants.KEY_PRODUCT_STORE_ID, product_store_id);
                                newProduct.put(Constants.KEY_PRODUCT_STORE_NAME, product_store_name);
                                newProduct.put(Constants.KEY_PRODUCT_CATEGORY, product_category);
                                newProduct.put(Constants.KEY_PRODUCT_IN_STOCK, true);
                                newProduct.put(Constants.KEY_PRODUCT_IMAGE, product_image);
                                newProduct.put(Constants.KEY_PRODUCT_NAME, product_name);
                                newProduct.put(Constants.KEY_PRODUCT_IS_VEG, product_is_veg);
                                newProduct.put(Constants.KEY_PRODUCT_UNIT, product_unit);
                                newProduct.put(Constants.KEY_PRODUCT_MRP, Double.parseDouble(product_mrp));
                                newProduct.put(Constants.KEY_PRODUCT_RETAIL_PRICE, Double.parseDouble(product_price));
                                newProduct.put(Constants.KEY_PRODUCT_OFFER, product_offer);
                                newProduct.put(Constants.KEY_PRODUCT_UNITS_IN_STOCK, Integer.parseInt(product_units_in_stock));
                                newProduct.put(Constants.KEY_PRODUCT_DESCRIPTION, product_desc);
                                newProduct.put(Constants.KEY_PRODUCT_BRAND, product_brand);
                                newProduct.put(Constants.KEY_PRODUCT_MFG_DATE, product_mfg_date);
                                newProduct.put(Constants.KEY_PRODUCT_EXPIRY_TIME, product_expiry_date);
                                newProduct.put(Constants.KEY_PRODUCT_SEARCH_KEYWORD, product_name.toLowerCase());

                                FirebaseFirestore.getInstance()
                                        .collection(Constants.KEY_COLLECTION_CITIES)
                                        .document(preferenceManager.getString(Constants.KEY_CITY))
                                        .collection(Constants.KEY_COLLECTION_LOCALITIES)
                                        .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                                        .collection(Constants.KEY_COLLECTION_STORES)
                                        .document(preferenceManager.getString(Constants.KEY_STORE_ID))
                                        .collection(Constants.KEY_COLLECTION_CATEGORIES)
                                        .document(product_category)
                                        .set(newCategory)
                                        .addOnSuccessListener(void1 -> FirebaseFirestore.getInstance()
                                                .collection(Constants.KEY_COLLECTION_CITIES)
                                                .document(preferenceManager.getString(Constants.KEY_CITY))
                                                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                                                .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                                                .collection(Constants.KEY_COLLECTION_CATEGORIES)
                                                .document(product_category)
                                                .set(newCategory)
                                                .addOnSuccessListener(void2 -> FirebaseFirestore.getInstance()
                                                        .collection(Constants.KEY_COLLECTION_CITIES)
                                                        .document(preferenceManager.getString(Constants.KEY_CITY))
                                                        .collection(Constants.KEY_COLLECTION_LOCALITIES)
                                                        .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                                                        .collection(Constants.KEY_COLLECTION_STORES)
                                                        .document(preferenceManager.getString(Constants.KEY_STORE_ID))
                                                        .collection(Constants.KEY_COLLECTION_CATEGORIES)
                                                        .document(product_category)
                                                        .collection(Constants.KEY_COLLECTION_PRODUCTS)
                                                        .document(product_id)
                                                        .set(newProduct)
                                                        .addOnSuccessListener(aVoid -> FirebaseFirestore.getInstance()
                                                                .collection(Constants.KEY_COLLECTION_CITIES)
                                                                .document(preferenceManager.getString(Constants.KEY_CITY))
                                                                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                                                                .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                                                                .collection(Constants.KEY_COLLECTION_STORES)
                                                                .document(preferenceManager.getString(Constants.KEY_STORE_ID))
                                                                .collection(Constants.KEY_COLLECTION_PRODUCTS)
                                                                .document(product_id)
                                                                .set(newProduct)
                                                                .addOnSuccessListener(aVoid1 -> FirebaseFirestore.getInstance()
                                                                        .collection(Constants.KEY_COLLECTION_CITIES)
                                                                        .document(preferenceManager.getString(Constants.KEY_CITY))
                                                                        .collection(Constants.KEY_COLLECTION_LOCALITIES)
                                                                        .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                                                                        .collection(Constants.KEY_COLLECTION_CATEGORIES)
                                                                        .document(product_category)
                                                                        .collection(Constants.KEY_COLLECTION_PRODUCTS)
                                                                        .document(product_id)
                                                                        .set(newProduct)
                                                                        .addOnSuccessListener(aVoid11 -> FirebaseFirestore.getInstance()
                                                                                .collection(Constants.KEY_COLLECTION_CITIES)
                                                                                .document(preferenceManager.getString(Constants.KEY_CITY))
                                                                                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                                                                                .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                                                                                .collection(Constants.KEY_COLLECTION_PRODUCTS)
                                                                                .document(product_id)
                                                                                .set(newProduct)
                                                                                .addOnSuccessListener(aVoid111 -> {
                                                                                    progressBar.setVisibility(View.GONE);
                                                                                    addProductBtnContainer.setVisibility(View.VISIBLE);
                                                                                    addProductBtn.setEnabled(true);

                                                                                    onBackPressed();
                                                                                    finish();
                                                                                }).addOnFailureListener(e -> {
                                                                                    progressBar.setVisibility(View.GONE);
                                                                                    addProductBtnContainer.setVisibility(View.VISIBLE);
                                                                                    addProductBtn.setEnabled(true);

                                                                                    Alerter.create(AddProductActivity.this)
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
                                                                            addProductBtnContainer.setVisibility(View.VISIBLE);
                                                                            addProductBtn.setEnabled(true);

                                                                            Alerter.create(AddProductActivity.this)
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
                                                                    addProductBtnContainer.setVisibility(View.VISIBLE);
                                                                    addProductBtn.setEnabled(true);

                                                                    Alerter.create(AddProductActivity.this)
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
                                                            addProductBtnContainer.setVisibility(View.VISIBLE);
                                                            addProductBtn.setEnabled(true);

                                                            Alerter.create(AddProductActivity.this)
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
                                                    addProductBtnContainer.setVisibility(View.VISIBLE);
                                                    addProductBtn.setEnabled(true);

                                                    Alerter.create(AddProductActivity.this)
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
                                    addProductBtnContainer.setVisibility(View.VISIBLE);
                                    addProductBtn.setEnabled(true);

                                    Alerter.create(AddProductActivity.this)
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
                                addProductBtnContainer.setVisibility(View.VISIBLE);
                                addProductBtn.setEnabled(true);

                                Alerter.create(AddProductActivity.this)
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
                addProductBtnContainer.setVisibility(View.VISIBLE);
                addProductBtn.setEnabled(true);

                Alerter.create(AddProductActivity.this)
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
            addProductBtnContainer.setVisibility(View.VISIBLE);
            addProductBtn.setEnabled(true);

            YoYo.with(Techniques.Shake).duration(700).repeat(0).playOn(productImage);
            Alerter.create(AddProductActivity.this)
                    .setText("Add an image for the product!")
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

    private boolean isConnectedToInternet(AddProductActivity addProductActivity) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) addProductActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (null != networkInfo &&
                (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(AddProductActivity.this)
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
        CustomIntent.customType(AddProductActivity.this, "up-to-bottom");
    }
}