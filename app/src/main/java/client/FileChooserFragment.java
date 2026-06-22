package client;

import android.Manifest;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import io.mosip.mock.sbi.R;
import io.mosip.mock.sbi.utility.FileUtils;

/**
 * @author Anshul.Vanawat
 */

public class FileChooserFragment extends Fragment {
    public static final String ARG_LAST_UPLOAD_DATE = "lastUploadDate";
    private static final String TAG = "FileChooserFragment";

    private TextView editTextPath;
    private Uri selectedFileUri;

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<String> pickFileLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Permission launcher - handles runtime permission for READ_EXTERNAL_STORAGE or READ_MEDIA_*
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    Log.d(TAG, "Permission result: " + isGranted);
                    if (isGranted) {
                        doBrowseFile();
                    } else {
                        Log.d(TAG, "Permission denied");
                        Toast.makeText(getContext(), "Permission denied. Cannot access files.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // File picker launcher - uses Storage Access Framework (modern approach)
        pickFileLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    Log.d(TAG, "Picker returned uri: " + uri);
                    if (uri != null) {
                        selectedFileUri = uri;
                        try {
                            Pair<String, String> fileNameAndSize = FileUtils.getFileNameAndSize(getContext(), selectedFileUri);
                            if (editTextPath != null) {
                                editTextPath.setText(String.format("%s (%s)", fileNameAndSize.first, fileNameAndSize.second));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading file name/size", e);
                            Toast.makeText(getContext(), "Error reading file info", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_file_chooser, container, false);

        editTextPath = rootView.findViewById(R.id.last_upload_date);
        assert getArguments() != null;
        String lastUploadedDate = getArguments().getString(ARG_LAST_UPLOAD_DATE);
        editTextPath.setText(lastUploadedDate);

        Button buttonBrowse = rootView.findViewById(R.id.button_browse);
        buttonBrowse.setOnClickListener(view -> {
            Log.d(TAG, "Browse button clicked");
            askPermissionAndBrowseFile();
        });

        Log.d(TAG, "onCreateView completed");
        return rootView;
    }

    private void askPermissionAndBrowseFile() {
        Log.d(TAG, "askPermissionAndBrowseFile called");

        // Determine which permission to request based on Android version
        String permissionToRequest = Manifest.permission.READ_EXTERNAL_STORAGE;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requires READ_MEDIA_* instead of READ_EXTERNAL_STORAGE
            permissionToRequest = Manifest.permission.READ_MEDIA_IMAGES;
        }

        int permission = ActivityCompat.checkSelfPermission(requireContext(), permissionToRequest);

        if (permission != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission not granted. Requesting: " + permissionToRequest);
            requestPermissionLauncher.launch(permissionToRequest);
        } else {
            Log.d(TAG, "Permission already granted, opening file picker");
            doBrowseFile();
        }
    }

    private void doBrowseFile() {
        Log.d(TAG, "doBrowseFile called - launching file picker");
        // GetContent uses Storage Access Framework (SAF), no extra permissions needed
        pickFileLauncher.launch("*/*");
    }

    public Uri getSelectedUri() {
        return selectedFileUri;
    }

    public void resetSelection(String lastUploadDate) {
        selectedFileUri = null;
        if (editTextPath != null)
            editTextPath.setText(lastUploadDate);
    }
}
