package com.application.grocertaxistore;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.application.grocertaxistore.Model.Product;
import com.application.grocertaxistore.Utilities.Constants;
import com.application.grocertaxistore.Utilities.PreferenceManager;
import com.baoyz.widget.PullRefreshLayout;
import com.bumptech.glide.Glide;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.firebase.ui.firestore.paging.FirestorePagingAdapter;
import com.firebase.ui.firestore.paging.FirestorePagingOptions;
import com.firebase.ui.firestore.paging.LoadingState;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.tapadoo.alerter.Alerter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;

import java.util.ArrayList;
import java.util.Locale;

import maes.tech.intentanim.CustomIntent;

public class ProductsListActivity extends AppCompatActivity {

    private ImageView backBtn, speechToText;
    private EditText inputProductSearch;
    private RecyclerView recyclerProducts;
    private TextView title;
    private FloatingActionButton addProductBtn;
    private ConstraintLayout layoutContent, layoutEmpty, layoutNoInternet, retryBtn;
    private PullRefreshLayout pullRefreshLayout;
    private ShimmerFrameLayout shimmerLayout;

    private CollectionReference productsRef;
    private FirestorePagingAdapter<Product, ProductViewHolder> productAdapter;

    private PreferenceManager preferenceManager;
    private static int LAST_POSITION = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products_list);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (!preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(ProductsListActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(ProductsListActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorBackground));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        ////////////////////////////////////////////////////////////////////////////////////////////

        backBtn = findViewById(R.id.back_btn);
        title = findViewById(R.id.products_list_title);
        speechToText = findViewById(R.id.speech_to_text);
        inputProductSearch = findViewById(R.id.input_product_search_field);
        recyclerProducts = findViewById(R.id.recycler_products);
        addProductBtn = findViewById(R.id.add_product_btn);
        pullRefreshLayout = findViewById(R.id.pull_refresh_layout);
        shimmerLayout = findViewById(R.id.shimmer_layout);
        layoutContent = findViewById(R.id.layout_content);
        layoutEmpty = findViewById(R.id.layout_empty);
        layoutNoInternet = findViewById(R.id.layout_no_internet);
        retryBtn = findViewById(R.id.retry_btn);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkNetworkConnection();
    }

    private void checkNetworkConnection() {
        if (!isConnectedToInternet(ProductsListActivity.this)) {
            layoutContent.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.GONE);
            layoutNoInternet.setVisibility(View.VISIBLE);
            retryBtn.setOnClickListener(v -> checkNetworkConnection());
        } else {
            initFirebase();
            setActionOnViews();
        }
    }

    private void initFirebase() {
        if (preferenceManager.getString(Constants.KEY_CATEGORY).isEmpty() || preferenceManager.getString(Constants.KEY_CATEGORY).equals("")) {
            productsRef = FirebaseFirestore.getInstance()
                    .collection(Constants.KEY_COLLECTION_CITIES)
                    .document(preferenceManager.getString(Constants.KEY_CITY))
                    .collection(Constants.KEY_COLLECTION_LOCALITIES)
                    .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                    .collection(Constants.KEY_COLLECTION_STORES)
                    .document(preferenceManager.getString(Constants.KEY_STORE_ID))
                    .collection(Constants.KEY_COLLECTION_PRODUCTS);
        } else {
            productsRef = FirebaseFirestore.getInstance()
                    .collection(Constants.KEY_COLLECTION_CITIES)
                    .document(preferenceManager.getString(Constants.KEY_CITY))
                    .collection(Constants.KEY_COLLECTION_LOCALITIES)
                    .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                    .collection(Constants.KEY_COLLECTION_STORES)
                    .document(preferenceManager.getString(Constants.KEY_STORE_ID))
                    .collection(Constants.KEY_COLLECTION_CATEGORIES)
                    .document(preferenceManager.getString(Constants.KEY_CATEGORY))
                    .collection(Constants.KEY_COLLECTION_PRODUCTS);
        }
    }

    private void setActionOnViews() {
        layoutNoInternet.setVisibility(View.GONE);
        layoutContent.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);

        pullRefreshLayout.setRefreshing(false);

        ////////////////////////////////////////////////////////////////////////////////////////////

        pullRefreshLayout.setColor(getColor(R.color.colorAccent));
        pullRefreshLayout.setBackgroundColor(getColor(R.color.colorBackground));
        pullRefreshLayout.setOnRefreshListener(this::checkNetworkConnection);

        ////////////////////////////////////////////////////////////////////////////////////////////

        backBtn.setOnClickListener(v -> {
            onBackPressed();
            finish();
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        KeyboardVisibilityEvent.setEventListener(ProductsListActivity.this, isOpen -> {
            if (!isOpen) {
                inputProductSearch.clearFocus();
            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        if (preferenceManager.getString(Constants.KEY_CATEGORY).isEmpty() || preferenceManager.getString(Constants.KEY_CATEGORY).equals("")) {
            title.setText("All products");
            inputProductSearch.setHint("Search products");
        } else {
            title.setText(preferenceManager.getString(Constants.KEY_CATEGORY));
            inputProductSearch.setHint(String.format("Search in %s", preferenceManager.getString(Constants.KEY_CATEGORY)));
        }

        ////////////////////////////////////////////////////////////////////////////////////////////

        loadProducts();

        ////////////////////////////////////////////////////////////////////////////////////////////

        speechToText.setOnClickListener(view -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, String.format("Name a product!"));

            try {
                startActivityForResult(intent, 123);
            } catch (ActivityNotFoundException e) {
                Alerter.create(ProductsListActivity.this)
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
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        inputProductSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Query updatedQuery;
                if (s.toString().isEmpty()) {
                    updatedQuery = productsRef.orderBy(Constants.KEY_PRODUCT_NAME, Query.Direction.ASCENDING);
                } else {
                    updatedQuery = productsRef.orderBy(Constants.KEY_PRODUCT_SEARCH_KEYWORD, Query.Direction.ASCENDING)
                            .startAt(s.toString().toLowerCase().trim()).endAt(s.toString().toLowerCase().trim() + "\uf8ff");
                }

                PagedList.Config updatedConfig = new PagedList.Config.Builder()
                        .setInitialLoadSizeHint(4)
                        .setPageSize(4)
                        .build();

                FirestorePagingOptions<Product> updatedOptions = new FirestorePagingOptions.Builder<Product>()
                        .setLifecycleOwner(ProductsListActivity.this)
                        .setQuery(updatedQuery, updatedConfig, Product.class)
                        .build();

                productAdapter.updateOptions(updatedOptions);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        ////////////////////////////////////////////////////////////////////////////////////////////

        addProductBtn.setOnClickListener(v -> {
            startActivity(new Intent(ProductsListActivity.this, AddProductActivity.class));
            CustomIntent.customType(ProductsListActivity.this, "bottom-to-up");
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 123:
                if (resultCode == RESULT_OK && data != null) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    inputProductSearch.setText(result.get(0));
                    inputProductSearch.clearFocus();
                }
                break;
        }
    }

    //////////////////////////////////////// Load Products /////////////////////////////////////////

    private void loadProducts() {
        shimmerLayout.setVisibility(View.VISIBLE);
        shimmerLayout.startShimmer();

        Query query = productsRef.orderBy(Constants.KEY_PRODUCT_NAME, Query.Direction.ASCENDING);
        PagedList.Config config = new PagedList.Config.Builder()
                .setInitialLoadSizeHint(4)
                .setPageSize(4)
                .build();
        FirestorePagingOptions<Product> options = new FirestorePagingOptions.Builder<Product>()
                .setLifecycleOwner(ProductsListActivity.this)
                .setQuery(query, config, Product.class)
                .build();

        productAdapter = new FirestorePagingAdapter<Product, ProductViewHolder>(options) {

            @NonNull
            @Override
            public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_product, parent, false);
                return new ProductViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull ProductViewHolder holder, int position, @NonNull Product model) {
                Glide.with(holder.productImage.getContext()).load(model.getProductImage())
                        .placeholder(R.drawable.thumbnail).centerCrop().into(holder.productImage);

                ////////////////////////////////////////////////////////////////////////////////////

                if (model.getProductCategory().equals("Baby Care") || model.getProductCategory().equals("Household") ||
                        model.getProductCategory().equals("Personal Care") || model.getProductCategory().equals("Stationary") ||
                        model.getProductCategory().equals("Hardware") || model.getProductCategory().equals("Medical") || model.getProductCategory().equals("Sports")) {
                    holder.productTypeImage.setVisibility(View.GONE);
                } else {
                    holder.productTypeImage.setVisibility(View.VISIBLE);
                    if (model.isProductIsVeg()) {
                        holder.productTypeImage.setImageResource(R.drawable.ic_veg);
                    } else {
                        holder.productTypeImage.setImageResource(R.drawable.ic_nonveg);
                    }
                }

                ////////////////////////////////////////////////////////////////////////////////////

                holder.productName.setText(model.getProductName());
                holder.productUnit.setText(model.getProductUnit());
                holder.productCategory.setText(model.getProductCategory());
                holder.productPrice.setText(String.format("₹ %s", model.getProductRetailPrice()));

                ////////////////////////////////////////////////////////////////////////////////////

                if (model.getProductOffer() == 0) {
                    holder.productMRP.setVisibility(View.GONE);
                    holder.productOffer.setVisibility(View.GONE);
                } else {
                    holder.productMRP.setText(String.format("₹ %s", model.getProductMRP()));
                    holder.productMRP.setPaintFlags(holder.productMRP.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    holder.productOffer.setText(String.format("%d%% OFF", model.getProductOffer()));
                }

                ////////////////////////////////////////////////////////////////////////////////////

                if (model.isProductInStock()) {
                    holder.productImage.clearColorFilter();

                    holder.productOffer.setVisibility(View.VISIBLE);

                    holder.productStatus.setText("In Stock");
                    holder.productStatus.setTextColor(getColor(R.color.successColor));

                    holder.productUnitInStock.setVisibility(View.VISIBLE);
                    if (model.getProductUnitsInStock() == 1) {
                        holder.productUnitInStock.setText(String.format("(%d unit in stock)", model.getProductUnitsInStock()));
                    } else if (model.getProductUnitsInStock() > 1) {
                        holder.productUnitInStock.setText(String.format("(%d units in stock)", model.getProductUnitsInStock()));
                    }
                } else {
                    ColorMatrix matrix = new ColorMatrix();
                    matrix.setSaturation(0);
                    ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
                    holder.productImage.setColorFilter(filter);

                    holder.productOffer.setVisibility(View.GONE);

                    holder.productStatus.setText("Out of Stock");
                    holder.productStatus.setTextColor(getColor(R.color.errorColor));

                    holder.productUnitInStock.setVisibility(View.GONE);
                }

                ////////////////////////////////////////////////////////////////////////////////////

                holder.clickListener.setOnClickListener(v -> {
                    preferenceManager.putString(Constants.KEY_PRODUCT_ID, model.getProductID());
                    preferenceManager.putString(Constants.KEY_PRODUCT_CATEGORY, model.getProductCategory());
                    preferenceManager.putBoolean(Constants.KEY_PRODUCT_IN_STOCK, model.isProductInStock());
                    preferenceManager.putString(Constants.KEY_PRODUCT_IMAGE, model.getProductImage());
                    preferenceManager.putString(Constants.KEY_PRODUCT_NAME, model.getProductName());
                    preferenceManager.putBoolean(Constants.KEY_PRODUCT_IS_VEG, model.isProductIsVeg());
                    preferenceManager.putString(Constants.KEY_PRODUCT_UNIT, model.getProductUnit());
                    preferenceManager.putString(Constants.KEY_PRODUCT_MRP, String.valueOf(model.getProductMRP()));
                    preferenceManager.putString(Constants.KEY_PRODUCT_RETAIL_PRICE, String.valueOf(model.getProductRetailPrice()));
                    preferenceManager.putString(Constants.KEY_PRODUCT_UNITS_IN_STOCK, String.valueOf(model.getProductUnitsInStock()));
                    preferenceManager.putString(Constants.KEY_PRODUCT_DESCRIPTION, model.getProductDescription());
                    preferenceManager.putString(Constants.KEY_PRODUCT_BRAND, model.getProductBrand());
                    preferenceManager.putString(Constants.KEY_PRODUCT_MFG_DATE, model.getProductMFGDate());
                    preferenceManager.putString(Constants.KEY_PRODUCT_EXPIRY_TIME, model.getProductExpiryTime());

                    startActivity(new Intent(ProductsListActivity.this, UpdateProductActivity.class));
                    CustomIntent.customType(ProductsListActivity.this, "bottom-to-up");
                });

                setAnimation(holder.itemView, position);
            }

            public void setAnimation(View viewToAnimate, int position) {
                if (position > LAST_POSITION) {
                    ScaleAnimation scaleAnimation = new ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f,
                            Animation.RELATIVE_TO_SELF, 0.5f,
                            Animation.RELATIVE_TO_SELF, 0.5f);
                    scaleAnimation.setDuration(1000);

                    viewToAnimate.setAnimation(scaleAnimation);
                    LAST_POSITION = position;
                }
            }

            @Override
            protected void onLoadingStateChanged(@NonNull LoadingState state) {
                super.onLoadingStateChanged(state);
                switch (state) {
                    case LOADING_INITIAL:
                    case LOADING_MORE:
                        pullRefreshLayout.setRefreshing(false);
                        break;
                    case LOADED:
                    case FINISHED:
                        pullRefreshLayout.setRefreshing(false);
                        shimmerLayout.stopShimmer();
                        shimmerLayout.setVisibility(View.GONE);

                        if (getItemCount() == 0) {
                            layoutEmpty.setVisibility(View.VISIBLE);
                        } else {
                            layoutEmpty.setVisibility(View.GONE);
                        }
                        break;
                    case ERROR:
                        pullRefreshLayout.setRefreshing(false);
                        shimmerLayout.stopShimmer();
                        shimmerLayout.setVisibility(View.GONE);
                        Alerter.create(ProductsListActivity.this)
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
                        break;
                }
            }
        };

        productAdapter.notifyDataSetChanged();

        recyclerProducts.setHasFixedSize(true);
        recyclerProducts.setLayoutManager(new LinearLayoutManager(ProductsListActivity.this));
        recyclerProducts.setAdapter(productAdapter);
    }

    ////////////////////////////////////// ProductViewHolder ///////////////////////////////////////

    public static class ProductViewHolder extends RecyclerView.ViewHolder {

        ConstraintLayout clickListener;
        ImageView productImage, productTypeImage;
        TextView productName, productPrice, productMRP, productOffer,
                productUnit, productCategory, productStatus, productUnitInStock;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);

            clickListener = itemView.findViewById(R.id.click_listener);
            productImage = itemView.findViewById(R.id.product_image);
            productTypeImage = itemView.findViewById(R.id.product_type_image);
            productName = itemView.findViewById(R.id.product_name);
            productPrice = itemView.findViewById(R.id.product_price);
            productMRP = itemView.findViewById(R.id.product_mrp);
            productOffer = itemView.findViewById(R.id.product_offer);
            productUnit = itemView.findViewById(R.id.product_unit);
            productCategory = itemView.findViewById(R.id.product_category);
            productStatus = itemView.findViewById(R.id.product_status);
            productUnitInStock = itemView.findViewById(R.id.product_unit_in_stock);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean isConnectedToInternet(ProductsListActivity productsListActivity) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) productsListActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (null != networkInfo &&
                (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        CustomIntent.customType(ProductsListActivity.this, "right-to-left");
    }
}