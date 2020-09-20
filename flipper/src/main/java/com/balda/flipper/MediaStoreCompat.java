/*
 * Copyright (c) 2019 Marco Stornelli
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.balda.flipper;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.text.TextUtils;

import java.io.File;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;


import static com.balda.flipper.MediaStoreCompat.Status.DELETE;

@SuppressWarnings("unused")
public class MediaStoreCompat {

    private Context context;
    private String folder;
    private String subFolder;

    @IntDef({Status.DONE, DELETE, Status.ASYNC})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Status {
        int DONE = 0;
        int DELETE = 1;
        int ASYNC = 2;
    }


    public interface MediaStoreCompatListener {
        @Status
        int onResult(Uri result);
    }

    /**
     * Create a new publisher
     *
     * @param c The content
     * @param f One of Directory defined in Environment class
     */
    public MediaStoreCompat(@NonNull Context c, @NonNull String f) {
        context = c;
        folder = f;
    }

    /**
     * Optional custom sub folder
     *
     * @param sub The sub folder name
     */
    public void setSubFolder(String sub) {
        subFolder = sub;
    }

    public void saveImage(@NonNull FileDescription description,
                          @NonNull MediaStoreCompatListener l) throws MediaStoreCompatException {
        saveMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, description, l);
    }

    public void saveVideo(@NonNull FileDescription description,
                          @NonNull MediaStoreCompatListener l) throws MediaStoreCompatException {
        saveMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, description, l);
    }

    public void saveAudio(@NonNull FileDescription description,
                          @NonNull MediaStoreCompatListener l) throws MediaStoreCompatException {
        saveMedia(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, description, l);
    }

    /**
     * Dispose the result uri
     *
     * @param uri The uri returned in onResult callback
     */
    public void dispose(@NonNull Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0);
            try {
                resolver.update(uri, contentValues, null, null);
            } catch (Exception ignored) {
            }
        }
    }

    private void saveMedia(Uri uri, FileDescription description,
                           @NonNull MediaStoreCompatListener l) throws MediaStoreCompatException {
        ContentResolver resolver = context.getContentResolver();
        OutputStream os;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, description.getName());
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, description.getMime());
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1);
            String imagesDir = folder;
            if (!TextUtils.isEmpty(subFolder)) {
                imagesDir += File.separator + subFolder;
            }
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, imagesDir);
            contentValues.put(MediaStore.MediaColumns.OWNER_PACKAGE_NAME, context.getPackageName());
            Uri uriRes = resolver.insert(uri, contentValues);
            if (uriRes == null)
                throw new MediaStoreCompatException(new RemoteException());
            int res = l.onResult(uriRes);
            switch (res) {
                case Status.DELETE:
                    resolver.delete(uriRes, null, null);
                    break;
                default:
                case Status.DONE:
                    contentValues.clear();
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0);
                    resolver.update(uriRes, contentValues, null, null);
                    break;
                case Status.ASYNC:
                    break;
            }
        } else {
            File extDir = Environment.getExternalStoragePublicDirectory(folder);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState(extDir))) {
                    throw new MediaStoreCompatException("External storage not currently available");
                }
            } else {
                if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState())) {
                    throw new MediaStoreCompatException("External storage not currently available");
                }
            }

            String extPath = extDir.toString();
            if (!TextUtils.isEmpty(subFolder)) {
                extPath += File.separator + subFolder;
            }
            File path = new File(extPath);
            if (!path.exists()) {
                //noinspection ResultOfMethodCallIgnored
                path.mkdir();
            }
            File f = new File(path, description.getFullName());
            int res = l.onResult(Uri.fromFile(f));
            switch (res) {
                case Status.DELETE:
                    //noinspection ResultOfMethodCallIgnored
                    f.delete();
                    break;
                default:
                case Status.DONE:
                case Status.ASYNC:
                    break;
            }
        }
    }
}
