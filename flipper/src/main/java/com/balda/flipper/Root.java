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
import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

@SuppressWarnings("unused")
public class Root implements Parcelable, Comparable<Root> {

    private String name;
    private Uri uri;

    Root(@NonNull String name, @NonNull Uri uri) {
        this.name = name;
        this.uri = uri;
    }

    Root(@NonNull String name, @NonNull File file) {
        this.name = name;
        this.uri = DocumentFile.fromFile(file).getUri();
    }

    Root() {
    }

    protected Root(Parcel in) {
        name = in.readString();
        int present = in.readInt();
        if (present == 1)
            uri = in.readParcelable(Uri.class.getClassLoader());
    }

    public static final Creator<Root> CREATOR = new Creator<Root>() {
        @Override
        public Root createFromParcel(Parcel in) {
            return new Root(in);
        }

        @Override
        public Root[] newArray(int size) {
            return new Root[size];
        }
    };

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name);
        if (uri != null) {
            parcel.writeInt(1);
            parcel.writeParcelable(uri, i);
        } else
            parcel.writeInt(0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Root root = (Root) o;
        return Objects.equals(name, root.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    /**
     * It returns a DocumentFile object for this tree root
     *
     * @param context The context
     * @return A DocumentFile object
     */
    public DocumentFile toRootDirectory(Context context) {
        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            //noinspection ConstantConditions
            return DocumentFile.fromFile(new File(uri.getPath()));
        } else {
            return DocumentFile.fromTreeUri(context, uri);
        }
    }

    public boolean isAccessGranted(Context context) {
        DocumentFile f = toRootDirectory(context);
        if (f == null)
            return false;
        return f.canRead() && f.canWrite();
    }

    static Root fromJson(String json) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);
        Root root = new Root();
        root.name = jsonObject.getString("name");
        String uri = jsonObject.optString("uri");
        if (!uri.isEmpty())
            root.uri = Uri.parse(uri);
        return root;
    }

    String toJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        jsonObject.put("uri", uri.toString());
        return jsonObject.toString();
    }

    @Override
    public int compareTo(@NonNull Root root) {
        return name.compareTo(root.getName());
    }
}
