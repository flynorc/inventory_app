package com.flynorc.a10_inventoryapp;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.flynorc.a10_inventoryapp.data.InventoryContract.ProductEntry;

/**
 * Created by flynorc on 31.5.2017.
 * custom cursor adapter implementation
 */

public class InventoryCursorAdapter extends CursorAdapter {

    /**
     * Constructs a new {@link InventoryCursorAdapter}
     *
     * @param context   The context
     * @param c         The cursor from which to get the data
     */
    public InventoryCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    /**
     * Inflate a new view from the layout XML file
     *
     * @param context The context
     * @param cursor  The cursor from which we get the data - already moved to correct position
     * @param parent  The parent view (to which newly inflated view will be attached to)
     * @return the newly created list item view
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * Bind the inventory item data to the list item layout.
     *
     * @param view    Existing view for reuse (or a newly inflated one from newView method)
     * @param context The context
     * @param cursor  The cursor from which we get the data - already moved to correct position
     */
    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        //Find the views that we need to modify in the list item layout
        ImageView thumbImageView = (ImageView) view.findViewById(R.id.item_thumb);
        TextView nameTextView = (TextView) view.findViewById(R.id.item_name);
        TextView priceTextView = (TextView) view.findViewById(R.id.item_price);
        TextView quantityTextView = (TextView) view.findViewById(R.id.item_quantity);
        Button sellButton = (Button) view.findViewById(R.id.item_sell_button);

        //set up the click listener
        sellButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // get the position and item id, so we can generate the uri we need to
                // decrease quantity of that product in DB
                View parentRow = (View) v.getParent();
                ListView listView = (ListView) parentRow.getParent();
                int position = listView.getPositionForView(parentRow);
                long itemId = listView.getItemIdAtPosition(position);
                Uri currentItemUri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI_SELL, itemId);
                context.getContentResolver().update(currentItemUri, null, null, null);
            }
        });

        Product product = Product.createProductBaseFromCursorData(cursor);
        //update the values
        nameTextView.setText(product.getName());
        priceTextView.setText(product.getPriceForDisplay(context, true));
        quantityTextView.setText(context.getResources().getQuantityString(R.plurals.number_items_left, product.getQuantity(), product.getQuantity()));

        //disable the button if quantity is 0, to prevent the user from clicking on sell button
        if(product.getQuantity() > 0) {
            sellButton.setEnabled(true);
        }
        else {
            sellButton.setEnabled(false);
        }

        thumbImageView.setImageBitmap(BitmapFactory.decodeFile(product.getThumbnailPath()));

    }
}
