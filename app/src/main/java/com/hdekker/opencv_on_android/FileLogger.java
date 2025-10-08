package com.hdekker.opencv_on_android;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileLogger {

    private static final String TAG = "FileLogger";
    private File logFile;

    /**
     * Creates a new log file in the app's external files directory.
     * The filename will include the current timestamp.
     * @param context The application context to access file directories.
     */
    public FileLogger(Context context) {
        try {
            // Get a directory that's private to your app but on external storage
            // This is a good place for logs as it doesn't require special permissions on most API levels
            File logDir = context.getExternalFilesDir("logs");
            if (logDir != null && !logDir.exists()) {
                logDir.mkdirs(); // Create the directory if it doesn't exist
            }

            // Create a filename with a timestamp
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "log_" + timeStamp + ".json";
            this.logFile = new File(logDir, fileName);

            if (!logFile.exists()) {
                if (logFile.createNewFile()) {
                    Log.i(TAG, "Successfully created log file at: " + logFile.getAbsolutePath());
                } else {
                    Log.e(TAG, "Failed to create new log file on disk.");
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to create log file", e);
        }
    }

    /**
     * Appends a line of text to the log file.
     * Each line will be followed by a newline character.
     * @param text The string to append to the file.
     */
    public void append(String text) {
        if (logFile == null || !logFile.canWrite()) {
            Log.e(TAG, "Log file is not available for writing.");
            return;
        }

        try (FileWriter writer = new FileWriter(logFile, true)) { // 'true' for append mode
            writer.append(text);
            writer.append("\n"); // Add a new line
            writer.flush();
        } catch (IOException e) {
            Log.e(TAG, "Error writing to log file", e);
        }
    }
}
