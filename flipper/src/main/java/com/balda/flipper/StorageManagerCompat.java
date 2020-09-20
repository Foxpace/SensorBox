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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.DocumentsContract;

import org.json.JSONException;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.preference.PreferenceManager;

@SuppressWarnings("unused")
public class StorageManagerCompat {

    private static final String PREF_ROOTS = "flipper.roots";
    public static final String DEF_MAIN_ROOT = "defroot";
    public static final String DEF_SD_ROOT = "defsdcard";
    private Set<Root> roots = new HashSet<>();

    public StorageManagerCompat(@NonNull Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> roots = prefs.getStringSet(PREF_ROOTS, new HashSet<String>());
        for (String r : roots) {
            try {
                this.roots.add(Root.fromJson(r));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && roots.isEmpty()) {
            addRoot(context, DEF_MAIN_ROOT, Environment.getExternalStorageDirectory());
        }
    }

    /**
     * It returns an intent to be dispatched via startActivityResult
     *
     * @param context The context
     * @return Null if no permission is needed, the intent needed otherwise
     */
    @Nullable
    public Intent requireExternalAccess(Context context) {
        StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        if (sm == null)
            throw new UnsupportedOperationException();
        Intent i;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            i = sm.getPrimaryStorageVolume().createOpenDocumentTreeIntent();
        } else {
            return null;
        }
        return i;
    }

    /**
     * It returns an intent to be dispatched via startActivityResult to access to
     * the first removable no primary storage. This method requires at least Nougat
     * because on previous Android versions there's no reliable way to get the
     * volume/path of SdCard, and no, SdCard != External Storage.
     *
     * @param context The context
     * @return Null if no storage is found, the intent object otherwise
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Nullable
    public Intent requireSdCardAccess(Context context) {
        StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        if (sm == null)
            throw new UnsupportedOperationException();
        List<StorageVolume> volumes = sm.getStorageVolumes();
        for (StorageVolume s : volumes) {
            if (s.isRemovable()) {
                Intent i;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    i = s.createOpenDocumentTreeIntent();
                } else {
                    //Access to the entire volume is only available for non-primary volumes
                    if (s.isPrimary())
                        return null;
                    i = s.createAccessIntent(null);
                }
                return i;
            }
        }
        return null;
    }

    /**
     * Add a root to the repository
     *
     * @param context The context
     * @param name    A generic name assigned to the root, ex. MyRoot
     * @param file    A file path
     * @return The root object
     */
    @Nullable
    public Root addRoot(@NonNull Context context, @NonNull String name, @NonNull File file) {
        Root r = new Root(name, file);
        roots.add(r);
        saveRoots(context);
        return r;
    }

    /**
     * Add a root to the repository
     *
     * @param context The context
     * @param name    A generic name assigned to the root, ex. MyRoot
     * @param data    The intent returned in onActivityResult, see #requireExternalAccess
     * @return The root object
     */
    @Nullable
    public Root addRoot(@NonNull Context context, @NonNull String name, @NonNull Intent data) {
        Uri uri = data.getData();
        if (uri == null)
            return null;
        context.getContentResolver()
                .takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        Root r = new Root(name, uri);
        roots.add(r);
        saveRoots(context);
        return r;
    }

    /**
     * Delete a root object. Root objects are deleted by name.
     *
     * @param name The root name
     */
    public void deleteRoot(@NonNull final String name) {
        for (Iterator<Root> i = roots.iterator(); i.hasNext(); ) {
            Root element = i.next();
            if (element.getName().equals(name)) {
                i.remove();
            }
        }
    }

    /**
     * Get a root
     *
     * @param name Get root with a specific name
     * @return The first root matching the predicate
     */
    @Nullable
    public Root getRoot(String name) {
        for (Root r : roots) {
            if (r.getName().equals(name))
                return r;
        }
        return null;
    }

    private void saveRoots(@NonNull Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> toBeSaved = new HashSet<>();
        for (Root r : roots) {
            try {
                toBeSaved.add(r.toJson());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        prefs.edit().putStringSet(PREF_ROOTS, toBeSaved).apply();
    }

    /**
     * Utitly method to select root according to an uri
     *
     * @param context The context
     * @param uri     The uri used to select the volume
     * @return The root if exist or null
     */
    @Nullable
    public Root getRoot(@NonNull Context context, Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            StorageManager m = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            if (m == null || uri == null)
                return null;
            if (FileUtils.isMediaDocument(uri)) {
                StorageVolume volume;
                try {
                    volume = m.getStorageVolume(uri);
                } catch (Exception e) {
                    return null;
                }
                if (volume.isPrimary()) {
                    return getRoot(StorageManagerCompat.DEF_MAIN_ROOT);
                } else if (volume.isRemovable()) {
                    return getRoot(StorageManagerCompat.DEF_SD_ROOT);
                } else
                    return null;
            } else if (DocumentsContract.isDocumentUri(context, uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                if (split.length <= 1)
                    return null;
                final String type = split[0];
                if ("primary".equals(type))
                    return getRoot(StorageManagerCompat.DEF_MAIN_ROOT);
                else
                    return getRoot(StorageManagerCompat.DEF_SD_ROOT);
            }
        }
        return getRoot(StorageManagerCompat.DEF_MAIN_ROOT);
    }
}
