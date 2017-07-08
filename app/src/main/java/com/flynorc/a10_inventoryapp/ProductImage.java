package com.flynorc.a10_inventoryapp;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Flynorc on 07-Jul-17.
 *
 * Helper class where all the image manipulation code lives
 */

public class ProductImage {
    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";

    /**
     * Function returns the bitmap of the image at given path in correct orientation
     * @param imagePath
     * @return
     */
    public static Bitmap getImageFromPath(String imagePath) {
        // make sure file with that path exists
        File imageFile = new File(imagePath);
        if(imageFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);

            // read the orientation from the metadata
            ExifInterface ei = null;
            try {
                ei = new ExifInterface(imagePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);

            //rorate the image (if needed), based on the orientation variable
            switch(orientation) {

                case ExifInterface.ORIENTATION_ROTATE_90:
                    bitmap = rotateImage(bitmap, 90);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    bitmap = rotateImage(bitmap, 180);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    bitmap = rotateImage(bitmap, 270);
                    break;

                case ExifInterface.ORIENTATION_NORMAL:
                    break;
            }
            return bitmap;
        }
        else {
            return null;
        }
    }


    /**
     * Helper function to rotate the product image
     * @param source
     * @param angle
     * @return
     */
    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }


    /**
     * function copies the file located at given uri and stores it to internal storage
     * in the folder of our app
     * @param uri
     * @param storageDir
     * @param context
     * @return
     * @throws IOException
     */
    public static File copyImageFromUri(Uri uri, File storageDir, Context context) throws IOException {
        String imagePath = getPath(context, uri) ;

        File sourceFile = new File(imagePath);

        //create new file for output
        File outputFile = createImageFile(storageDir);

        // make sure the source file exists
        if (!sourceFile.exists()) {
            return null;
        }

        // copy the file
        FileChannel source = null;
        FileChannel destination = null;
        source = new FileInputStream(sourceFile).getChannel();
        destination = new FileOutputStream(outputFile).getChannel();
        if (destination != null && source != null) {
            destination.transferFrom(source, 0, source.size());
        }
        if (source != null) {
            source.close();
        }
        if (destination != null) {
            destination.close();
        }

        return outputFile;
    }

    /**
     * Creates an empty image file with the prefix and suffix inside our apps folder
     * @param storageDir
     * @return
     * @throws IOException
     */
    public static File createImageFile(File storageDir) throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
        File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, storageDir);

        return imageF;
    }

    public static void deleteFile(String filePath) {
        File fileToDelete = new File(filePath);
        if (fileToDelete.exists()) {
            fileToDelete.delete();
        }
    }

    /**
     * Creates a thumbnail of the image at given path in the same folder with the filename
     * based on the original path (only adding _thumb at the end of the file name - before ending)
     * @param imagePath
     * @return
     */
    public static String createThumbnail(String imagePath) {
        //make the filename have _thumb prefix (before the .jpg) ending
        String thumbnailPath = imagePath.substring(0, imagePath.length() - 4) + "_thumb" + imagePath.substring(imagePath.length()-4);
        // Thumbnail of 400x400 px should be enough for all kind of devices in 100dp
        // in a real app this should be calculated based on the screen type,
        //but I guess it is out of the scope for this course
        Bitmap bmThumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(imagePath), 400, 400);

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(thumbnailPath);
            bmThumbnail.compress(Bitmap.CompressFormat.JPEG, 85, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return thumbnailPath;
    }

    /*
     * code to get the file path from uri
     * https://stackoverflow.com/questions/36128077/android-opening-a-file-with-action-get-content-results-into-different-uris
     * Don't know how to solve the "Call requires API level 19" in the marked lines (apart from simply changing the build.gradle
     * but as I was also testing on API 17, I left it so
     */
    public static String getPath(Context context, Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {    //this line is API17 incompatible
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri); //this line is API17 incompatible
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri); //this line is API17 incompatible
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else
            if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri); //this line is API17 incompatible
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = { column };
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
}
