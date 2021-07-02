package com.application.grocertaxistore;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.application.grocertaxistore.Model.OrderItem;
import com.application.grocertaxistore.Utilities.Constants;
import com.application.grocertaxistore.Utilities.PreferenceManager;
import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.makeramen.roundedimageview.RoundedImageView;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.tapadoo.alerter.Alerter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import dmax.dialog.SpotsDialog;
import maes.tech.intentanim.CustomIntent;

public class OrderDetailsActivity extends AppCompatActivity {

    private ImageView closeBtn, updateStatus;
    private TextView orderID, orderStatus, orderPayment, customerName, customerMobile, customerAddress, deliveryDistance,
            orderNoOfItems, totalMRP, discountAmount, couponDiscount, deliveryCharges, textTipAdded, tipAmount, convenienceFee, totalPayable,
            orderID2, orderInstructions, paymentMethod, orderPlacedTime, textDeliveredOn, orderCompletionTime;
    private RecyclerView recyclerOrderItems;
    private CardView cancelBtnContainer;
    private ConstraintLayout layoutContent, layoutNoInternet, retryBtn, cancelBtn;
    private ProgressBar progressBar;

    private CollectionReference storeOrdersRef;
    private FirestoreRecyclerAdapter<OrderItem, OrderItemViewHolder> orderItemAdapter;

    private PreferenceManager preferenceManager;
    private AlertDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (!preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(OrderDetailsActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(OrderDetailsActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorBackground));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        progressDialog = new SpotsDialog.Builder().setContext(OrderDetailsActivity.this)
                .setMessage("Hold on..")
                .setCancelable(false)
                .setTheme(R.style.SpotsDialog)
                .build();

        ////////////////////////////////////////////////////////////////////////////////////////////

        layoutContent = findViewById(R.id.layout_content);
        layoutNoInternet = findViewById(R.id.layout_no_internet);
        retryBtn = findViewById(R.id.retry_btn);

        closeBtn = findViewById(R.id.close_btn);

        orderID = findViewById(R.id.order_id);
        orderStatus = findViewById(R.id.order_status);
        updateStatus = findViewById(R.id.update_status);
        orderPayment = findViewById(R.id.order_payment);

        customerName = findViewById(R.id.customer_name);
        customerMobile = findViewById(R.id.customer_mobile);
        customerAddress = findViewById(R.id.customer_address);
        deliveryDistance = findViewById(R.id.delivery_distance);

        orderNoOfItems = findViewById(R.id.no_of_items);
        recyclerOrderItems = findViewById(R.id.recycler_order_items);
        totalMRP = findViewById(R.id.total_mrp);
        discountAmount = findViewById(R.id.discount_amount);
        couponDiscount = findViewById(R.id.coupon_discount);
        deliveryCharges = findViewById(R.id.delivery_charges);
        textTipAdded = findViewById(R.id.text_tip_added);
        tipAmount = findViewById(R.id.tip_amount);
        convenienceFee = findViewById(R.id.convenience_fee);
        totalPayable = findViewById(R.id.total_payable);

        orderID2 = findViewById(R.id.order_id2);
        orderInstructions = findViewById(R.id.instructions);
        paymentMethod = findViewById(R.id.payment_method);
        orderPlacedTime = findViewById(R.id.order_placing_time);
        textDeliveredOn = findViewById(R.id.text_delivered_on);
        orderCompletionTime = findViewById(R.id.order_completed_time);

        cancelBtnContainer = findViewById(R.id.cancel_btn_container);
        cancelBtn = findViewById(R.id.cancel_btn);
        progressBar = findViewById(R.id.progress_bar);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkNetworkConnection();
    }

    private void checkNetworkConnection() {
        if (!isConnectedToInternet(OrderDetailsActivity.this)) {
            layoutContent.setVisibility(View.GONE);
            layoutNoInternet.setVisibility(View.VISIBLE);
            retryBtn.setOnClickListener(v -> checkNetworkConnection());
        } else {
            initFirebase();
            setActionOnViews();
        }
    }

    private void initFirebase() {
        if (preferenceManager.getString(Constants.KEY_ORDER_TYPE).equals("Pending")) {
            storeOrdersRef = FirebaseFirestore.getInstance()
                    .collection(Constants.KEY_COLLECTION_CITIES)
                    .document(preferenceManager.getString(Constants.KEY_CITY))
                    .collection(Constants.KEY_COLLECTION_LOCALITIES)
                    .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                    .collection(Constants.KEY_COLLECTION_STORES)
                    .document(preferenceManager.getString(Constants.KEY_STORE_ID))
                    .collection(Constants.KEY_COLLECTION_PENDING_ORDERS);
        } else if (preferenceManager.getString(Constants.KEY_ORDER_TYPE).equals("Completed")) {
            storeOrdersRef = FirebaseFirestore.getInstance()
                    .collection(Constants.KEY_COLLECTION_CITIES)
                    .document(preferenceManager.getString(Constants.KEY_CITY))
                    .collection(Constants.KEY_COLLECTION_LOCALITIES)
                    .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                    .collection(Constants.KEY_COLLECTION_STORES)
                    .document(preferenceManager.getString(Constants.KEY_STORE_ID))
                    .collection(Constants.KEY_COLLECTION_COMPLETED_ORDERS);
        } else if (preferenceManager.getString(Constants.KEY_ORDER_TYPE).equals("Cancelled")) {
            storeOrdersRef = FirebaseFirestore.getInstance()
                    .collection(Constants.KEY_COLLECTION_CITIES)
                    .document(preferenceManager.getString(Constants.KEY_CITY))
                    .collection(Constants.KEY_COLLECTION_LOCALITIES)
                    .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                    .collection(Constants.KEY_COLLECTION_STORES)
                    .document(preferenceManager.getString(Constants.KEY_STORE_ID))
                    .collection(Constants.KEY_COLLECTION_CANCELLED_ORDERS);
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

        progressBar.setVisibility(View.VISIBLE);
        loadOrderDetails();
        loadOrderItems();
    }

    /////////////////////////////////////// loadOrderDetails ///////////////////////////////////////

    private void loadOrderDetails() {
        final DocumentReference orderDocumentRef = storeOrdersRef.document(preferenceManager.getString(Constants.KEY_ORDER));

        orderDocumentRef.addSnapshotListener((documentSnapshot, error) -> {
            if (error != null) {
                progressBar.setVisibility(View.GONE);
                onBackPressed();
            } else {
                progressBar.setVisibility(View.GONE);
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    /////////////////////////////// Order ID ///////////////////////////////////

                    orderID.setText(preferenceManager.getString(Constants.KEY_ORDER));
                    orderID2.setText(preferenceManager.getString(Constants.KEY_ORDER));

                    ///////////////////////// Order Status & Time //////////////////////////////

                    String order_status = documentSnapshot.getString(Constants.KEY_ORDER_STATUS);
                    String order_placing_time = documentSnapshot.getString(Constants.KEY_ORDER_PLACED_TIME);
                    String order_completion_time = documentSnapshot.getString(Constants.KEY_ORDER_COMPLETION_TIME);
                    String order_cancellation_time = documentSnapshot.getString(Constants.KEY_ORDER_CANCELLATION_TIME);

                    if (preferenceManager.getString(Constants.KEY_ORDER_TYPE).equals("Pending")) {
                        orderStatus.setText(order_status);
                        orderStatus.setTextColor(getColor(R.color.processingColor));

                        updateStatus.setVisibility(View.VISIBLE);
                        updateStatus.setOnClickListener(v -> {
                            PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                            popupMenu.inflate(R.menu.menu_update_status);
                            popupMenu.setOnMenuItemClickListener(item -> {
                                switch (item.getItemId()) {
                                    case R.id.menu_processing:
                                        if (!isConnectedToInternet(OrderDetailsActivity.this)) {
                                            showConnectToInternetDialog();
                                        } else {
                                            progressDialog.show();
                                            storeOrdersRef.document(preferenceManager.getString(Constants.KEY_ORDER))
                                                    .update(Constants.KEY_ORDER_STATUS, "Processing",
                                                            Constants.KEY_ORDER_TIMESTAMP, FieldValue.serverTimestamp())
                                                    .addOnSuccessListener(aVoid ->
                                                            FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                                                                    .document(documentSnapshot.getString(Constants.KEY_ORDER_BY_USERID))
                                                                    .collection(Constants.KEY_COLLECTION_PENDING_ORDERS)
                                                                    .document(preferenceManager.getString(Constants.KEY_ORDER))
                                                                    .update(Constants.KEY_ORDER_STATUS, "Processing",
                                                                            Constants.KEY_ORDER_TIMESTAMP, FieldValue.serverTimestamp())
                                                                    .addOnSuccessListener(aVoid1 -> {
                                                                        progressDialog.dismiss();
                                                                        orderStatus.setText("Processing");
                                                                        orderStatus.setTextColor(getColor(R.color.processingColor));
                                                                    }).addOnFailureListener(e -> {
                                                                progressDialog.dismiss();
                                                                Alerter.create(OrderDetailsActivity.this)
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
                                                progressDialog.dismiss();
                                                Alerter.create(OrderDetailsActivity.this)
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
                                        return true;
                                    case R.id.menu_packed:
                                        if (!isConnectedToInternet(OrderDetailsActivity.this)) {
                                            showConnectToInternetDialog();
                                        } else {
                                            progressDialog.show();
                                            storeOrdersRef.document(preferenceManager.getString(Constants.KEY_ORDER))
                                                    .update(Constants.KEY_ORDER_STATUS, "Packed",
                                                            Constants.KEY_ORDER_TIMESTAMP, FieldValue.serverTimestamp())
                                                    .addOnSuccessListener(aVoid ->
                                                            FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                                                                    .document(documentSnapshot.getString(Constants.KEY_ORDER_BY_USERID))
                                                                    .collection(Constants.KEY_COLLECTION_PENDING_ORDERS)
                                                                    .document(preferenceManager.getString(Constants.KEY_ORDER))
                                                                    .update(Constants.KEY_ORDER_STATUS, "Packed",
                                                                            Constants.KEY_ORDER_TIMESTAMP, FieldValue.serverTimestamp())
                                                                    .addOnSuccessListener(aVoid1 -> {
                                                                        progressDialog.dismiss();
                                                                        orderStatus.setText("Packed");
                                                                        orderStatus.setTextColor(getColor(R.color.processingColor));
                                                                    }).addOnFailureListener(e -> {
                                                                progressDialog.dismiss();
                                                                Alerter.create(OrderDetailsActivity.this)
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
                                                progressDialog.dismiss();
                                                Alerter.create(OrderDetailsActivity.this)
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
                                        return true;
                                    case R.id.menu_out_for_delivery:
                                        if (!isConnectedToInternet(OrderDetailsActivity.this)) {
                                            showConnectToInternetDialog();
                                        } else {
                                            progressDialog.show();
                                            storeOrdersRef.document(preferenceManager.getString(Constants.KEY_ORDER))
                                                    .update(Constants.KEY_ORDER_STATUS, "Out for Delivery",
                                                            Constants.KEY_ORDER_TIMESTAMP, FieldValue.serverTimestamp())
                                                    .addOnSuccessListener(aVoid ->
                                                            FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                                                                    .document(documentSnapshot.getString(Constants.KEY_ORDER_BY_USERID))
                                                                    .collection(Constants.KEY_COLLECTION_PENDING_ORDERS)
                                                                    .document(preferenceManager.getString(Constants.KEY_ORDER))
                                                                    .update(Constants.KEY_ORDER_STATUS, "Out for Delivery",
                                                                            Constants.KEY_ORDER_TIMESTAMP, FieldValue.serverTimestamp())
                                                                    .addOnSuccessListener(aVoid1 -> {
                                                                        progressDialog.dismiss();
                                                                        orderStatus.setText("Out for Delivery");
                                                                        orderStatus.setTextColor(getColor(R.color.processingColor));
                                                                    }).addOnFailureListener(e -> {
                                                                progressDialog.dismiss();
                                                                Alerter.create(OrderDetailsActivity.this)
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
                                                progressDialog.dismiss();
                                                Alerter.create(OrderDetailsActivity.this)
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
                                        return true;
                                    case R.id.menu_delivered:
                                        if (!isConnectedToInternet(OrderDetailsActivity.this)) {
                                            showConnectToInternetDialog();
                                        } else {
                                            progressDialog.show();
                                            Calendar calendar = Calendar.getInstance();
                                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEEE , dd-MMM-yyyy hh:mm a");
                                            String currentTime = simpleDateFormat.format(calendar.getTime());

                                            HashMap<String, Object> newCompletedOrder = new HashMap<>();
                                            newCompletedOrder.put(Constants.KEY_ORDER_ID, documentSnapshot.get(Constants.KEY_ORDER_ID));
                                            newCompletedOrder.put(Constants.KEY_ORDER_BY_USERID, documentSnapshot.get(Constants.KEY_ORDER_BY_USERID));
                                            newCompletedOrder.put(Constants.KEY_ORDER_BY_USERNAME, documentSnapshot.get(Constants.KEY_ORDER_BY_USERNAME));
                                            newCompletedOrder.put(Constants.KEY_ORDER_FROM_STOREID, documentSnapshot.get(Constants.KEY_ORDER_FROM_STOREID));
                                            newCompletedOrder.put(Constants.KEY_ORDER_FROM_STORENAME, documentSnapshot.get(Constants.KEY_ORDER_FROM_STORENAME));
                                            newCompletedOrder.put(Constants.KEY_ORDER_CUSTOMER_NAME, documentSnapshot.get(Constants.KEY_ORDER_CUSTOMER_NAME));
                                            newCompletedOrder.put(Constants.KEY_ORDER_CUSTOMER_MOBILE, documentSnapshot.get(Constants.KEY_ORDER_CUSTOMER_MOBILE));
                                            newCompletedOrder.put(Constants.KEY_ORDER_DELIVERY_LOCATION, documentSnapshot.get(Constants.KEY_ORDER_DELIVERY_LOCATION));
                                            newCompletedOrder.put(Constants.KEY_ORDER_DELIVERY_ADDRESS, documentSnapshot.get(Constants.KEY_ORDER_DELIVERY_ADDRESS));
                                            newCompletedOrder.put(Constants.KEY_ORDER_DELIVERY_LATITUDE, documentSnapshot.get(Constants.KEY_ORDER_DELIVERY_LATITUDE));
                                            newCompletedOrder.put(Constants.KEY_ORDER_DELIVERY_LONGITUDE, documentSnapshot.get(Constants.KEY_ORDER_DELIVERY_LONGITUDE));
                                            newCompletedOrder.put(Constants.KEY_ORDER_DELIVERY_DISTANCE, documentSnapshot.get(Constants.KEY_ORDER_DELIVERY_DISTANCE));
                                            newCompletedOrder.put(Constants.KEY_ORDER_NO_OF_ITEMS, documentSnapshot.get(Constants.KEY_ORDER_NO_OF_ITEMS));
                                            newCompletedOrder.put(Constants.KEY_ORDER_TOTAL_MRP, documentSnapshot.get(Constants.KEY_ORDER_TOTAL_MRP));
                                            newCompletedOrder.put(Constants.KEY_ORDER_TOTAL_RETAIL_PRICE, documentSnapshot.get(Constants.KEY_ORDER_TOTAL_RETAIL_PRICE));
                                            newCompletedOrder.put(Constants.KEY_ORDER_COUPON_APPLIED, documentSnapshot.get(Constants.KEY_ORDER_COUPON_APPLIED));
                                            newCompletedOrder.put(Constants.KEY_ORDER_COUPON_DISCOUNT, documentSnapshot.get(Constants.KEY_ORDER_COUPON_DISCOUNT));
                                            newCompletedOrder.put(Constants.KEY_ORDER_TOTAL_DISCOUNT, documentSnapshot.get(Constants.KEY_ORDER_TOTAL_DISCOUNT));
                                            newCompletedOrder.put(Constants.KEY_ORDER_DELIVERY_CHARGES, documentSnapshot.get(Constants.KEY_ORDER_DELIVERY_CHARGES));
                                            newCompletedOrder.put(Constants.KEY_ORDER_TIP_AMOUNT, documentSnapshot.get(Constants.KEY_ORDER_TIP_AMOUNT));
                                            newCompletedOrder.put(Constants.KEY_ORDER_SUB_TOTAL, documentSnapshot.get(Constants.KEY_ORDER_SUB_TOTAL));
                                            newCompletedOrder.put(Constants.KEY_ORDER_PAYMENT_MODE, documentSnapshot.get(Constants.KEY_ORDER_PAYMENT_MODE));
                                            newCompletedOrder.put(Constants.KEY_ORDER_CONVENIENCE_FEE, documentSnapshot.get(Constants.KEY_ORDER_CONVENIENCE_FEE));
                                            newCompletedOrder.put(Constants.KEY_ORDER_TOTAL_PAYABLE, documentSnapshot.get(Constants.KEY_ORDER_TOTAL_PAYABLE));
                                            newCompletedOrder.put(Constants.KEY_ORDER_INSTRUCTIONS, documentSnapshot.get(Constants.KEY_ORDER_INSTRUCTIONS));
                                            newCompletedOrder.put(Constants.KEY_ORDER_STATUS, "Delivered");
                                            newCompletedOrder.put(Constants.KEY_ORDER_PLACED_TIME, documentSnapshot.get(Constants.KEY_ORDER_PLACED_TIME));
                                            newCompletedOrder.put(Constants.KEY_ORDER_COMPLETION_TIME, currentTime);
                                            newCompletedOrder.put(Constants.KEY_ORDER_CANCELLATION_TIME, "");
                                            newCompletedOrder.put(Constants.KEY_ORDER_TIMESTAMP, FieldValue.serverTimestamp());

                                            FirebaseFirestore.getInstance()
                                                    .collection(Constants.KEY_COLLECTION_CITIES)
                                                    .document(preferenceManager.getString(Constants.KEY_CITY))
                                                    .collection(Constants.KEY_COLLECTION_LOCALITIES)
                                                    .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                                                    .collection(Constants.KEY_COLLECTION_STORES)
                                                    .document(preferenceManager.getString(Constants.KEY_STORE_ID))
                                                    .collection(Constants.KEY_COLLECTION_COMPLETED_ORDERS)
                                                    .document(preferenceManager.getString(Constants.KEY_ORDER))
                                                    .set(newCompletedOrder)
                                                    .addOnSuccessListener(aVoid ->
                                                            FirebaseFirestore.getInstance()
                                                                    .collection(Constants.KEY_COLLECTION_CITIES)
                                                                    .document(preferenceManager.getString(Constants.KEY_CITY))
                                                                    .collection(Constants.KEY_COLLECTION_LOCALITIES)
                                                                    .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                                                                    .collection(Constants.KEY_COLLECTION_STORES)
                                                                    .document(preferenceManager.getString(Constants.KEY_STORE_ID))
                                                                    .collection(Constants.KEY_COLLECTION_PENDING_ORDERS)
                                                                    .document(preferenceManager.getString(Constants.KEY_ORDER))
                                                                    .collection(Constants.KEY_COLLECTION_ORDER_ITEMS)
                                                                    .get()
                                                                    .addOnSuccessListener(queryDocumentSnapshots1 -> {
                                                                        for (QueryDocumentSnapshot itemDocumentSnapshot : queryDocumentSnapshots1) {
                                                                            HashMap<String, Object> newCompletedOrderItem = new HashMap<>();
                                                                            newCompletedOrderItem.put(Constants.KEY_ORDER_ITEM_ID, itemDocumentSnapshot.getId());
                                                                            newCompletedOrderItem.put(Constants.KEY_ORDER_ITEM_PRODUCT_ID, itemDocumentSnapshot.get(Constants.KEY_ORDER_ITEM_PRODUCT_ID));
                                                                            newCompletedOrderItem.put(Constants.KEY_ORDER_ITEM_PRODUCT_STORE_ID, itemDocumentSnapshot.get(Constants.KEY_ORDER_ITEM_PRODUCT_STORE_ID));
                                                                            newCompletedOrderItem.put(Constants.KEY_ORDER_ITEM_PRODUCT_STORE_NAME, itemDocumentSnapshot.get(Constants.KEY_ORDER_ITEM_PRODUCT_STORE_NAME));
                                                                            newCompletedOrderItem.put(Constants.KEY_ORDER_ITEM_PRODUCT_CATEGORY, itemDocumentSnapshot.get(Constants.KEY_ORDER_ITEM_PRODUCT_CATEGORY));
                                                                            newCompletedOrderItem.put(Constants.KEY_ORDER_ITEM_PRODUCT_IMAGE, itemDocumentSnapshot.get(Constants.KEY_ORDER_ITEM_PRODUCT_IMAGE));
                                                                            newCompletedOrderItem.put(Constants.KEY_ORDER_ITEM_PRODUCT_NAME, itemDocumentSnapshot.get(Constants.KEY_ORDER_ITEM_PRODUCT_NAME));
                                                                            newCompletedOrderItem.put(Constants.KEY_ORDER_ITEM_PRODUCT_UNIT, itemDocumentSnapshot.get(Constants.KEY_ORDER_ITEM_PRODUCT_UNIT));
                                                                            newCompletedOrderItem.put(Constants.KEY_ORDER_ITEM_PRODUCT_MRP, itemDocumentSnapshot.get(Constants.KEY_ORDER_ITEM_PRODUCT_MRP));
                                                                            newCompletedOrderItem.put(Constants.KEY_ORDER_ITEM_PRODUCT_RETAIL_PRICE, itemDocumentSnapshot.get(Constants.KEY_ORDER_ITEM_PRODUCT_RETAIL_PRICE));
                                                                            newCompletedOrderItem.put(Constants.KEY_ORDER_ITEM_PRODUCT_QUANTITY, itemDocumentSnapshot.get(Constants.KEY_ORDER_ITEM_PRODUCT_QUANTITY));
                                                                            newCompletedOrderItem.put(Constants.KEY_ORDER_ITEM_TIMESTAMP, FieldValue.serverTimestamp());

                                                                            FirebaseFirestore.getInstance()
                                                                                    .collection(Constants.KEY_COLLECTION_CITIES)
                                                                                    .document(preferenceManager.getString(Constants.KEY_CITY))
                                                                                    .collection(Constants.KEY_COLLECTION_LOCALITIES)
                                                                                    .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                                                                                    .collection(Constants.KEY_COLLECTION_STORES)
                                                                                    .document(preferenceManager.getString(Constants.KEY_STORE_ID))
                                                                                    .collection(Constants.KEY_COLLECTION_COMPLETED_ORDERS)
                                                                                    .document(preferenceManager.getString(Constants.KEY_ORDER))
                                                                                    .collection(Constants.KEY_COLLECTION_ORDER_ITEMS)
                                                                                    .document(itemDocumentSnapshot.getId())
                                                                                    .set(newCompletedOrderItem)
                                                                                    .addOnSuccessListener(aVoid12 ->
                                                                                            FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                                                                                                    .document(documentSnapshot.getString(Constants.KEY_ORDER_BY_USERID))
                                                                                                    .collection(Constants.KEY_COLLECTION_COMPLETED_ORDERS)
                                                                                                    .document(preferenceManager.getString(Constants.KEY_ORDER))
                                                                                                    .set(newCompletedOrder)
                                                                                                    .addOnSuccessListener(aVoid121 ->
                                                                                                            FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                                                                                                                    .document(documentSnapshot.getString(Constants.KEY_ORDER_BY_USERID))
                                                                                                                    .collection(Constants.KEY_COLLECTION_COMPLETED_ORDERS)
                                                                                                                    .document(preferenceManager.getString(Constants.KEY_ORDER))
                                                                                                                    .collection(Constants.KEY_COLLECTION_ORDER_ITEMS)
                                                                                                                    .document(itemDocumentSnapshot.getId())
                                                                                                                    .set(newCompletedOrderItem)
                                                                                                                    .addOnSuccessListener(aVoid1211 ->
                                                                                                                            FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                                                                                                                                    .document(documentSnapshot.getString(Constants.KEY_ORDER_BY_USERID))
                                                                                                                                    .collection(Constants.KEY_COLLECTION_PENDING_ORDERS)
                                                                                                                                    .document(preferenceManager.getString(Constants.KEY_ORDER))
                                                                                                                                    .collection(Constants.KEY_COLLECTION_ORDER_ITEMS)
                                                                                                                                    .get()
                                                                                                                                    .addOnSuccessListener(queryDocumentSnapshots2 -> {
                                                                                                                                        for (QueryDocumentSnapshot deleteDocumentSnapshot1 : queryDocumentSnapshots2) {
                                                                                                                                            deleteDocumentSnapshot1.getReference().delete()
                                                                                                                                                    .addOnSuccessListener(aVoid12111 ->
                                                                                                                                                            FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                                                                                                                                                                    .document(documentSnapshot.getString(Constants.KEY_ORDER_BY_USERID))
                                                                                                                                                                    .collection(Constants.KEY_COLLECTION_PENDING_ORDERS)
                                                                                                                                                                    .document(preferenceManager.getString(Constants.KEY_ORDER))
                                                                                                                                                                    .delete()
                                                                                                                                                                    .addOnSuccessListener(aVoid121111 ->
                                                                                                                                                                            FirebaseFirestore.getInstance()
                                                                                                                                                                                    .collection(Constants.KEY_COLLECTION_CITIES)
                                                                                                                                                                                    .document(preferenceManager.getString(Constants.KEY_CITY))
                                                                                                                                                                                    .collection(Constants.KEY_COLLECTION_LOCALITIES)
                                                                                                                                                                                    .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                                                                                                                                                                                    .collection(Constants.KEY_COLLECTION_STORES)
                                                                                                                                                                                    .document(preferenceManager.getString(Constants.KEY_STORE_ID))
                                                                                                                                                                                    .collection(Constants.KEY_COLLECTION_PENDING_ORDERS)
                                                                                                                                                                                    .document(preferenceManager.getString(Constants.KEY_ORDER))
                                                                                                                                                                                    .collection(Constants.KEY_COLLECTION_ORDER_ITEMS)
                                                                                                                                                                                    .get()
                                                                                                                                                                                    .addOnSuccessListener(queryDocumentSnapshots3 -> {
                                                                                                                                                                                        for (QueryDocumentSnapshot deleteDocumentSnapshot2 : queryDocumentSnapshots3) {
                                                                                                                                                                                            deleteDocumentSnapshot2.getReference().delete()
                                                                                                                                                                                                    .addOnSuccessListener(aVoid1211111 ->
                                                                                                                                                                                                            FirebaseFirestore.getInstance()
                                                                                                                                                                                                                    .collection(Constants.KEY_COLLECTION_CITIES)
                                                                                                                                                                                                                    .document(preferenceManager.getString(Constants.KEY_CITY))
                                                                                                                                                                                                                    .collection(Constants.KEY_COLLECTION_LOCALITIES)
                                                                                                                                                                                                                    .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                                                                                                                                                                                                                    .collection(Constants.KEY_COLLECTION_STORES)
                                                                                                                                                                                                                    .document(preferenceManager.getString(Constants.KEY_STORE_ID))
                                                                                                                                                                                                                    .collection(Constants.KEY_COLLECTION_PENDING_ORDERS)
                                                                                                                                                                                                                    .document(preferenceManager.getString(Constants.KEY_ORDER))
                                                                                                                                                                                                                    .delete()
                                                                                                                                                                                                                    .addOnSuccessListener(aVoid12111111 -> {
                                                                                                                                                                                                                        progressDialog.dismiss();
                                                                                                                                                                                                                        onBackPressed();
                                                                                                                                                                                                                    }).addOnFailureListener(e -> {
                                                                                                                                                                                                                progressDialog.dismiss();
                                                                                                                                                                                                                Alerter.create(OrderDetailsActivity.this)
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
                                                                                                                                                                                                progressDialog.dismiss();
                                                                                                                                                                                                Alerter.create(OrderDetailsActivity.this)
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
                                                                                                                                                                                    }).addOnFailureListener(e -> {
                                                                                                                                                                                progressDialog.dismiss();
                                                                                                                                                                                Alerter.create(OrderDetailsActivity.this)
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
                                                                                                                                                                progressDialog.dismiss();
                                                                                                                                                                Alerter.create(OrderDetailsActivity.this)
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
                                                                                                                                                progressDialog.dismiss();
                                                                                                                                                Alerter.create(OrderDetailsActivity.this)
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
                                                                                                                                    }).addOnFailureListener(e -> {
                                                                                                                                progressDialog.dismiss();
                                                                                                                                Alerter.create(OrderDetailsActivity.this)
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
                                                                                                                progressDialog.dismiss();
                                                                                                                Alerter.create(OrderDetailsActivity.this)
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
                                                                                                progressDialog.dismiss();
                                                                                                Alerter.create(OrderDetailsActivity.this)
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
                                                                                progressDialog.dismiss();
                                                                                Alerter.create(OrderDetailsActivity.this)
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
                                                                    }).addOnFailureListener(e -> {
                                                                progressDialog.dismiss();
                                                                Alerter.create(OrderDetailsActivity.this)
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
                                                progressDialog.dismiss();
                                                Alerter.create(OrderDetailsActivity.this)
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
                                        return true;

                                    default:
                                        return false;
                                }
                            });
                            popupMenu.show();
                        });

                        orderPlacedTime.setText(order_placing_time);
                        textDeliveredOn.setText("Delivered on");
                        orderCompletionTime.setText("Order isn't delivered yet.");
                    } else if (preferenceManager.getString(Constants.KEY_ORDER_TYPE).equals("Completed")) {
                        orderStatus.setText("Delivered");
                        orderStatus.setTextColor(getColor(R.color.successColor));

                        updateStatus.setVisibility(View.GONE);

                        orderPlacedTime.setText(order_placing_time);
                        textDeliveredOn.setText("Delivered on");
                        orderCompletionTime.setText(order_completion_time);
                    } else if (preferenceManager.getString(Constants.KEY_ORDER_TYPE).equals("Cancelled")) {
                        orderStatus.setText("Cancelled");
                        orderStatus.setTextColor(getColor(R.color.errorColor));

                        updateStatus.setVisibility(View.GONE);

                        orderPlacedTime.setText(order_placing_time);
                        textDeliveredOn.setText("Cancelled on");
                        orderCompletionTime.setText(order_cancellation_time);
                    }

                    ///////////////////////////// Names & Address //////////////////////////////

                    String customer_name = documentSnapshot.getString(Constants.KEY_ORDER_CUSTOMER_NAME);
                    String customer_mobile = documentSnapshot.getString(Constants.KEY_ORDER_CUSTOMER_MOBILE);
                    String customer_address = documentSnapshot.getString(Constants.KEY_ORDER_DELIVERY_ADDRESS);
                    double delivery_distance = documentSnapshot.getDouble(Constants.KEY_ORDER_DELIVERY_DISTANCE);

                    customerName.setText(customer_name);
                    customerMobile.setText(customer_mobile);
                    customerAddress.setText(customer_address);
                    if (delivery_distance <= 1) {
                        deliveryDistance.setText(String.format("Distance : %s km", delivery_distance));
                    } else if (delivery_distance > 1) {
                        deliveryDistance.setText(String.format("Distance : %s kms", delivery_distance));
                    }

                    ///////////////////////////// Order Summary ////////////////////////////////

                    long no_of_items = documentSnapshot.getLong(Constants.KEY_ORDER_NO_OF_ITEMS);
                    double total_mrp = documentSnapshot.getDouble(Constants.KEY_ORDER_TOTAL_MRP);
                    double discount_amount = documentSnapshot.getDouble(Constants.KEY_ORDER_TOTAL_DISCOUNT);
                    double coupon_discount = documentSnapshot.getDouble(Constants.KEY_ORDER_COUPON_DISCOUNT);
                    double delivery_charges = documentSnapshot.getDouble(Constants.KEY_ORDER_DELIVERY_CHARGES);
                    double tip_amount = documentSnapshot.getDouble(Constants.KEY_ORDER_TIP_AMOUNT);
                    double convenience_fee = documentSnapshot.getDouble(Constants.KEY_ORDER_CONVENIENCE_FEE);
                    double total_payable = documentSnapshot.getDouble(Constants.KEY_ORDER_TOTAL_PAYABLE);

                    if (no_of_items == 1) {
                        orderNoOfItems.setText(String.format("(%d Item)", no_of_items));
                    } else {
                        orderNoOfItems.setText(String.format("(%d Items)", no_of_items));
                    }

                    totalMRP.setText(String.format("₹ %s", total_mrp));
                    discountAmount.setText(String.format("₹ %s", discount_amount));

                    if (coupon_discount == 0) {
                        couponDiscount.setText("No coupon applied");
                        couponDiscount.setTextColor(getColor(R.color.colorTextDark));
                    } else {
                        couponDiscount.setText(String.format("₹ %s", coupon_discount));
                        couponDiscount.setTextColor(getColor(R.color.successColor));
                    }

                    if (delivery_charges == 0) {
                        deliveryCharges.setText("FREE");
                        deliveryCharges.setTextColor(getColor(R.color.successColor));
                    } else {
                        deliveryCharges.setText(String.format("₹ %s", delivery_charges));
                        deliveryCharges.setTextColor(getColor(R.color.errorColor));
                    }

                    if (tip_amount == 0) {
                        textTipAdded.setVisibility(View.GONE);
                        tipAmount.setVisibility(View.GONE);
                    } else {
                        textTipAdded.setVisibility(View.VISIBLE);
                        tipAmount.setVisibility(View.VISIBLE);
                        tipAmount.setText(String.format("+ ₹ %s", tip_amount));
                    }

                    if (convenience_fee == 0) {
                        convenienceFee.setText("FREE");
                        convenienceFee.setTextColor(getColor(R.color.successColor));
                    } else {
                        convenienceFee.setText(String.format("+ ₹ %s", convenience_fee));
                        convenienceFee.setTextColor(getColor(R.color.errorColor));
                    }

                    totalPayable.setText(String.format("₹ %s", total_payable));

                    ////////////////////////////// Order Details ///////////////////////////////////

                    String instruction = documentSnapshot.getString(Constants.KEY_ORDER_INSTRUCTIONS);
                    String payment_method = documentSnapshot.getString(Constants.KEY_ORDER_PAYMENT_MODE);

                    if (instruction.equals("") || instruction.isEmpty()) {
                        orderInstructions.setText("No instructions");
                    } else {
                        orderInstructions.setText(instruction);
                    }

                    if (payment_method.equals("Online Payment")) {
                        orderPayment.setText("Pre-paid Order");
                        paymentMethod.setText(payment_method);
                    } else if (payment_method.equals("Pay on Delivery")) {
                        orderPayment.setText("Post-paid Order");
                        paymentMethod.setText(payment_method);
                    }

                    /////////////////////////////// Cancel Button //////////////////////////////////

                    if (preferenceManager.getString(Constants.KEY_ORDER_TYPE).equals("Pending")) {
                        cancelBtnContainer.setCardBackgroundColor(getColor(R.color.errorColor));
                        cancelBtn.setEnabled(true);
                        cancelBtn.setOnClickListener(v -> {
                            if (!isConnectedToInternet(OrderDetailsActivity.this)) {
                                showConnectToInternetDialog();
                                return;
                            } else {
                                MaterialDialog materialDialog = new MaterialDialog.Builder(OrderDetailsActivity.this)
                                        .setMessage("Are you sure you want to cancel this order?")
                                        .setCancelable(false)
                                        .setPositiveButton("Yes", R.drawable.ic_dialog_okay, (dialogInterface, which) -> {
                                            dialogInterface.dismiss();
                                            progressDialog.show();
                                            Calendar calendar = Calendar.getInstance();
                                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEEE , dd-MMM-yyyy hh:mm a");
                                            String currentTime = simpleDateFormat.format(calendar.getTime());

                                            HashMap<String, Object> newCancelledOrder = new HashMap<>();
                                            newCancelledOrder.put(Constants.KEY_ORDER_ID, documentSnapshot.get(Constants.KEY_ORDER_ID));
                                            newCancelledOrder.put(Constants.KEY_ORDER_BY_USERID, documentSnapshot.get(Constants.KEY_ORDER_BY_USERID));
                                            newCancelledOrder.put(Constants.KEY_ORDER_BY_USERNAME, documentSnapshot.get(Constants.KEY_ORDER_BY_USERNAME));
                                            newCancelledOrder.put(Constants.KEY_ORDER_FROM_STOREID, documentSnapshot.get(Constants.KEY_ORDER_FROM_STOREID));
                                            newCancelledOrder.put(Constants.KEY_ORDER_FROM_STORENAME, documentSnapshot.get(Constants.KEY_ORDER_FROM_STORENAME));
                                            newCancelledOrder.put(Constants.KEY_ORDER_CUSTOMER_NAME, documentSnapshot.get(Constants.KEY_ORDER_CUSTOMER_NAME));
                                            newCancelledOrder.put(Constants.KEY_ORDER_CUSTOMER_MOBILE, documentSnapshot.get(Constants.KEY_ORDER_CUSTOMER_MOBILE));
                                            newCancelledOrder.put(Constants.KEY_ORDER_DELIVERY_LOCATION, documentSnapshot.get(Constants.KEY_ORDER_DELIVERY_LOCATION));
                                            newCancelledOrder.put(Constants.KEY_ORDER_DELIVERY_ADDRESS, documentSnapshot.get(Constants.KEY_ORDER_DELIVERY_ADDRESS));
                                            newCancelledOrder.put(Constants.KEY_ORDER_DELIVERY_LATITUDE, documentSnapshot.get(Constants.KEY_ORDER_DELIVERY_LATITUDE));
                                            newCancelledOrder.put(Constants.KEY_ORDER_DELIVERY_LONGITUDE, documentSnapshot.get(Constants.KEY_ORDER_DELIVERY_LONGITUDE));
                                            newCancelledOrder.put(Constants.KEY_ORDER_DELIVERY_DISTANCE, documentSnapshot.get(Constants.KEY_ORDER_DELIVERY_DISTANCE));
                                            newCancelledOrder.put(Constants.KEY_ORDER_NO_OF_ITEMS, documentSnapshot.get(Constants.KEY_ORDER_NO_OF_ITEMS));
                                            newCancelledOrder.put(Constants.KEY_ORDER_TOTAL_MRP, documentSnapshot.get(Constants.KEY_ORDER_TOTAL_MRP));
                                            newCancelledOrder.put(Constants.KEY_ORDER_TOTAL_RETAIL_PRICE, documentSnapshot.get(Constants.KEY_ORDER_TOTAL_RETAIL_PRICE));
                                            newCancelledOrder.put(Constants.KEY_ORDER_COUPON_APPLIED, documentSnapshot.get(Constants.KEY_ORDER_COUPON_APPLIED));
                                            newCancelledOrder.put(Constants.KEY_ORDER_COUPON_DISCOUNT, documentSnapshot.get(Constants.KEY_ORDER_COUPON_DISCOUNT));
                                            newCancelledOrder.put(Constants.KEY_ORDER_TOTAL_DISCOUNT, documentSnapshot.get(Constants.KEY_ORDER_TOTAL_DISCOUNT));
                                            newCancelledOrder.put(Constants.KEY_ORDER_DELIVERY_CHARGES, documentSnapshot.get(Constants.KEY_ORDER_DELIVERY_CHARGES));
                                            newCancelledOrder.put(Constants.KEY_ORDER_TIP_AMOUNT, documentSnapshot.get(Constants.KEY_ORDER_TIP_AMOUNT));
                                            newCancelledOrder.put(Constants.KEY_ORDER_SUB_TOTAL, documentSnapshot.get(Constants.KEY_ORDER_SUB_TOTAL));
                                            newCancelledOrder.put(Constants.KEY_ORDER_PAYMENT_MODE, documentSnapshot.get(Constants.KEY_ORDER_PAYMENT_MODE));
                                            newCancelledOrder.put(Constants.KEY_ORDER_CONVENIENCE_FEE, documentSnapshot.get(Constants.KEY_ORDER_CONVENIENCE_FEE));
                                            newCancelledOrder.put(Constants.KEY_ORDER_TOTAL_PAYABLE, documentSnapshot.get(Constants.KEY_ORDER_TOTAL_PAYABLE));
                                            newCancelledOrder.put(Constants.KEY_ORDER_INSTRUCTIONS, documentSnapshot.get(Constants.KEY_ORDER_INSTRUCTIONS));
                                            newCancelledOrder.put(Constants.KEY_ORDER_STATUS, "Cancelled");
                                            newCancelledOrder.put(Constants.KEY_ORDER_PLACED_TIME, documentSnapshot.get(Constants.KEY_ORDER_PLACED_TIME));
                                            newCancelledOrder.put(Constants.KEY_ORDER_COMPLETION_TIME, "");
                                            newCancelledOrder.put(Constants.KEY_ORDER_CANCELLATION_TIME, currentTime);
                                            newCancelledOrder.put(Constants.KEY_ORDER_TIMESTAMP, FieldValue.serverTimestamp());

                                            FirebaseFirestore.getInstance()
                                                    .collection(Constants.KEY_COLLECTION_CITIES)
                                                    .document(preferenceManager.getString(Constants.KEY_CITY))
                                                    .collection(Constants.KEY_COLLECTION_LOCALITIES)
                                                    .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                                                    .collection(Constants.KEY_COLLECTION_STORES)
                                                    .document(preferenceManager.getString(Constants.KEY_STORE_ID))
                                                    .collection(Constants.KEY_COLLECTION_CANCELLED_ORDERS)
                                                    .document(preferenceManager.getString(Constants.KEY_ORDER))
                                                    .set(newCancelledOrder)
                                                    .addOnSuccessListener(aVoid ->
                                                            FirebaseFirestore.getInstance()
                                                                    .collection(Constants.KEY_COLLECTION_CITIES)
                                                                    .document(preferenceManager.getString(Constants.KEY_CITY))
                                                                    .collection(Constants.KEY_COLLECTION_LOCALITIES)
                                                                    .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                                                                    .collection(Constants.KEY_COLLECTION_STORES)
                                                                    .document(preferenceManager.getString(Constants.KEY_STORE_ID))
                                                                    .collection(Constants.KEY_COLLECTION_PENDING_ORDERS)
                                                                    .document(preferenceManager.getString(Constants.KEY_ORDER))
                                                                    .collection(Constants.KEY_COLLECTION_ORDER_ITEMS)
                                                                    .get()
                                                                    .addOnSuccessListener(queryDocumentSnapshots1 -> {
                                                                        for (QueryDocumentSnapshot itemDocumentSnapshot : queryDocumentSnapshots1) {
                                                                            HashMap<String, Object> newCancelledOrderItem = new HashMap<>();
                                                                            newCancelledOrderItem.put(Constants.KEY_ORDER_ITEM_ID, itemDocumentSnapshot.getId());
                                                                            newCancelledOrderItem.put(Constants.KEY_ORDER_ITEM_PRODUCT_ID, itemDocumentSnapshot.get(Constants.KEY_ORDER_ITEM_PRODUCT_ID));
                                                                            newCancelledOrderItem.put(Constants.KEY_ORDER_ITEM_PRODUCT_STORE_ID, itemDocumentSnapshot.get(Constants.KEY_ORDER_ITEM_PRODUCT_STORE_ID));
                                                                            newCancelledOrderItem.put(Constants.KEY_ORDER_ITEM_PRODUCT_STORE_NAME, itemDocumentSnapshot.get(Constants.KEY_ORDER_ITEM_PRODUCT_STORE_NAME));
                                                                            newCancelledOrderItem.put(Constants.KEY_ORDER_ITEM_PRODUCT_CATEGORY, itemDocumentSnapshot.get(Constants.KEY_ORDER_ITEM_PRODUCT_CATEGORY));
                                                                            newCancelledOrderItem.put(Constants.KEY_ORDER_ITEM_PRODUCT_IMAGE, itemDocumentSnapshot.get(Constants.KEY_ORDER_ITEM_PRODUCT_IMAGE));
                                                                            newCancelledOrderItem.put(Constants.KEY_ORDER_ITEM_PRODUCT_NAME, itemDocumentSnapshot.get(Constants.KEY_ORDER_ITEM_PRODUCT_NAME));
                                                                            newCancelledOrderItem.put(Constants.KEY_ORDER_ITEM_PRODUCT_UNIT, itemDocumentSnapshot.get(Constants.KEY_ORDER_ITEM_PRODUCT_UNIT));
                                                                            newCancelledOrderItem.put(Constants.KEY_ORDER_ITEM_PRODUCT_MRP, itemDocumentSnapshot.get(Constants.KEY_ORDER_ITEM_PRODUCT_MRP));
                                                                            newCancelledOrderItem.put(Constants.KEY_ORDER_ITEM_PRODUCT_RETAIL_PRICE, itemDocumentSnapshot.get(Constants.KEY_ORDER_ITEM_PRODUCT_RETAIL_PRICE));
                                                                            newCancelledOrderItem.put(Constants.KEY_ORDER_ITEM_PRODUCT_QUANTITY, itemDocumentSnapshot.get(Constants.KEY_ORDER_ITEM_PRODUCT_QUANTITY));
                                                                            newCancelledOrderItem.put(Constants.KEY_ORDER_ITEM_TIMESTAMP, FieldValue.serverTimestamp());

                                                                            FirebaseFirestore.getInstance()
                                                                                    .collection(Constants.KEY_COLLECTION_CITIES)
                                                                                    .document(preferenceManager.getString(Constants.KEY_CITY))
                                                                                    .collection(Constants.KEY_COLLECTION_LOCALITIES)
                                                                                    .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                                                                                    .collection(Constants.KEY_COLLECTION_STORES)
                                                                                    .document(preferenceManager.getString(Constants.KEY_STORE_ID))
                                                                                    .collection(Constants.KEY_COLLECTION_CANCELLED_ORDERS)
                                                                                    .document(preferenceManager.getString(Constants.KEY_ORDER))
                                                                                    .collection(Constants.KEY_COLLECTION_ORDER_ITEMS)
                                                                                    .document(itemDocumentSnapshot.getId())
                                                                                    .set(newCancelledOrderItem)
                                                                                    .addOnSuccessListener(aVoid13 ->
                                                                                            FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                                                                                                    .document(documentSnapshot.getString(Constants.KEY_ORDER_BY_USERID))
                                                                                                    .collection(Constants.KEY_COLLECTION_CANCELLED_ORDERS)
                                                                                                    .document(preferenceManager.getString(Constants.KEY_ORDER))
                                                                                                    .set(newCancelledOrder)
                                                                                                    .addOnSuccessListener(aVoid131 ->
                                                                                                            FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                                                                                                                    .document(documentSnapshot.getString(Constants.KEY_ORDER_BY_USERID))
                                                                                                                    .collection(Constants.KEY_COLLECTION_CANCELLED_ORDERS)
                                                                                                                    .document(preferenceManager.getString(Constants.KEY_ORDER))
                                                                                                                    .collection(Constants.KEY_COLLECTION_ORDER_ITEMS)
                                                                                                                    .document(itemDocumentSnapshot.getId())
                                                                                                                    .set(newCancelledOrderItem)
                                                                                                                    .addOnSuccessListener(aVoid1311 ->
                                                                                                                            FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                                                                                                                                    .document(documentSnapshot.getString(Constants.KEY_ORDER_BY_USERID))
                                                                                                                                    .collection(Constants.KEY_COLLECTION_PENDING_ORDERS)
                                                                                                                                    .document(preferenceManager.getString(Constants.KEY_ORDER))
                                                                                                                                    .collection(Constants.KEY_COLLECTION_ORDER_ITEMS)
                                                                                                                                    .get()
                                                                                                                                    .addOnSuccessListener(queryDocumentSnapshots2 -> {
                                                                                                                                        for (QueryDocumentSnapshot deleteDocumentSnapshot1 : queryDocumentSnapshots2) {
                                                                                                                                            deleteDocumentSnapshot1.getReference().delete()
                                                                                                                                                    .addOnSuccessListener(aVoid13111 ->
                                                                                                                                                            FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                                                                                                                                                                    .document(documentSnapshot.getString(Constants.KEY_ORDER_BY_USERID))
                                                                                                                                                                    .collection(Constants.KEY_COLLECTION_PENDING_ORDERS)
                                                                                                                                                                    .document(preferenceManager.getString(Constants.KEY_ORDER))
                                                                                                                                                                    .delete()
                                                                                                                                                                    .addOnSuccessListener(aVoid131111 ->
                                                                                                                                                                            FirebaseFirestore.getInstance()
                                                                                                                                                                                    .collection(Constants.KEY_COLLECTION_CITIES)
                                                                                                                                                                                    .document(preferenceManager.getString(Constants.KEY_CITY))
                                                                                                                                                                                    .collection(Constants.KEY_COLLECTION_LOCALITIES)
                                                                                                                                                                                    .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                                                                                                                                                                                    .collection(Constants.KEY_COLLECTION_STORES)
                                                                                                                                                                                    .document(preferenceManager.getString(Constants.KEY_STORE_ID))
                                                                                                                                                                                    .collection(Constants.KEY_COLLECTION_PENDING_ORDERS)
                                                                                                                                                                                    .document(preferenceManager.getString(Constants.KEY_ORDER))
                                                                                                                                                                                    .collection(Constants.KEY_COLLECTION_ORDER_ITEMS)
                                                                                                                                                                                    .get()
                                                                                                                                                                                    .addOnSuccessListener(queryDocumentSnapshots3 -> {
                                                                                                                                                                                        for (QueryDocumentSnapshot deleteDocumentSnapshot2 : queryDocumentSnapshots3) {
                                                                                                                                                                                            deleteDocumentSnapshot2.getReference().delete()
                                                                                                                                                                                                    .addOnSuccessListener(aVoid1311111 ->
                                                                                                                                                                                                            FirebaseFirestore.getInstance()
                                                                                                                                                                                                                    .collection(Constants.KEY_COLLECTION_CITIES)
                                                                                                                                                                                                                    .document(preferenceManager.getString(Constants.KEY_CITY))
                                                                                                                                                                                                                    .collection(Constants.KEY_COLLECTION_LOCALITIES)
                                                                                                                                                                                                                    .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                                                                                                                                                                                                                    .collection(Constants.KEY_COLLECTION_STORES)
                                                                                                                                                                                                                    .document(preferenceManager.getString(Constants.KEY_STORE_ID))
                                                                                                                                                                                                                    .collection(Constants.KEY_COLLECTION_PENDING_ORDERS)
                                                                                                                                                                                                                    .document(preferenceManager.getString(Constants.KEY_ORDER))
                                                                                                                                                                                                                    .delete()
                                                                                                                                                                                                                    .addOnSuccessListener(aVoid13111111 -> {
                                                                                                                                                                                                                        progressDialog.dismiss();
                                                                                                                                                                                                                        onBackPressed();
                                                                                                                                                                                                                    }).addOnFailureListener(e -> {
                                                                                                                                                                                                                progressDialog.dismiss();
                                                                                                                                                                                                                Alerter.create(OrderDetailsActivity.this)
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
                                                                                                                                                                                                progressDialog.dismiss();
                                                                                                                                                                                                Alerter.create(OrderDetailsActivity.this)
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
                                                                                                                                                                                    }).addOnFailureListener(e -> {
                                                                                                                                                                                progressDialog.dismiss();
                                                                                                                                                                                Alerter.create(OrderDetailsActivity.this)
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
                                                                                                                                                                progressDialog.dismiss();
                                                                                                                                                                Alerter.create(OrderDetailsActivity.this)
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
                                                                                                                                                progressDialog.dismiss();
                                                                                                                                                Alerter.create(OrderDetailsActivity.this)
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
                                                                                                                                    }).addOnFailureListener(e -> {
                                                                                                                                progressDialog.dismiss();
                                                                                                                                Alerter.create(OrderDetailsActivity.this)
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
                                                                                                                progressDialog.dismiss();
                                                                                                                Alerter.create(OrderDetailsActivity.this)
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
                                                                                                progressDialog.dismiss();
                                                                                                Alerter.create(OrderDetailsActivity.this)
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
                                                                                progressDialog.dismiss();
                                                                                Alerter.create(OrderDetailsActivity.this)
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
                                                                    }).addOnFailureListener(e -> {
                                                                progressDialog.dismiss();
                                                                Alerter.create(OrderDetailsActivity.this)
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
                                                progressDialog.dismiss();
                                                Alerter.create(OrderDetailsActivity.this)
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
                                        })
                                        .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss())
                                        .build();
                                materialDialog.show();
                            }
                        });
                    } else {
                        cancelBtnContainer.setCardBackgroundColor(getColor(R.color.colorInactive));
                        cancelBtn.setEnabled(false);
                    }
                } else {
                    onBackPressed();
                }
            }
        });
    }

    ///////////////////////////////////////// LoadOrderItems ///////////////////////////////////////

    private void loadOrderItems() {
        Query query = null;

        if (preferenceManager.getString(Constants.KEY_ORDER_TYPE).equals("Pending")) {
            query = FirebaseFirestore.getInstance()
                    .collection(Constants.KEY_COLLECTION_CITIES)
                    .document(preferenceManager.getString(Constants.KEY_CITY))
                    .collection(Constants.KEY_COLLECTION_LOCALITIES)
                    .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                    .collection(Constants.KEY_COLLECTION_STORES)
                    .document(preferenceManager.getString(Constants.KEY_STORE_ID))
                    .collection(Constants.KEY_COLLECTION_PENDING_ORDERS)
                    .document(preferenceManager.getString(Constants.KEY_ORDER))
                    .collection(Constants.KEY_COLLECTION_ORDER_ITEMS)
                    .orderBy(Constants.KEY_ORDER_ITEM_TIMESTAMP, Query.Direction.ASCENDING);
        } else if (preferenceManager.getString(Constants.KEY_ORDER_TYPE).equals("Completed")) {
            query = FirebaseFirestore.getInstance()
                    .collection(Constants.KEY_COLLECTION_CITIES)
                    .document(preferenceManager.getString(Constants.KEY_CITY))
                    .collection(Constants.KEY_COLLECTION_LOCALITIES)
                    .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                    .collection(Constants.KEY_COLLECTION_STORES)
                    .document(preferenceManager.getString(Constants.KEY_STORE_ID))
                    .collection(Constants.KEY_COLLECTION_COMPLETED_ORDERS)
                    .document(preferenceManager.getString(Constants.KEY_ORDER))
                    .collection(Constants.KEY_COLLECTION_ORDER_ITEMS)
                    .orderBy(Constants.KEY_ORDER_ITEM_TIMESTAMP, Query.Direction.ASCENDING);
        } else if (preferenceManager.getString(Constants.KEY_ORDER_TYPE).equals("Cancelled")) {
            query = FirebaseFirestore.getInstance()
                    .collection(Constants.KEY_COLLECTION_CITIES)
                    .document(preferenceManager.getString(Constants.KEY_CITY))
                    .collection(Constants.KEY_COLLECTION_LOCALITIES)
                    .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                    .collection(Constants.KEY_COLLECTION_STORES)
                    .document(preferenceManager.getString(Constants.KEY_STORE_ID))
                    .collection(Constants.KEY_COLLECTION_CANCELLED_ORDERS)
                    .document(preferenceManager.getString(Constants.KEY_ORDER))
                    .collection(Constants.KEY_COLLECTION_ORDER_ITEMS)
                    .orderBy(Constants.KEY_ORDER_ITEM_TIMESTAMP, Query.Direction.ASCENDING);
        }

        FirestoreRecyclerOptions<OrderItem> options = new FirestoreRecyclerOptions.Builder<OrderItem>()
                .setLifecycleOwner(OrderDetailsActivity.this)
                .setQuery(query, OrderItem.class)
                .build();

        orderItemAdapter = new FirestoreRecyclerAdapter<OrderItem, OrderItemViewHolder>(options) {

            @NonNull
            @Override
            public OrderItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_order_item, parent, false);
                return new OrderItemViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull OrderItemViewHolder holder, int position, @NonNull OrderItem model) {
                Glide.with(holder.orderItemProductImage.getContext()).load(model.getOrderItemProductImage())
                        .placeholder(R.drawable.thumbnail).centerCrop().into(holder.orderItemProductImage);

                holder.orderItemProductName.setText(model.getOrderItemProductName());

                holder.orderItemProductQuantity.setText(String.valueOf(model.getOrderItemProductQuantity()));
                holder.orderItemProductPrice.setText(String.format("₹ %s", model.getOrderItemProductRetailPrice()));

                if (model.getOrderItemProductRetailPrice() == model.getOrderItemProductMRP()) {
                    holder.orderItemProductMRP.setVisibility(View.GONE);
                } else {
                    holder.orderItemProductMRP.setVisibility(View.VISIBLE);
                    holder.orderItemProductMRP.setText(String.format("₹ %s", model.getOrderItemProductMRP()));
                    holder.orderItemProductMRP.setPaintFlags(holder.orderItemProductMRP.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                }

                double total_price = Math.round((model.getOrderItemProductQuantity() * model.getOrderItemProductRetailPrice()) * 100.0) / 100.0;

                holder.orderItemProductTotalPrice.setText(String.format("₹ %s", total_price));
            }

            @Override
            public void onError(@NonNull FirebaseFirestoreException e) {
                super.onError(e);
                Alerter.create(OrderDetailsActivity.this)
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
            }
        };

        orderItemAdapter.notifyDataSetChanged();

        recyclerOrderItems.setLayoutManager(new LinearLayoutManager(OrderDetailsActivity.this));
        recyclerOrderItems.getLayoutManager().setAutoMeasureEnabled(true);
        recyclerOrderItems.setNestedScrollingEnabled(false);
        recyclerOrderItems.setHasFixedSize(false);
        recyclerOrderItems.setAdapter(orderItemAdapter);
    }

    //////////////////////////////////// OrderItemViewHolder ///////////////////////////////////////

    public static class OrderItemViewHolder extends RecyclerView.ViewHolder {

        RoundedImageView orderItemProductImage;
        TextView orderItemProductName, orderItemProductQuantity, orderItemProductPrice, orderItemProductMRP, orderItemProductTotalPrice;

        public OrderItemViewHolder(@NonNull View itemView) {
            super(itemView);

            orderItemProductImage = itemView.findViewById(R.id.order_item_product_image);
            orderItemProductName = itemView.findViewById(R.id.order_item_product_name);
            orderItemProductQuantity = itemView.findViewById(R.id.order_item_product_quantity);
            orderItemProductPrice = itemView.findViewById(R.id.order_item_product_price);
            orderItemProductMRP = itemView.findViewById(R.id.order_item_product_mrp);
            orderItemProductTotalPrice = itemView.findViewById(R.id.order_item_product_total_price);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean isConnectedToInternet(OrderDetailsActivity orderDetailsActivity) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) orderDetailsActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (null != networkInfo &&
                (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(OrderDetailsActivity.this)
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
        CustomIntent.customType(OrderDetailsActivity.this, "up-to-bottom");
    }
}