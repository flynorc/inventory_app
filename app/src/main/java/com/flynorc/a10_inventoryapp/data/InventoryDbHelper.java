package com.flynorc.a10_inventoryapp.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.flynorc.a10_inventoryapp.data.InventoryContract.ProductEntry;

/**
 * Created by flynorc on 31.5.2017.
 * Helper class for managing database creation and migrations for database updates
 */

public class InventoryDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "inventory.db";
    private static final int DATABASE_VERSION = 1;

    public InventoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    //create the db (only called if not exists)
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_PRODUCTS_TABLE_SQL = "CREATE TABLE " + ProductEntry.TABLE_NAME + " ("
                + ProductEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ProductEntry.COLUMN_PRODUCT_NAME + " TEXT NOT NULL, "
                + ProductEntry.COLUMN_PRODUCT_PRICE + " INTEGER NOT NULL, "
                + ProductEntry.COLUMN_PRODUCT_QUANTITY + " INTEGER NOT NULL DEFAULT 0, "
                + ProductEntry.COLUMN_PRODUCT_SUPPLIER_EMAIL + " TEXT NOT NULL, "
                + ProductEntry.COLUMN_PRODUCT_DESCRIPTION + " TEXT, "
                + ProductEntry.COLUMN_PRODUCT_THUMB_PATH + " TEXT, "
                + ProductEntry.COLUMN_PRODUCT_IMAGE_PATH + " TEXT);";

        db.execSQL(CREATE_PRODUCTS_TABLE_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //still at version 1 so nothing but spider webs here now...
    }
}
