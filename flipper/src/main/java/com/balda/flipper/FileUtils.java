package com.balda.flipper;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;


import static android.net.Uri.parse;

@SuppressWarnings("unused")
public class FileUtils {

    private FileUtils() {
    }

    @Nullable
    public static DocumentFile getDocumentFile(Context context, Uri uri, Root root) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                return DocumentFile.fromSingleUri(context,
                        DocumentsContract.buildDocumentUriUsingTree(root
                        .toRootDirectory(context)
                        .getUri(), DocumentsContract.getDocumentId(uri)));
            } catch (Exception e) {
                return null;
            }
        } else {
            String path = getPath(context, uri);
            if (path == null) {
                return null;
            }
            return DocumentFile.fromFile(new File(path));
        }
    }

    @Nullable
    public static DocumentFile getDocumentFile(Context context, String uri, Root root) {
        return getDocumentFile(context, Uri.parse(uri), root);
    }

    private static String getPath(final Context context, final Uri uri) {
        try {
            // DocumentProvider
            if (DocumentFile.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    if (split.length <= 1)
                        return null;
                    return Environment.getExternalStorageDirectory() + File.separator + split[1];
                }
                // DownloadsProvider
                else if (isDownloadsDocument(uri)) {
                    final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri =
                            ContentUris.withAppendedId(parse("content://downloads" +
                                    "/public_downloads"), Long
                            .valueOf(id));
                    return getDataColumn(context, contentUri, null, null);
                }
                // MediaProvider
                else if (isMediaDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    if (split.length <= 1)
                        return null;
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
                    final String[] selectionArgs = new String[]{split[1]};
                    return getDataColumn(context, contentUri, selection, selectionArgs);
                }
            }
            // MediaStore (and general)
            else if (ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(uri.getScheme())) {
                return getDataColumn(context, uri, null, null);
            }
            // File
            else if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }
            Log.d("Tag", "no path");
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    private static String getDataColumn(Context context, Uri uri, String selection,
                                        String[] selectionArgs) {
        final String column = MediaStore.MediaColumns.DATA;
        final String[] projection = {column};
        try (Cursor cursor = context.getContentResolver()
                .query(uri, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                final int columnIndex = cursor.getColumnIndex(column);
                if (columnIndex == -1)
                    return null;
                return cursor.getString(columnIndex);
            }
        } catch (Exception ignored) {
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
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
}
