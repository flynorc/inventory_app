package com.flynorc.a10_inventoryapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.flynorc.a10_inventoryapp.data.InventoryContract.ProductEntry;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;

public class AddEditItemActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    /** Identifier for the product data loader */
    private static final int PRODUCT_LOADER = 0;

    // Codes for handling different requests
    private static final int PICK_IMAGE_REQUEST = 0;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int MY_PERMISSIONS_REQUEST = 2;

    // Constants for storing images to storage
    private static final String FILE_PROVIDER_AUTHORITY = "com.flynorc.fileprovider";


    /** Content URI for the existing product (or null if it's a new product) */
    private Uri currentProductUri;

    private Uri imageUri;

    // EditText fields
    private EditText nameEditText;
    private EditText priceEditText;
    private EditText quantityEditText;
    private EditText descriptionEditText;
    private EditText supplierEditText;
    private TextView imageUriTextView;
    private ImageView imageView;
    private Button takePictureButton;

    /** Product object - that holds all the data about the product we are manipulating (or creating) */
    private Product product;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_item);

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new product or editing an existing one.
        Intent intent = getIntent();
        currentProductUri = intent.getData();

        // Store references to the elements in layout that we might need to interact with
        findElementsInLayout();
        //request permissions to use the camera (if user wants to take photos)
        requestPermissions();

        // If the intent DOES NOT contain a product content URI, then we are creating a new one.
        if (currentProductUri == null) {
            setTitle(getString(R.string.editor_activity_title_new_product));

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a product that hasn't been created yet.)
            invalidateOptionsMenu();

            //create a new (empty) Product
            product = new Product();
        } else {
            // Otherwise this is an existing product
            setTitle(getString(R.string.editor_activity_title_edit_product));

            // Initialize a loader to read the product data from the database
            // and display the current values in the editor
            getLoaderManager().initLoader(PRODUCT_LOADER, null, this);
        }
    }

    /**
     * find and store references to and layout elements we will need to manipulate or interact with
     */
    private void findElementsInLayout() {
        nameEditText = (EditText) findViewById(R.id.edit_product_name);
        priceEditText = (EditText) findViewById(R.id.edit_product_price);
        quantityEditText = (EditText) findViewById(R.id.edit_product_quantity);
        descriptionEditText = (EditText) findViewById(R.id.edit_product_description);
        supplierEditText = (EditText) findViewById(R.id.edit_product_supplier);

        imageUriTextView = (TextView) findViewById(R.id.image_uri);
        imageView = (ImageView) findViewById(R.id.image_preview);

        takePictureButton = (Button) findViewById(R.id.take_picture_button);
        takePictureButton.setEnabled(false);
    }

    /**
     * Check if user has already allowed us to access to external storage
     * ask for permission if it has not yet been given
     * at this time, no explanation is shown to the user
     */
    public void requestPermissions() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {

            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST);

            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the result of the request.
        } else {
            takePictureButton.setEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Enable the take picture button
                    takePictureButton.setEnabled(true);
                } else {
                    // permission denied in our case the button is already disabled so no need to change anything now
                }
                return;
            }
        }
    }

    public void quantityDec(View view) {
        int newQuantity = 0;
        String quantityString = quantityEditText.getText().toString();

        if(!quantityString.isEmpty()) {
            newQuantity = Integer.parseInt(quantityString) - 1;
        }

        if (newQuantity < 0) {
            newQuantity = 0;
        }

        quantityEditText.setText(newQuantity + "");
    }

    public void quantityInc(View view) {
        int newQuantity = 0;
        String quantityString = quantityEditText.getText().toString();

        if(!quantityString.isEmpty()) {
            newQuantity = Integer.parseInt(quantityString) + 1;
        }

        quantityEditText.setText(newQuantity + "");
    }

    /**
     * onClick handler for sending email to the supplier email
     * @param view
     */
    public void sendEmail(View view) {
        readInputFields();
        Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

        emailIntent.setType("plain/text");
        String aEmailList[] = { product.getSupplier() };
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, aEmailList);
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.email_subject) + product.getName());
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.email_body_intro) + product.getName());

        startActivity(Intent.createChooser(emailIntent, getString(R.string.send_email_using)));
    }

    /**
     * onclick handler for when user clicks on "select image from gallery"
     * @param view
     */
    public void openImageSelector(View view) {
        Intent intent;

        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }

        // Show only images, no videos or anything else
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_picture)), PICK_IMAGE_REQUEST);
    }

    /**
     * onClick handler for when user clicks on "take picture" (with camera)
     * A new image file is created and passed along with the intent
     * to make sure camera app saves the image to that location
     * @param view
     */
    public void takePicture(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            //create a new image (empty) file, where the result will be stored
            File photoFile = null;
            try {
                photoFile = ProductImage.createImageFile(getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            } catch (IOException ex) {
                // There was an error while creating the file, show a toast
                Toast.makeText(this, R.string.error_creating_file_toast, Toast.LENGTH_LONG).show();
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, photoFile);

                /*
                 * Fix for permissions issue causing an error in Android N
                 * https://stackoverflow.com/questions/39787129/permission-denial-writing-android-support-v4-content-fileprovider-uri
                 */
                List<ResolveInfo> resolvedIntentActivities = this.getPackageManager().queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolvedIntentInfo : resolvedIntentActivities) {
                    String packageName = resolvedIntentInfo.activityInfo.packageName;
                    this.grantUriPermission(packageName, photoURI, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }

                /*
                 * start the camera intent and pass the uri to the file (where the result will be stored)
                 */
                product.setImagePath(photoFile.getAbsolutePath());
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    /**
     * Function that is called when the user returns from the gallery or camera intents back to this activity
     * @param requestCode
     * @param resultCode
     * @param resultData
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        switch (requestCode) {
            // Chose image from gallery
            case PICK_IMAGE_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    // We should receive the uri of the selected image from resultData.getData()
                    if (resultData != null) {
                        imageUri = resultData.getData();

                        // Copy the image located at the returned Uri to the storage of our app
                        // Also remove the previous version of the image from the filesystem IF
                        // the image is not the image referenced in the DB (the image we started with)
                        try {
                            product.removeProductImageIfChanged();

                            File productImage = ProductImage.copyImageFromUri(imageUri, getExternalFilesDir(Environment.DIRECTORY_PICTURES), this);
                            String imagePath = productImage.getAbsolutePath();
                            product.setImagePath(imagePath);
                        } catch (IOException e) {
                            e.printStackTrace();
                            //show toast
                            Toast.makeText(this, R.string.pick_image_error, Toast.LENGTH_SHORT).show();
                        }
                        // update the image preview
                        updateProductImagePreview();
                    }
                }
                else {
                    //result code was not ok, show toast
                    Toast.makeText(this, R.string.pick_image_error, Toast.LENGTH_SHORT).show();
                }
               break;
            // Take picture with camera
            case REQUEST_IMAGE_CAPTURE:
                if (resultCode == Activity.RESULT_OK) {
                    //update the preview
                    updateProductImagePreview();
                } else {
                    // Delete the temporary file created before starting the camera activity
                    // and revert the image path to the old path (how it was before editing)
                    product.removeProductImageIfChanged();
                }
                break;
        }
    }

    private void updateProductImagePreview() {
        imageUriTextView.setText(product.getImagePath());
        Bitmap bitmap = ProductImage.getImageFromPath(product.getImagePath());
        imageView.setImageBitmap(bitmap);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_add_item.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_add_item, menu);
        return true;
    }

    /**
     * This method is called after invalidateOptionsMenu(), so that the
     * menu can be updated (some menu items can be hidden or made visible).
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new product, hide the "Delete" menu item.
        if (currentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                //read the text fields and store values to variables
                readInputFields();
                //check if item valid
                if(validateInputs()) {
                    // Save the item to the database
                    saveItem();
                    // Exit activity
                    finish();
                }
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Pop up confirmation dialog for deletion
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the product hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                readInputFields();
                if (!product.hasChanged()) {
                    NavUtils.navigateUpFromSameTask(AddEditItemActivity.this);
                    return true;
                }

                // There are unsaved changes, setup a dialog to warn the user.
                setUpUnsavedChangesDialog();

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setUpUnsavedChangesDialog() {
        // Create a click listener to handle the user confirming that
        // changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, navigate to parent activity.
                        if (product.hasImageChanged()) {
                            product.deleteImageFromStorage();
                        }
                        NavUtils.navigateUpFromSameTask(AddEditItemActivity.this);
                    }
                };

        // Show a dialog that notifies the user they have unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private boolean validateInputs() {
        //validate name
        if (product.getName().isEmpty()) {
            Toast.makeText(this, R.string.validation_error_name_empty, Toast.LENGTH_LONG).show();
            return false;
        }

        // validate price
        if (product.getPrice() <= 0) {
            Toast.makeText(this, R.string.validation_error_price_negative, Toast.LENGTH_LONG).show();
            return false;
        }

        // validate quantity
        if (product.getQuantity() < 0) {
            Toast.makeText(this, R.string.validation_error_quantity_negative, Toast.LENGTH_LONG).show();
            return false;
        }

        // validate supplier email (optional)
        if(!product.getSupplier().isEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(product.getSupplier()).matches()) {
            Toast.makeText(this, R.string.validation_error_supplier_invalid, Toast.LENGTH_LONG).show();
            return false;
        }

        //validate image
        if(product.getImagePath() == null || new File (product.getImagePath()) == null ) {
            Toast.makeText(this, R.string.validation_error_image, Toast.LENGTH_LONG).show();
            return false;
        }

        //all the validation rules have passed,... YEEY!
        return true;
    }

    private void readInputFields() {
        product.setName(nameEditText.getText().toString().trim());
        product.setPrice(parsePrice(priceEditText.getText().toString().trim()));
        product.setQuantity(parseInteger(quantityEditText.getText().toString().trim()));
        product.setDescription(descriptionEditText.getText().toString().trim());
        product.setSupplier(supplierEditText.getText().toString().trim());
    }

    /*
     * helper function that returns 0 if the string is empty otherwise tries to parse an integer value
     */
    private int parseInteger(String s) {
        if(s.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(s);
    }

    private int parsePrice(String s) {
        if(s.isEmpty()) {
            return 0;
        }
        Float price = 0.0f;
        NumberFormat nf = NumberFormat.getInstance();
        try {
            Number number = nf.parse(s);
            price = number.floatValue();
        } catch (ParseException e) {
            Toast.makeText(this, R.string.price_parsing_error, Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        return Math.round(price*100);

    }

    private void saveItem() {
        // Create a new thumbnail
        if (product.hasImageChanged()) {
            product.createThumbnail();
        }

        //get the values from the product object
        ContentValues values = product.getContentValues();

        if (currentProductUri == null) {
            insertNewProduct(values);
        } else {
            updateProduct(values);
        }
    }


    private void insertNewProduct(ContentValues values) {
        Uri newUri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);

        // Show a toast message depending on whether or not the insertion was successful.
        if (newUri == null) {
            // If the new content URI is null, then there was an error with insertion.
            Toast.makeText(this, getString(R.string.editor_insert_product_failed), Toast.LENGTH_SHORT).show();
        } else {
            // Otherwise, the insertion was successful and we can display a toast.
            Toast.makeText(this, getString(R.string.editor_insert_product_successful), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateProduct(ContentValues values) {
        int rowsAffected = getContentResolver().update(currentProductUri, values, null, null);

        // Show a toast message depending on whether or not the update was successful.
        if (rowsAffected == 0) {
            // If no rows were affected, then there was an error with the update.
            product.removeProductImageIfChanged();
            Toast.makeText(this, getString(R.string.editor_update_product_failed), Toast.LENGTH_SHORT).show();
        } else {
            // Otherwise, the update was successful and we can display a toast.
            if(product.hasImageChanged()) {
                product.removeOldImages();
            }
            Toast.makeText(this, getString(R.string.editor_update_product_successful), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Prompt the user to confirm that they want to delete this product.
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the product.
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void deleteProduct() {
        // Call the ContentResolver to delete the product at the given content URI.
        // Pass in null for the selection and selection args because the currentProductUri
        // content URI already identifies the product that we want.
        // If product was deleted from database, also remove the image file(s) from the storage
        int rowsDeleted = getContentResolver().delete(currentProductUri, null, null);

        // Show a toast message depending on whether or not the delete was successful.
        if (rowsDeleted == 0) {
            // If no rows were deleted, then there was an error with the delete.
            Toast.makeText(this, getString(R.string.editor_delete_product_failed),
                    Toast.LENGTH_SHORT).show();
        } else {
            // Otherwise, the delete was successful, also delete the image files from storage
            // and we can display a toast.
            product.deleteImageFromStorage();
            product.deleteThumbFromStorage();
            Toast.makeText(this, getString(R.string.editor_delete_product_successful), Toast.LENGTH_SHORT).show();
        }

        // Close the activity
        finish();
    }

    /**
     * This method is called when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        // If the product hasn't changed, continue with handling back button press
        readInputFields();
        if (!product.hasChanged()) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, check if the image file was changed
                        // that needs to be removed and close the current activity.
                        product.removeProductImageIfChanged();
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // define a projection that contains all relevant columns from the products table
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_DESCRIPTION,
                ProductEntry.COLUMN_PRODUCT_SUPPLIER_EMAIL,
                ProductEntry.COLUMN_PRODUCT_IMAGE_PATH,
                ProductEntry.COLUMN_PRODUCT_THUMB_PATH};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                currentProductUri,         // Query the content URI for the current product
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        // Make product be an empty (new) product
        if (data == null || data.getCount() < 1) {
            product = new Product();
            return;
        }

        product = Product.createProductFromCursorData(data);


        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (data.moveToFirst()) {

            // Update the views on the screen with the values from the database
            nameEditText.setText(product.getName());
            priceEditText.setText(product.getPriceForDisplay(this, false));
            quantityEditText.setText(Integer.toString(product.getQuantity()));
            descriptionEditText.setText(product.getDescription());
            supplierEditText.setText(product.getSupplier());
            imageUriTextView.setText(product.getImagePath()+ "\n" + product.getThumbnailPath());

            imageView.setImageBitmap(product.getImageBitmap());




        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        nameEditText.setText("");
        priceEditText.setText("");
        quantityEditText.setText("");
        descriptionEditText.setText("");
        supplierEditText.setText("");
        imageUriTextView.setText("");
        imageView.setImageResource(android.R.color.transparent);
    }

}
