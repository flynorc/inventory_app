package com.flynorc.a10_inventoryapp.data;

/**
 * Created by flynorc on 31.5.2017.
 */

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * API Contract for the Inventory app.
 */
public final class InventoryContract {

    //private constructor to prevent someone from instantiating the contract class
    private InventoryContract() {}

    /*
     * constants to provide consistent use of paths and content authority throughout the app
     */
    public static final String CONTENT_AUTHORITY = "com.flynorc.a10_inventoryapp";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_PRODUCTS = "products";
    public static final String PATH_PRODUCTS_SELL = "products_sell";

    /**
     * Inner class that defines constant values for the products database table
     * Each entry in the table represents a single product
     */
    public static final class ProductEntry implements BaseColumns {
        //content://com.flynorc.a10_inventoryapp/products
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_PRODUCTS);
        public static final Uri CONTENT_URI_SELL = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_PRODUCTS_SELL);

        public static final String CONTENT_LIST_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCTS;
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCTS;

        //database table name
        public final static String TABLE_NAME = "products";

        /*
         * columns in the table
         */

        /**
         * Unique ID number for the product (only for use in the database table).
         *
         * Type: INTEGER
         */
        public final static String _ID = BaseColumns._ID;

        /**
         * Name of the product.
         *
         * Type: TEXT
         */
        public final static String COLUMN_PRODUCT_NAME ="name";

        /**
         * Price of the product.
         * stored in cents
         *
         * Type: INTEGER
         */
        public final static String COLUMN_PRODUCT_PRICE = "price";

        /**
         * Quantity of the product (nr items in stock).
         *
         * Type: INTEGER
         */
        public final static String COLUMN_PRODUCT_QUANTITY = "quantity";

        /**
         * Email of supplier where the product can be ordered when out of stock.
         *
         * Type: TEXT
         */
        public final static String COLUMN_PRODUCT_SUPPLIER_EMAIL = "supplier_email";

        /**
         * Image path of the product.
         *
         * Type: TEXT
         */
        public final static String COLUMN_PRODUCT_IMAGE_PATH = "image_path";

        /**
         * Image thumbnail path of the product.
         *
         * Type: TEXT
         */
        public final static String COLUMN_PRODUCT_THUMB_PATH = "thumb_path";

        /**
         * Description of the product.
         *
         * Type: TEXT
         */
        public final static String COLUMN_PRODUCT_DESCRIPTION = "description";
    }
}
