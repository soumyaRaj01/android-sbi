package io.android.sbi.log;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

public class LogExporter {

    public static void shareAppLogs(Context context) {
        // 1. Create a log file in the app's internal cache directory
        File logFile = new File(context.getCacheDir(), "app_logs.txt");

        try {
            // 2. Execute logcat command (-d dumps logs and exits immediately)
            Process process = Runtime.getRuntime().exec("logcat -d");
            InputStream inputStream = process.getInputStream();
            FileOutputStream outputStream = new FileOutputStream(logFile);

            // 3. Write data to the text file
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            // 4. Generate a secure FileProvider URI
            String authority = context.getPackageName() + ".fileprovider";
            Uri logUri = FileProvider.getUriForFile(context, authority, logFile);

            // 5. Create and launch the native sharing dialog
            if (logUri != null) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_STREAM, logUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Required for security

                context.startActivity(Intent.createChooser(shareIntent, "Share Tester Logs"));
            }

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to extract logs: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
