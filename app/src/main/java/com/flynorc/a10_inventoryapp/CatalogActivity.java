package com.flynorc.a10_inventoryapp;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.flynorc.a10_inventoryapp.data.InventoryContract.ProductEntry;

public class CatalogActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    // identifier for the inventory items loader
    private static final int INVENTORY_ITEMS_LOADER = 0;

    // Adapter for the ListView
    InventoryCursorAdapter cursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);


        // Add the onClick handler for the Add button
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.add_item_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CatalogActivity.this, AddEditItemActivity.class);
                startActivity(intent);
            }
        });

        // Get the reference to the listView
        ListView inventoryListView = (ListView) findViewById(R.id.list);

        //set up the empty list view for when there are no items to be shown
        View emptyView = findViewById(R.id.empty_view);
        inventoryListView.setEmptyView(emptyView);

        // create a new InventoryCursorAdapter and attach it to the listView
        // there is no data available until the loader finishes so we pass null for the Cursor
        cursorAdapter = new InventoryCursorAdapter(this, null);
        inventoryListView.setAdapter(cursorAdapter);

        // Setup the item click listener
        inventoryListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                // Create new intent to go to AddEditItemActivity
                Intent intent = new Intent(CatalogActivity.this, AddEditItemActivity.class);

                // Form the content URI that represents the specific item that was clicked on,
                // by appending the "id" (passed as input to this method) onto the CONTENT_URI
                // For example, the URI would be "content://com.flynorc.a10_inventoryapp/items/4"
                // if the item with ID 4 was clicked on.
                Uri currentItemUri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, id);

                // Set the URI on the data field of the intent
                intent.setData(currentItemUri);

                // Launch the {@link AddEditItemActivity} to display the details about current item
                startActivity(intent);
            }
        });

        // Start the loader
        getLoaderManager().initLoader(INVENTORY_ITEMS_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_THUMB_PATH};

        return new CursorLoader(this, ProductEntry.CONTENT_URI, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        cursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        cursorAdapter.swapCursor(null);
    }
}
