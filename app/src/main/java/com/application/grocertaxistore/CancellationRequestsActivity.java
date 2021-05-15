package com.application.grocertaxistore;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.application.grocertaxistore.Model.CancelRequest;
import com.application.grocertaxistore.Utilities.Constants;
import com.application.grocertaxistore.Utilities.PreferenceManager;
import com.baoyz.widget.PullRefreshLayout;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.tapadoo.alerter.Alerter;

import dmax.dialog.SpotsDialog;
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;
import maes.tech.intentanim.CustomIntent;

public class CancellationRequestsActivity extends AppCompatActivity {

    private ImageView closeBtn;
    private RecyclerView recyclerRequests;
    private ConstraintLayout layoutContent, layoutEmpty, layoutNoInternet, retryBtn;
    private PullRefreshLayout pullRefreshLayout;
    private ShimmerFrameLayout shimmerLayout;

    private CollectionReference requestsRef;
    private RequestAdapter requestAdapter;

    private PreferenceManager preferenceManager;
    private AlertDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cancellation_requests);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (!preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(CancellationRequestsActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(CancellationRequestsActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorBackground));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        progressDialog = new SpotsDialog.Builder().setContext(CancellationRequestsActivity.this)
                .setMessage("Hold on..")
                .setCancelable(false)
                .setTheme(R.style.SpotsDialog)
                .build();

        ////////////////////////////////////////////////////////////////////////////////////////////

        closeBtn = findViewById(R.id.close_btn);
        recyclerRequests = findViewById(R.id.recycler_requests);
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
        if (!isConnectedToInternet(CancellationRequestsActivity.this)) {
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
        requestsRef = FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_CITIES)
                .document(preferenceManager.getString(Constants.KEY_CITY))
                .collection(Constants.KEY_COLLECTION_LOCALITIES)
                .document(preferenceManager.getString(Constants.KEY_LOCALITY))
                .collection(Constants.KEY_COLLECTION_STORES)
                .document(preferenceManager.getString(Constants.KEY_STORE_ID))
                .collection(Constants.KEY_COLLECTION_CANCELLATION_REQUESTS);
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

        closeBtn.setOnClickListener(v -> onBackPressed());

        ////////////////////////////////////////////////////////////////////////////////////////////

        loadCancelRequests();
    }

    ///////////////////////////////////// LoadCartItems ////////////////////////////////////////////

    private void loadCancelRequests() {
        shimmerLayout.setVisibility(View.VISIBLE);
        shimmerLayout.startShimmer();

        Query query = requestsRef.orderBy("requestTimestamp", Query.Direction.DESCENDING);
        FirestoreRecyclerOptions<CancelRequest> options = new FirestoreRecyclerOptions.Builder<CancelRequest>()
                .setLifecycleOwner(CancellationRequestsActivity.this)
                .setQuery(query, CancelRequest.class)
                .build();

        requestAdapter = new RequestAdapter(options);
        requestAdapter.notifyDataSetChanged();

        recyclerRequests.setHasFixedSize(true);
        recyclerRequests.setLayoutManager(new LinearLayoutManager(CancellationRequestsActivity.this));
        recyclerRequests.setAdapter(requestAdapter);

        recyclerRequests.getAdapter().notifyDataSetChanged();

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                if (direction == ItemTouchHelper.RIGHT) {
                    if (!isConnectedToInternet(CancellationRequestsActivity.this)) {
                        showConnectToInternetDialog();
                        return;
                    } else {
                        progressDialog.show();
                        requestAdapter.deleteItem(viewHolder.getAdapterPosition());
                        recyclerRequests.getAdapter().notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

                new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        .addBackgroundColor(ContextCompat.getColor(CancellationRequestsActivity.this, R.color.errorColor))
                        .addSwipeRightActionIcon(R.drawable.ic_dialog_remove)
                        .setSwipeRightActionIconTint(getColor(R.color.colorIconLight))
                        .create()
                        .decorate();

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        }).attachToRecyclerView(recyclerRequests);
    }

    /////////////////////////////////////// RequestAdapter /////////////////////////////////////////

    public class RequestAdapter extends FirestoreRecyclerAdapter<CancelRequest, RequestAdapter.RequestViewHolder> {

        public RequestAdapter(@NonNull FirestoreRecyclerOptions<CancelRequest> options) {
            super(options);
        }

        @NonNull
        @Override
        public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.layout_cancellation_request, parent, false);
            return new RequestViewHolder(itemView);
        }

        @Override
        protected void onBindViewHolder(@NonNull RequestViewHolder holder, int position, @NonNull CancelRequest model) {
            holder.orderID.setText(model.getOrderID());
            holder.requestTime.setText("Requested on " + model.getRequestTime());
        }

        @Override
        public void onDataChanged() {
            super.onDataChanged();

            pullRefreshLayout.setRefreshing(false);
            shimmerLayout.stopShimmer();
            shimmerLayout.setVisibility(View.GONE);

            if (getItemCount() == 0) {
                layoutEmpty.setVisibility(View.VISIBLE);
            } else {
                layoutEmpty.setVisibility(View.GONE);
            }
        }

        @Override
        public void onError(@NonNull FirebaseFirestoreException e) {
            super.onError(e);

            pullRefreshLayout.setRefreshing(false);
            shimmerLayout.stopShimmer();
            shimmerLayout.setVisibility(View.GONE);
            Alerter.create(CancellationRequestsActivity.this)
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

        public void deleteItem(int position) {
            getSnapshots().getSnapshot(position).getReference().delete()
                    .addOnSuccessListener(aVoid -> {
                        progressDialog.dismiss();
                        Alerter.create(CancellationRequestsActivity.this)
                                .setText("Ahan! Successfully removed.")
                                .setTextAppearance(R.style.AlertText)
                                .setBackgroundColorRes(R.color.infoColor)
                                .setIcon(R.drawable.ic_dialog_okay)
                                .setDuration(3000)
                                .enableIconPulse(true)
                                .enableVibration(true)
                                .disableOutsideTouch()
                                .enableProgress(true)
                                .setProgressColorInt(getColor(android.R.color.white))
                                .show();
                    }).addOnFailureListener(e -> {
                progressDialog.dismiss();
                Alerter.create(CancellationRequestsActivity.this)
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
        }

        public class RequestViewHolder extends RecyclerView.ViewHolder{

            TextView orderID, requestTime;

            public RequestViewHolder(@NonNull View itemView) {
                super(itemView);

                orderID = itemView.findViewById(R.id.order_id);
                requestTime = itemView.findViewById(R.id.request_time);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean isConnectedToInternet(CancellationRequestsActivity cancellationRequestsActivity) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) cancellationRequestsActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (null != networkInfo &&
                (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(CancellationRequestsActivity.this)
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
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        CustomIntent.customType(CancellationRequestsActivity.this, "up-to-bottom");
    }
}