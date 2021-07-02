package com.application.grocertaxistore;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.bumptech.glide.Glide;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.makeramen.roundedimageview.RoundedImageView;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.tapadoo.alerter.Alerter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import java.util.HashMap;

import dmax.dialog.SpotsDialog;
import maes.tech.intentanim.CustomIntent;

public class UpdateProductActivity extends AppCompatActivity {

    private ImageView closeBtn;
    private RoundedImageView productImage;
    private TextView productInStock, productType;
    private SwitchMaterial productInStockSwitch, productTypeSwitch;
    private TextInputLayout productName, productUnit, productMRP, productPrice, productUnitsInStock,
            productDescription, productBrand, productMFGDate, productExpiryDate;
    private CardView updateProductBtnContainer;
    private ConstraintLayout layoutContent, layoutNoInternet, retryBtn, updateProductBtn, deleteProductBtn;
    private ProgressBar progressBar;

    private PreferenceManager preferenceManager;
    private AlertDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_product);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (!preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(UpdateProductActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(UpdateProductActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorBackground));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        progressDialog = new SpotsDialog.Builder().setContext(UpdateProductActivity.this)
                .setMessage("Deleting..")
                .setCancelable(false)
                .setTheme(R.style.SpotsDialog)
                .build();

        ////////////////////////////////////////////////////////////////////////////////////////////

        layoutContent = findViewById(R.id.layout_content);
        layoutNoInternet = findViewById(R.id.layout_no_internet);
        retryBtn = findViewById(R.id.retry_btn);

        closeBtn = findViewById(R.id.close_btn);

        productInStock = findViewById(R.id.product_in_stock);
        productInStockSwitch = findViewById(R.id.product_in_stock_switch);
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

        updateProductBtnContainer = findViewById(R.id.update_product_btn_container);
        updateProductBtn = findViewById(R.id.update_product_btn);
        progressBar = findViewById(R.id.progress_bar);
        deleteProductBtn = findViewById(R.id.delete_product_btn);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkNetworkConnection();
    }

    private void checkNetworkConnection() {
        if (!isConnectedToInternet(UpdateProductActivity.this)) {
            layoutContent.setVisibility(View.GONE);
            layoutNoInternet.setVisibility(View.VISIBLE);
            retryBtn.setOnClickListener(v -> checkNetworkConnection());
        } else {
            setActionOnViews();
        }
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

        KeyboardVisibilityEvent.setEventListener(UpdateProductActivity.this, isOpen -> {
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

        productInStockSwitch.setChecked(preferenceManager.getBoolean(Constants.KEY_PRODUCT_IN_STOCK));

        if (preferenceManager.getBoolean(Constants.KEY_PRODUCT_IN_STOCK)) {
            productInStock.setText("In Stock");
            productInStockSwitch.setThumbTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchThumbOpened)));
            productInStockSwitch.setTrackTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchTrackOpened)));
            productUnitsInStock.getEditText().setText(preferenceManager.getString(Constants.KEY_PRODUCT_UNITS_IN_STOCK));
        } else {
            productInStock.setText("Out of Stock");
            productInStockSwitch.setThumbTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchThumbClosed)));
            productInStockSwitch.setTrackTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchTrackClosed)));
            productUnitsInStock.getEditText().setText(String.valueOf(0));
        }

        productInStockSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                productInStock.setText("In Stock");
                productInStockSwitch.setThumbTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchThumbOpened)));
                productInStockSwitch.setTrackTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchTrackOpened)));
                productUnitsInStock.getEditText().setText(preferenceManager.getString(Constants.KEY_PRODUCT_UNITS_IN_STOCK));
            } else {
                productInStock.setText("Out of Stock");
                productInStockSwitch.setThumbTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchThumbClosed)));
                productInStockSwitch.setTrackTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchTrackClosed)));
                productUnitsInStock.getEditText().setText(String.valueOf(0));
            }
        });

        productUnitsInStock.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.toString().equals("0")) {
                    productInStockSwitch.setChecked(false);
                    productInStock.setText("Out of Stock");
                    productInStockSwitch.setThumbTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchThumbClosed)));
                    productInStockSwitch.setTrackTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchTrackClosed)));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        Glide.with(productImage.getContext()).load(Uri.parse(preferenceManager.getString(Constants.KEY_PRODUCT_IMAGE)))
                .placeholder(R.drawable.thumbnail).centerCrop().into(productImage);

        ////////////////////////////////////////////////////////////////////////////////////////////

        productTypeSwitch.setChecked(preferenceManager.getBoolean(Constants.KEY_PRODUCT_IS_VEG));

        if (preferenceManager.getBoolean(Constants.KEY_PRODUCT_IS_VEG)) {
            productType.setText("Vegetarian");
            productTypeSwitch.setThumbTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchThumbOpened)));
            productTypeSwitch.setTrackTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchTrackOpened)));
        } else {
            productType.setText("Non-Vegetarian");
            productTypeSwitch.setThumbTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchThumbClosed)));
            productTypeSwitch.setTrackTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchTrackClosed)));
        }

        productTypeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                productType.setText("Vegetarian");
                productTypeSwitch.setThumbTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchThumbOpened)));
                productTypeSwitch.setTrackTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchTrackOpened)));
            } else {
                productType.setText("Non-Vegetarian");
                productTypeSwitch.setThumbTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchThumbClosed)));
                productTypeSwitch.setTrackTintList(ColorStateList.valueOf(getColor(R.color.colorSwitchTrackClosed)));
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        productName.getEditText().setText(preferenceManager.getString(Constants.KEY_PRODUCT_NAME));
        productUnit.getEditText().setText(preferenceManager.getString(Constants.KEY_PRODUCT_UNIT));
        productMRP.getEditText().setText(preferenceManager.getString(Constants.KEY_PRODUCT_MRP));
        productPrice.getEditText().setText(preferenceManager.getString(Constants.KEY_PRODUCT_RETAIL_PRICE));

        ////////////////////////////////////////////////////////////////////////////////////////////

        productDescription.getEditText().setText(preferenceManager.getString(Constants.KEY_PRODUCT_DESCRIPTION));
        productBrand.getEditText().setText(preferenceManager.getString(Constants.KEY_PRODUCT_BRAND));
        productMFGDate.getEditText().setText(preferenceManager.getString(Constants.KEY_PRODUCT_MFG_DATE));
        productExpiryDate.getEditText().setText(preferenceManager.getString(Constants.KEY_PRODUCT_EXPIRY_TIME));

        ////////////////////////////////////////////////////////////////////////////////////////////

        updateProductBtn.setOnClickListener(v -> {
            UIUtil.hideKeyboard(UpdateProductActivity.this);

            if (!validateProductName() | !validateProductUnit() | !validateProductMRP() | !validateProductPrice() | !validateProductUnitsInStock()) {
                return;
            } else {
                if (!isConnectedToInternet(UpdateProductActivity.this)) {
                    showConnectToInternetDialog();
                    return;
                } else {
                    updateProductBtnContainer.setVisibility(View.INVISIBLE);
                    updateProductBtn.setEnabled(false);
                    progressBar.setVisibility(View.VISIBLE);

                    saveProductDataToFirebase();
                }
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        deleteProductBtn.setOnClickListener(v -> {
            if (!isConnectedToInternet(UpdateProductActivity.this)) {
                showConnectToInternetDialog();
                return;
            } else {
                MaterialDialog materialDialog = new MaterialDialog.Builder(UpdateProductActivity.this)
                        .setMessage("Are you sure of deleting " + preferenceManager.getString(Constants.KEY_PRODUCT_NAME))
                        .setCancelable(false)
                        .setPositiveButton("Delete", R.drawable.ic_dialog_delete, (dialogInterface, which) -> {
                            dialogInterface.dismiss();
                            progressDialog.show();

                            deleteProduct();
                        })
                        .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss()).build();
                materialDialog.show();
            }
        });
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
        } else {
            productUnitsInStock.setError(null);
            productUnitsInStock.setErrorEnabled(false);
            return true;
        }
    }

    private void saveProductDataToFirebase() {
        final boolean product_in_stock = productInStockSwitch.isChecked();
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

        HashMap<String, Object> category = new HashMap<>();
        category.put(Constants.KEY_CATEGORY_NAME, preferenceManager.getString(Constants.KEY_PRODUCT_CATEGORY));

        HashMap<String, Object> product = new HashMap<>();
        product.put(Constants.KEY_PRODUCT_TIMESTAMP, FieldValue.serverTimestamp());
        product.put(Constants.KEY_PRODUCT_IN_STOCK, product_in_stock);
        product.put(Constants.KEY_PRODUCT_NAME, product_name);
        product.put(Constants.KEY_PRODUCT_IS_VEG, product_is_veg);
        product.put(Constants.KEY_PRODUCT_UNIT, product_unit);
        product.put(Constants.KEY_PRODUCT_MRP, Double.parseDouble(product_mrp));
        product.put(Constants.KEY_PRODUCT_RETAIL_PRICE, Double.parseDouble(product_price));
        product.put(Constants.KEY_PRODUCT_OFFER, product_offer);
        product.put(Constants.KEY_PRODUCT_UNITS_IN_STOCK, Integer.parseInt(product_units_in_stock));
        product.put(Constants.KEY_PRODUCT_DESCRIPTION, product_desc);
        product.put(Constants.KEY_PRODUCT_BRAND, product_brand);
        product.put(Constants.KEY_PRODUCT_MFG_DATE, product_mfg_date);
        product.put(Constants.KEY_PRODUCT_EXPIRY_TIME, product_expiry_date);
        product.put(Constants.KEY_PRODUCT_SEARCH_KEYWORD, product_name.toLowerCase());

        FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_CITY))
                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                .collection(Constants.KEY_COLLECTION_STORES)
                .document(preferenceManager.getString(Constants.KEY_STORE_ID))
                .collection(Constants.KEY_COLLECTION_CATEGORIES)
                .document(preferenceManager.getString(Constants.KEY_PRODUCT_CATEGORY))
                .collection(Constants.KEY_COLLECTION_PRODUCTS)
                .document(preferenceManager.getString(Constants.KEY_PRODUCT_ID))
                .update(product)
                .addOnSuccessListener(aVoid ->
                        FirebaseFirestore.getInstance()
                                .collection(Constants.KEY_COLLECTION_CITIES)
                                .document(preferenceManager.getString(Constants.KEY_CITY))
                                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                                .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                                .collection(Constants.KEY_COLLECTION_STORES)
                                .document(preferenceManager.getString(Constants.KEY_STORE_ID))
                                .collection(Constants.KEY_COLLECTION_PRODUCTS)
                                .document(preferenceManager.getString(Constants.KEY_PRODUCT_ID))
                                .update(product)
                                .addOnSuccessListener(aVoid1 ->
                                        FirebaseFirestore.getInstance()
                                                .collection(Constants.KEY_COLLECTION_CITIES)
                                                .document(preferenceManager.getString(Constants.KEY_CITY))
                                                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                                                .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                                                .collection(Constants.KEY_COLLECTION_CATEGORIES)
                                                .document(preferenceManager.getString(Constants.KEY_PRODUCT_CATEGORY))
                                                .collection(Constants.KEY_COLLECTION_PRODUCTS)
                                                .document(preferenceManager.getString(Constants.KEY_PRODUCT_ID))
                                                .update(product)
                                                .addOnSuccessListener(aVoid11 ->
                                                        FirebaseFirestore.getInstance()
                                                                .collection(Constants.KEY_COLLECTION_CITIES)
                                                                .document(preferenceManager.getString(Constants.KEY_CITY))
                                                                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                                                                .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                                                                .collection(Constants.KEY_COLLECTION_PRODUCTS)
                                                                .document(preferenceManager.getString(Constants.KEY_PRODUCT_ID))
                                                                .update(product)
                                                                .addOnSuccessListener(aVoid111 -> {
                                                                    progressBar.setVisibility(View.GONE);
                                                                    updateProductBtnContainer.setVisibility(View.VISIBLE);
                                                                    updateProductBtn.setEnabled(true);

                                                                    onBackPressed();
                                                                    finish();
                                                                }).addOnFailureListener(e -> {
                                                            progressBar.setVisibility(View.GONE);
                                                            updateProductBtnContainer.setVisibility(View.VISIBLE);
                                                            updateProductBtn.setEnabled(true);

                                                            Alerter.create(UpdateProductActivity.this)
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
                                                        })).addOnFailureListener(e -> {
                                            progressBar.setVisibility(View.GONE);
                                            updateProductBtnContainer.setVisibility(View.VISIBLE);
                                            updateProductBtn.setEnabled(true);

                                            Alerter.create(UpdateProductActivity.this)
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
                                        })).addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE);
                            updateProductBtnContainer.setVisibility(View.VISIBLE);
                            updateProductBtn.setEnabled(true);

                            Alerter.create(UpdateProductActivity.this)
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
                        })).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            updateProductBtnContainer.setVisibility(View.VISIBLE);
            updateProductBtn.setEnabled(true);

            Alerter.create(UpdateProductActivity.this)
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
        });
    }

    private void deleteProduct() {
        FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_CITY))
                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                .collection(Constants.KEY_COLLECTION_STORES)
                .document(preferenceManager.getString(Constants.KEY_STORE_ID))
                .collection(Constants.KEY_COLLECTION_CATEGORIES)
                .document(preferenceManager.getString(Constants.KEY_PRODUCT_CATEGORY))
                .collection(Constants.KEY_COLLECTION_PRODUCTS)
                .document(preferenceManager.getString(Constants.KEY_PRODUCT_ID))
                .delete()
                .addOnSuccessListener(aVoid ->
                        FirebaseFirestore.getInstance()
                                .collection(Constants.KEY_COLLECTION_CITIES)
                                .document(preferenceManager.getString(Constants.KEY_CITY))
                                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                                .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                                .collection(Constants.KEY_COLLECTION_STORES)
                                .document(preferenceManager.getString(Constants.KEY_STORE_ID))
                                .collection(Constants.KEY_COLLECTION_PRODUCTS)
                                .document(preferenceManager.getString(Constants.KEY_PRODUCT_ID))
                                .delete()
                                .addOnSuccessListener(aVoid1 ->
                                        FirebaseFirestore.getInstance()
                                                .collection(Constants.KEY_COLLECTION_CITIES)
                                                .document(preferenceManager.getString(Constants.KEY_CITY))
                                                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                                                .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                                                .collection(Constants.KEY_COLLECTION_CATEGORIES)
                                                .document(preferenceManager.getString(Constants.KEY_PRODUCT_CATEGORY))
                                                .collection(Constants.KEY_COLLECTION_PRODUCTS)
                                                .document(preferenceManager.getString(Constants.KEY_PRODUCT_ID))
                                                .delete()
                                                .addOnSuccessListener(aVoid11 ->
                                                        FirebaseFirestore.getInstance()
                                                                .collection(Constants.KEY_COLLECTION_CITIES)
                                                                .document(preferenceManager.getString(Constants.KEY_CITY))
                                                                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                                                                .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                                                                .collection(Constants.KEY_COLLECTION_PRODUCTS)
                                                                .document(preferenceManager.getString(Constants.KEY_PRODUCT_ID))
                                                                .delete()
                                                                .addOnSuccessListener(aVoid111 -> {
                                                                    progressDialog.dismiss();

                                                                    onBackPressed();
                                                                    finish();
                                                                }).addOnFailureListener(e -> {
                                                            progressDialog.dismiss();
                                                            Alerter.create(UpdateProductActivity.this)
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
                                            progressDialog.dismiss();
                                            Alerter.create(UpdateProductActivity.this)
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
                            progressDialog.dismiss();
                            Alerter.create(UpdateProductActivity.this)
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
            progressDialog.dismiss();
            Alerter.create(UpdateProductActivity.this)
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

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean isConnectedToInternet(UpdateProductActivity updateProductActivity) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) updateProductActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (null != networkInfo &&
                (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(UpdateProductActivity.this)
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
        CustomIntent.customType(UpdateProductActivity.this, "up-to-bottom");
    }
}