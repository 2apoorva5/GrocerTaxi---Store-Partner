package com.application.grocertaxistore.Utilities;

public class Constants {
    public static final String KEY_PREFERENCE_NAME = "GrocerTaxiPreference";

    public static final String KEY_IS_SIGNED_IN = "isSignedIn";

    public static final String KEY_SIGNIN_SIGNUP_ACTION = "SignInUpAction";

    //Firebase Constants
    public static final String KEY_COLLECTION_CITIES = "Cities";
    public static final String KEY_COLLECTION_LOCALITIES = "Localities";
    public static final String KEY_COLLECTION_USERS = "Users";
    public static final String KEY_COLLECTION_STORES = "Stores";
    public static final String KEY_COLLECTION_REVIEWS = "Reviews";
    public static final String KEY_COLLECTION_CATEGORIES = "Categories";
    public static final String KEY_COLLECTION_PRODUCTS = "Products";
    public static final String KEY_COLLECTION_PENDING_ORDERS = "PendingOrders";
    public static final String KEY_COLLECTION_COMPLETED_ORDERS = "CompletedOrders";
    public static final String KEY_COLLECTION_CANCELLED_ORDERS = "CancelledOrders";
    public static final String KEY_COLLECTION_CANCELLATION_REQUESTS = "CancellationRequests";
    public static final String KEY_COLLECTION_ORDER_ITEMS = "OrderItems";

    public static final String KEY_CITY = "City";
    public static final String KEY_LOCALITY = "Locality";
    public static final String KEY_CATEGORY = "Category";
    public static final String KEY_ORDER = "Order";
    public static final String KEY_ORDER_TYPE = "OrderType";

    //User Constants
    public static final String KEY_USER_MOBILE = "userMobile";

    //Store Constants
    public static final String KEY_UID = "UID";
    public static final String KEY_STORE_TOKEN = "storeToken";
    public static final String KEY_STORE_ID = "storeID";
    public static final String KEY_STORE_TIMESTAMP = "storeTimestamp";
    public static final String KEY_STORE_IMAGE = "storeImage";
    public static final String KEY_STORE_NAME = "storeName";
    public static final String KEY_STORE_OWNER = "storeOwner";
    public static final String KEY_STORE_EMAIL = "storeEmail";
    public static final String KEY_STORE_MOBILE = "storeMobile";
    public static final String KEY_STORE_LOCATION = "storeLocation";
    public static final String KEY_STORE_ADDRESS = "storeAddress";
    public static final String KEY_STORE_LATITUDE = "storeLatitude";
    public static final String KEY_STORE_LONGITUDE = "storeLongitude";
    public static final String KEY_STORE_TIMING = "storeTiming";
    public static final String KEY_STORE_STATUS = "storeStatus";
    public static final String KEY_STORE_MINIMUM_ORDER_VALUE = "storeMinimumOrderValue";
    public static final String KEY_STORE_AVERAGE_RATING = "storeAverageRating";
    public static final String KEY_STORE_SEARCH_KEYWORD = "storeSearchKeyword";

    public static final String SUBLOCALITY = "subLocality";
    public static final String LOCALITY = "locality";
    public static final String COUNTRY = "country";
    public static final String PINCODE = "pinCode";
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";

    //Category Constants
    public static final String KEY_CATEGORY_NAME = "categoryName";

    //Product Constants
    public static final String KEY_PRODUCT_ID = "productID";
    public static final String KEY_PRODUCT_TIMESTAMP = "productTimestamp";
    public static final String KEY_PRODUCT_STORE_ID = "productStoreID";
    public static final String KEY_PRODUCT_STORE_NAME = "productStoreName";
    public static final String KEY_PRODUCT_CATEGORY = "productCategory";
    public static final String KEY_PRODUCT_IN_STOCK = "productInStock";
    public static final String KEY_PRODUCT_IMAGE = "productImage";
    public static final String KEY_PRODUCT_NAME = "productName";
    public static final String KEY_PRODUCT_IS_VEG = "productIsVeg";
    public static final String KEY_PRODUCT_UNIT = "productUnit";
    public static final String KEY_PRODUCT_MRP = "productMRP";
    public static final String KEY_PRODUCT_RETAIL_PRICE = "productRetailPrice";
    public static final String KEY_PRODUCT_OFFER = "productOffer";
    public static final String KEY_PRODUCT_UNITS_IN_STOCK = "productUnitsInStock";
    public static final String KEY_PRODUCT_DESCRIPTION = "productDescription";
    public static final String KEY_PRODUCT_BRAND = "productBrand";
    public static final String KEY_PRODUCT_MFG_DATE = "productMFGDate";
    public static final String KEY_PRODUCT_EXPIRY_TIME = "productExpiryTime";
    public static final String KEY_PRODUCT_SEARCH_KEYWORD = "productSearchKeyword";

    //Order Constants
    public static final String KEY_ORDER_ID = "orderID";
    public static final String KEY_ORDER_BY_USERID = "orderByUserID";
    public static final String KEY_ORDER_BY_USERNAME = "orderByUserName";
    public static final String KEY_ORDER_FROM_STOREID = "orderFromStoreID";
    public static final String KEY_ORDER_FROM_STORENAME = "orderFromStoreName";
    public static final String KEY_ORDER_CUSTOMER_NAME = "orderCustomerName";
    public static final String KEY_ORDER_CUSTOMER_MOBILE = "orderCustomerMobile";
    public static final String KEY_ORDER_DELIVERY_LOCATION = "orderDeliveryLocation";
    public static final String KEY_ORDER_DELIVERY_ADDRESS = "orderDeliveryAddress";
    public static final String KEY_ORDER_DELIVERY_LATITUDE = "orderDeliveryLatitude";
    public static final String KEY_ORDER_DELIVERY_LONGITUDE = "orderDeliveryLongitude";
    public static final String KEY_ORDER_DELIVERY_DISTANCE = "orderDeliveryDistance";
    public static final String KEY_ORDER_NO_OF_ITEMS = "orderNoOfItems";
    public static final String KEY_ORDER_TOTAL_MRP = "orderTotalMRP";
    public static final String KEY_ORDER_TOTAL_RETAIL_PRICE = "orderTotalRetailPrice";
    public static final String KEY_ORDER_COUPON_APPLIED = "orderCouponApplied";
    public static final String KEY_ORDER_COUPON_DISCOUNT = "orderCouponDiscount";
    public static final String KEY_ORDER_TOTAL_DISCOUNT = "orderTotalDiscount";
    public static final String KEY_ORDER_DELIVERY_CHARGES = "orderDeliveryCharges";
    public static final String KEY_ORDER_TIP_AMOUNT = "orderTipAmount";
    public static final String KEY_ORDER_SUB_TOTAL = "orderSubTotal";
    public static final String KEY_ORDER_PAYMENT_MODE = "orderPaymentMode";
    public static final String KEY_ORDER_CONVENIENCE_FEE = "orderConvenienceFee";
    public static final String KEY_ORDER_TOTAL_PAYABLE = "orderTotalPayable";
    public static final String KEY_ORDER_INSTRUCTIONS = "orderInstructions";
    public static final String KEY_ORDER_STATUS = "orderStatus";
    public static final String KEY_ORDER_PLACED_TIME = "orderPlacedTime";
    public static final String KEY_ORDER_COMPLETION_TIME = "orderCompletionTime";
    public static final String KEY_ORDER_CANCELLATION_TIME = "orderCancellationTime";
    public static final String KEY_ORDER_TIMESTAMP = "orderTimestamp";

    //OrderItems Constants
    public static final String KEY_ORDER_ITEM_ID = "orderItemID";
    public static final String KEY_ORDER_ITEM_TIMESTAMP = "orderItemTimestamp";
    public static final String KEY_ORDER_ITEM_PRODUCT_ID = "orderItemProductID";
    public static final String KEY_ORDER_ITEM_PRODUCT_STORE_ID = "orderItemProductStoreID";
    public static final String KEY_ORDER_ITEM_PRODUCT_STORE_NAME = "orderItemProductStoreName";
    public static final String KEY_ORDER_ITEM_PRODUCT_CATEGORY = "orderItemProductCategory";
    public static final String KEY_ORDER_ITEM_PRODUCT_IMAGE = "orderItemProductImage";
    public static final String KEY_ORDER_ITEM_PRODUCT_NAME = "orderItemProductName";
    public static final String KEY_ORDER_ITEM_PRODUCT_UNIT = "orderItemProductUnit";
    public static final String KEY_ORDER_ITEM_PRODUCT_MRP = "orderItemProductMRP";
    public static final String KEY_ORDER_ITEM_PRODUCT_RETAIL_PRICE = "orderItemProductRetailPrice";
    public static final String KEY_ORDER_ITEM_PRODUCT_QUANTITY = "orderItemProductQuantity";
}
