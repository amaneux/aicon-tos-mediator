package com.avlino.common.utils;

import java.io.File;
import java.io.IOException;

/**
 * Convenience utilities for File handling.
 */
public class FileUtils {

    /**
     * Makes sure that the file and the path to the file gets created.
     * @param filePath the full file path (dirs+filename)
     * @return the File object
     */
    static public File ensureFileWithPath(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            return file;
        } else {
            if (file.getParent() != null) {
                File path = new File(file.getParent());
                path.mkdirs();
            }
            return file;
        }
    }


    /**
     * Returns the full path of the given file when valid.
     * @param file the file to return its full file path (CanonicalPath)
     * @return the full file path or null when this produced an Exception or the file was null.
     */
    static public String getFullPath(File file) {
        if (file == null) {
            return null;
        }
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return null;
        }
    }

}
