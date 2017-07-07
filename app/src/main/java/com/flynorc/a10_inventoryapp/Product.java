package com.flynorc.a10_inventoryapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;

import com.flynorc.a10_inventoryapp.data.InventoryContract;

import java.io.File;
import java.io.IOException;

/**
 * Created by Flynorc on 07-Jul-17.
 */

public class Product {

    // Logging tag
    public static final String LOG_TAG = "Product class";

    // Values of the item
    private String name = "";
    private int price = 0;
    private int quantity = 0;
    private String description = "";
    private String supplier = "";
    private String imagePath = null;
    private String thumbnailPath = null;
    // path to the image that has already been chosen from gallery or taken with camera,
    // but before the changes were saved to DB
    // the file needs to be removed if the user decided to discard the changes
    private String temporaryImagePath;

    // Original values of the item (used to check if values have actually changed)
    // and to delete the old image(s) from storage if a new one is provided
    private String nameOld = "";
    private int priceOld = 0;
    private int quantityOld = 0;
    private String descriptionOld = "";
    private String supplierOld = "";
    private String imagePathOld = null;
    private String thumbnailPathOld = null;

    public Product() {

    }

    /*
     * getters
     */
    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public String getPriceForDisplay(Context context, boolean withUnit) {
        if(withUnit) {
            return String.format("%.2f", price/100.0) + context.getString(R.string.price_unit);
        }
        return String.format("%.2f", price/100.0);
    }
    public int getQuantity() {
        return quantity;
    }

    public String getDescription() {
        return description;
    }

    public String getSupplier() {
        return supplier;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }


    /*
     * setters
     */
    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    /*
     * checking if product has changed
     * as soon as one of the conditions is true, the product has changed
     */
    public boolean hasChanged() {

        Log.d(LOG_TAG, "has changed here");
        return !areStringsSame(name, nameOld) ||
                !areStringsSame(description, descriptionOld) ||
                !areStringsSame(supplier, supplierOld) ||
                !areIntsSame(price, priceOld) ||
                !areIntsSame(quantity, quantityOld) ||
                hasImageChanged();
    }

    public boolean hasImageChanged() {
        //if both image paths are not set up yet (new product), image has not changed yet
        if(imagePath == null && imagePathOld == null) {
            return false;
        }

        return !imagePath.equals(imagePathOld);
    }

    private boolean areStringsSame(String valueNew, String valueOld) {
        if(valueNew == null && valueOld == null) {
            return true;
        }

        return valueNew.equals(valueOld);
    }

    private boolean areIntsSame(int valueNew, int valueOld) {
        Log.d(LOG_TAG, "comparing ints...  old: " + valueOld + ", new: " + valueNew);
        return valueNew == valueOld;
    }


    public void deleteImageFromStorage() {
        ProductImage.deleteFile(imagePath);
    }

    public void deleteThumbFromStorage() {
        ProductImage.deleteFile(thumbnailPath);
    }

    public void removeProductImageIfChanged() {
        if(hasImageChanged()) {
            deleteImageFromStorage();
            restoreOldImagePath();
        }
    }


    private void restoreOldImagePath() {
        imagePath = imagePathOld;
    }

    public void removeOldImages() {
        if(imagePathOld != null) {
            ProductImage.deleteFile(imagePathOld);
        }
        if(thumbnailPathOld != null) {
            ProductImage.deleteFile(thumbnailPathOld);
        }
    }

    public void createThumbnail() {

        Log.d(LOG_TAG, "creating thumbnail");
        thumbnailPath = ProductImage.createThumbnail(imagePath);
    }

    /**
     * Function copies all the current attributes to the "old" version of the attributes
     * used when creating a product from db values to be able to check later on if some values have changed
     */
    public void copyCurrentAttributesToOld() {
        nameOld = name;
        priceOld = price;
        quantityOld = quantity;
        descriptionOld = description;
        supplierOld = supplier;
        imagePathOld = imagePath;
        thumbnailPathOld = thumbnailPath;
    }

    /**
     * Create the ContentValues object from all product attributes
     * @return
     */
    public ContentValues getContentValues() {

        ContentValues values = new ContentValues();
        values.put(InventoryContract.ProductEntry.COLUMN_PRODUCT_NAME, name);
        values.put(InventoryContract.ProductEntry.COLUMN_PRODUCT_PRICE, price);
        values.put(InventoryContract.ProductEntry.COLUMN_PRODUCT_QUANTITY, quantity);
        values.put(InventoryContract.ProductEntry.COLUMN_PRODUCT_DESCRIPTION, description);
        values.put(InventoryContract.ProductEntry.COLUMN_PRODUCT_SUPPLIER_EMAIL, supplier);
        values.put(InventoryContract.ProductEntry.COLUMN_PRODUCT_IMAGE_PATH, imagePath);
        values.put(InventoryContract.ProductEntry.COLUMN_PRODUCT_THUMB_PATH, thumbnailPath);

        return values;
    }

    /**
     * Function returns the bitmap of the product image in correct orientation
     * @return
     */
    public Bitmap getImageBitmap() {
        // check if we have an image path at all
        if(imagePath == null) {
            return null;
        }

        return ProductImage.getImageFromPath(imagePath);
    }


    public static Product createProductFromCursorData(Cursor data) {
        Product product = new Product();

        if (data.moveToFirst()) {
            // Find the columns of product attributes that we're interested in
            int nameColumnIndex = data.getColumnIndex(InventoryContract.ProductEntry.COLUMN_PRODUCT_NAME);
            int priceColumnIndex = data.getColumnIndex(InventoryContract.ProductEntry.COLUMN_PRODUCT_PRICE);
            int quantityColumnIndex = data.getColumnIndex(InventoryContract.ProductEntry.COLUMN_PRODUCT_QUANTITY);
            int descriptionColumnIndex = data.getColumnIndex(InventoryContract.ProductEntry.COLUMN_PRODUCT_DESCRIPTION);
            int supplierColumnIndex = data.getColumnIndex(InventoryContract.ProductEntry.COLUMN_PRODUCT_SUPPLIER_EMAIL);
            int imageColumnIndex = data.getColumnIndex(InventoryContract.ProductEntry.COLUMN_PRODUCT_IMAGE_PATH);
            int thumbColumnIndex = data.getColumnIndex(InventoryContract.ProductEntry.COLUMN_PRODUCT_THUMB_PATH);

            // Extract out the value from the Cursor for the given column index
            product.setName(data.getString(nameColumnIndex));
            product.setPrice(data.getInt(priceColumnIndex));
            product.setQuantity(data.getInt(quantityColumnIndex));
            product.setDescription(data.getString(descriptionColumnIndex));
            product.setSupplier(data.getString(supplierColumnIndex));
            product.setImagePath(data.getString(imageColumnIndex));
            product.setThumbnailPath(data.getString(thumbColumnIndex));

            //call the function that copies all the current values to old values
            product.copyCurrentAttributesToOld();
        }

        return product;
    }

    public static Product createProductBaseFromCursorData(Cursor data) {
        Product product = new Product();

        // Find the columns of product attributes that we're interested in
        // (in this case only the basic 4 atributes)
        int nameColumnIndex = data.getColumnIndex(InventoryContract.ProductEntry.COLUMN_PRODUCT_NAME);
        int priceColumnIndex = data.getColumnIndex(InventoryContract.ProductEntry.COLUMN_PRODUCT_PRICE);
        int quantityColumnIndex = data.getColumnIndex(InventoryContract.ProductEntry.COLUMN_PRODUCT_QUANTITY);
        int thumbColumnIndex = data.getColumnIndex(InventoryContract.ProductEntry.COLUMN_PRODUCT_THUMB_PATH);

        // Extract out the value from the Cursor for the given column index
        product.setName(data.getString(nameColumnIndex));
        product.setPrice(data.getInt(priceColumnIndex));
        product.setQuantity(data.getInt(quantityColumnIndex));
        product.setThumbnailPath(data.getString(thumbColumnIndex));

        //call the function that copies all the current values to old values
        product.copyCurrentAttributesToOld();

        return product;
    }


}
