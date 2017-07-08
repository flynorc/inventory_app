package com.flynorc.a10_inventoryapp.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.flynorc.a10_inventoryapp.data.InventoryContract.ProductEntry;

/**
 * Created by flynorc on 31.5.2017.
 * Content provider class for the inventory app
 */

public class ProductProvider extends ContentProvider {

    //URI matcher code for the content URI for the products table
    private static final int PRODUCTS = 100;

    //URI matcher code for the content URI for a single product in the products table
    private static final int PRODUCT_ID = 101;

    //URI matcher code for the content URI for calling a sale (decreasing quantity by one) for a single product in the products table
    private static final int PRODUCT_ID_SALE = 102;

    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        // The calls to addURI() go here, for all of the content URI patterns that the provider
        // should recognize. All paths added to the UriMatcher have a corresponding code to return
        // when a match is found.

        // The content URI of the form "content://com.flynorc.a10_inventoryapp/products" will map to the
        // integer code {@link #PRODSDUCTS}. This URI is used to provide access to MULTIPLE rows
        // of the products table.
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_PRODUCTS, PRODUCTS);

        // The content URI of the form "content://com.flynorc.a10_inventoryapp/products/#" will map to the
        // integer code {@link #PRODUCT_ID}. This URI is used to provide access to ONE single row
        // of the products table.
        //
        // In this case, the "#" wildcard is used where "#" can be substituted for an integer.
        // For example, "content://com.flynorc.a10_inventoryapp/products/3" matches, but
        // "content://com.flynorc.a10_inventoryapp/products" (without a number at the end) doesn't match.
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_PRODUCTS + "/#", PRODUCT_ID);

        // The content URI of the form "content://com.flynorc.a10_inventoryapp/products_sell/#"
        // will handle decreasing the quantity by one - for the product with the id of # wildcard
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_PRODUCTS_SELL + "/#", PRODUCT_ID_SALE);
    }

    //database helper object
    private InventoryDbHelper dbHelper;

    @Override
    public boolean onCreate() {
        dbHelper = new InventoryDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        // Get readable database
        SQLiteDatabase database = dbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                // We are interested in the multiple rows from the products table
                // Just pass along the projection, selection and selection args to the database query.
                // The cursor could contain multiple rows of the products table.
                cursor = database.query(ProductEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case PRODUCT_ID:
                // For the PRODUCT_ID code, extract out the ID from the URI.
                // For an example URI such as "content://com.flynorc.a10_inventoryapp/products/3",
                // the selection will be "_id=?" and the selection argument will be a
                // String array containing the actual ID of 3 in this case.
                //
                // For every "?" in the selection, we need to have an element in the selection
                // arguments that will fill in the "?". Since we have 1 question mark in the
                // selection, we have 1 String in the selection arguments' String array.
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };

                // This will perform a query on the products table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                cursor = database.query(ProductEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Set notification URI on the Cursor,
        // so we know what content URI the Cursor was created for.
        // If the data at this URI changes, then we know we need to update the Cursor.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        // Return the cursor
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return ProductEntry.CONTENT_LIST_TYPE;
            case PRODUCT_ID:
                return ProductEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return insertProduct(uri, values);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Insert a product into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */
    private Uri insertProduct(Uri uri, ContentValues values) {

        validateProductInsert(values);

        // Get writable database
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        // Insert the new product with the given values
        long id = database.insert(ProductEntry.TABLE_NAME, null, values);
        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1) {
            return null;
        }

        // Notify all listeners that the data has changed for the product content URI
        getContext().getContentResolver().notifyChange(uri, null);

        // Return the new URI with the ID (of the newly inserted row) appended at the end
        return ContentUris.withAppendedId(uri, id);
    }

    private void validateProductInsert(ContentValues values) {

        // Check that the required fields are not null
        validateRequired(values, ProductEntry.COLUMN_PRODUCT_NAME);
        validateRequired(values, ProductEntry.COLUMN_PRODUCT_PRICE);
        validateRequired(values, ProductEntry.COLUMN_PRODUCT_QUANTITY);
        validateRequired(values, ProductEntry.COLUMN_PRODUCT_IMAGE_PATH);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        // Get writable database
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        // Track the number of rows that were deleted
        int rowsDeleted;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                // Delete all rows that match the selection and selection args
                rowsDeleted = database.delete(ProductEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case PRODUCT_ID:
                // Delete a single row given by the ID in the URI
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                rowsDeleted = database.delete(ProductEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        // If 1 or more rows were deleted, then notify all listeners that the data at the
        // given URI has changed
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows deleted
        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCTS:
                return updateProduct(uri, values, selection, selectionArgs);
            case PRODUCT_ID:
                // For the PRODUCT_ID code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                selection = ProductEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                return updateProduct(uri, values, selection, selectionArgs);
            case PRODUCT_ID_SALE:
                // Get writable database to update the data
                SQLiteDatabase database = dbHelper.getWritableDatabase();

                // Create the SQL string and statement arguments
                // make sure to only decrease quantity if it is bigger than 0 and for the id passed with uri
                String sqlStatement = "UPDATE " + ProductEntry.TABLE_NAME
                        + " SET " + ProductEntry.COLUMN_PRODUCT_QUANTITY + " = " +  ProductEntry.COLUMN_PRODUCT_QUANTITY + " -1"
                        + " WHERE " + ProductEntry._ID + "=? AND " + ProductEntry.COLUMN_PRODUCT_QUANTITY + " > 0";

                SQLiteStatement statement = database.compileStatement(sqlStatement);
                statement.bindLong(1, ContentUris.parseId(uri));
                // Execute the prepared statement
                int nrRowsUpdated = statement.executeUpdateDelete();

                // There was a change in the database, notify all interested parties about it
                if(nrRowsUpdated > 0) {
                    getContext().getContentResolver().notifyChange(ContentUris.withAppendedId(ProductEntry.CONTENT_URI, ContentUris.parseId(uri)), null);
                }
                return nrRowsUpdated;

            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    private int updateProduct(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        validateProductUpdate(values);

        // If there are no values to update, then don't try to update the database
        if (values.size() == 0) {
            return 0;
        }

        // Otherwise, get writable database to update the data
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        // Perform the update on the database and get the number of rows affected
        int rowsUpdated = database.update(ProductEntry.TABLE_NAME, values, selection, selectionArgs);

        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows updated
        return rowsUpdated;
    }

    private void validateProductUpdate(ContentValues values) {
        //If there is a key for product name, make sure it is not empty. If there is no name set
        //no need to worry, since the name was validated on insert. Same applies for other columns
        if (values.containsKey(ProductEntry.COLUMN_PRODUCT_NAME)) {
            validateRequired(values, ProductEntry.COLUMN_PRODUCT_NAME);
        }
    }

    private void validateRequired(ContentValues values, String columnName) {
        String name = values.getAsString(columnName);
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException(columnName + "is required and can not be empty");
        }
    }

}
