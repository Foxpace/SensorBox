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

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

public class DocumentFileCompat {

    private DocumentFileCompat() {

    }

    /**
     * Traverse a path of folders starting from a parent and it creates them
     * if they don't exist.
     *
     * @param parent The parent folder
     * @param names  The list of folders to be traversed
     * @return The last folder
     * @throws OperationFailedException if a folder can't be created
     */
    @NonNull
    public static DocumentFile getSubFolderTraverse(@NonNull DocumentFile parent, List<String> names) throws OperationFailedException {
        DocumentFile p = parent;
        for (String d : names) {
            p = getSubFolder(p, d);
        }
        return p;
    }

    /**
     * Traverse a path of folders starting from a parent and but it doesn't create them
     * if they don't exist.
     *
     * @param parent The parent folder
     * @param names  The list of folders to be traversed
     * @return The last existing folder on the path
     */
    @NonNull
    public static DocumentFile peekSubFolderTraverse(@NonNull DocumentFile parent, List<String> names) {
        DocumentFile p = parent;
        for (String d : names) {
            DocumentFile tmp = peekSubFolder(p, d);
            if (tmp == null)
                return p;
            p = tmp;
        }
        return p;
    }

    /**
     * Retrieve a sub folder if it exists
     *
     * @param parent The parent folder
     * @param name   The sub folder name
     * @return The sub folder or null if it doesn't exist
     */
    @Nullable
    public static DocumentFile peekSubFolder(@NonNull DocumentFile parent, String name) {
        DocumentFile interFolder = parent.findFile(name);
        if (interFolder != null && interFolder.isDirectory()) {
            return interFolder;
        } else {
            return null;
        }
    }

    /**
     * Retrieve a file if it exists comparing even its mime type if the parameter is not null
     *
     * @param folder The parent folder
     * @param name   The file name
     * @param mime   The file mime, it can be null
     * @return The file found
     */
    @Nullable
    public static DocumentFile peekFile(@NonNull DocumentFile folder, @NonNull String name, @Nullable String mime) {
        DocumentFile file = folder.findFile(name);
        if (file != null && file.isFile()) {
            if (mime != null) {
                if (mime.equals(file.getType()))
                    return file;
            } else {
                return file;
            }
        } else {
            return null;
        }
        return null;
    }

    /**
     * Retrieve a folder. The method creates the folder if it doesn't exist
     * @param parent The parent folder
     * @param name The folder name
     * @return The folder
     * @throws OperationFailedException if creation fail
     */
    @NonNull
    public static DocumentFile getSubFolder(DocumentFile parent, String name) throws OperationFailedException {
        DocumentFile interFolder = parent.findFile(name);
        if (interFolder != null && interFolder.isDirectory()) {
            return interFolder;
        } else {
            DocumentFile f = parent.createDirectory(name);
            if (f == null)
                throw new OperationFailedException("Impossible to create the folder");
            return f;
        }
    }

    /**
     * Retrieve a file. The method creates the file if it doesn't exist
     * @param folder The parent folder
     * @param name The file name
     * @param mime The file mime
     * @return The file
     * @throws OperationFailedException if creation fail
     */
    @NonNull
    public static DocumentFile getFile(@NonNull DocumentFile folder, @NonNull String name, @NonNull String mime) throws OperationFailedException {
        DocumentFile file = folder.findFile(name);
        if (file != null && file.isFile() && mime.equals(file.getType())) {
            return file;
        } else {
            DocumentFile f = folder.createFile(mime, name);
            if (f == null)
                throw new OperationFailedException("Impossible to create the file");
            return f;
        }
    }
}
