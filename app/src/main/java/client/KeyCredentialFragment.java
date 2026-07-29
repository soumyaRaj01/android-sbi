package client;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.Executors;

import io.android.sbi.R;
import io.android.sbi.constants.ClientConstants;
import io.android.sbi.crypto.AndroidKeystoreCryptoProvider;
import io.android.sbi.crypto.CryptoProvider;
import io.android.sbi.crypto.DmsClient;
import io.android.sbi.crypto.ProvisioningManager;
import io.android.sbi.utility.DeviceConstants;
import io.android.sbi.utility.Logger;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class KeyCredentialFragment extends Fragment {

    public static final String KEY_TYPE_DEVICE = "Device Key";
    public static final String KEY_TYPE_FTM = "FTM Key";

    // the fragment initialization parameters
    public static final String ARG_KEY_LABEL = "keyLabel";
    public static final String ARG_KEY_ALIAS = "keyAlias";
    public static final String ARG_PASSWORD = "password";

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    private TextView keyTextView;
    private TextView statusTextView;
    private TextView issuedOnTextView;
    private TextView expiresOnTextView;
    private String keyLabel;
    private CryptoProvider cryptoProvider;

    public KeyCredentialFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_key_credential, container, false);

        keyTextView = rootView.findViewById(R.id.key_label_textview);
        statusTextView = rootView.findViewById(R.id.key_status_textview);
        issuedOnTextView = rootView.findViewById(R.id.key_issued_on_textview);
        expiresOnTextView = rootView.findViewById(R.id.key_expires_on_textview);
        Button checkNowButton = rootView.findViewById(R.id.check_now_button);

        cryptoProvider = new AndroidKeystoreCryptoProvider(requireContext().getApplicationContext());
        keyLabel = getArguments() != null ? getArguments().getString(ARG_KEY_LABEL) : null;

        if (keyLabel != null) {
            keyTextView.setText(keyLabel);
        }

        checkNowButton.setOnClickListener(v -> checkNow());

        refreshStatus();

        return rootView;
    }

    private void checkNow() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(requireContext());

                String dmsBaseUrl = sharedPreferences.getString(
                        ClientConstants.DMS_BASE_URL,
                        ClientConstants.DEFAULT_DMS_BASE_URL);

                DmsClient dmsClient = new DmsClient(dmsBaseUrl);
                ProvisioningManager provisioningManager =
                        new ProvisioningManager(cryptoProvider, dmsClient);

                provisioningManager.initialize();
            } catch (Exception e) {
                Logger.e(DeviceConstants.LOG_TAG, "Check Now provisioning failed: " + e.getMessage());
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(this::refreshStatus);
            }
        });
    }

    private void refreshStatus() {
        X509Certificate certificate = KEY_TYPE_FTM.equals(keyLabel)
                ? cryptoProvider.getFtmCertificate()
                : cryptoProvider.getDeviceCertificate();

        if (certificate == null) {
            statusTextView.setText(R.string.key_status_not_provisioned);
            issuedOnTextView.setText("");
            expiresOnTextView.setText("");
            return;
        }

        statusTextView.setText(R.string.key_status_present);
        issuedOnTextView.setText(getString(R.string.issued_on, DATE_FORMAT.format(certificate.getNotBefore())));
        expiresOnTextView.setText(getString(R.string.expires_on, DATE_FORMAT.format(certificate.getNotAfter())));
    }

    // Retained so ConfigurationActivity's existing Save/Reset handlers (built around
    // the old manual P12 upload flow) keep compiling; the underlying alias/password/file
    // views no longer exist in this layout now that certs are fetched automatically.
    public String getKeyAlias() {
        return "";
    }

    public String getPassword() {
        return "";
    }

    public Uri getSelectedUri() {
        return null;
    }

    public void setValues(String keyAlias, String password, String lastUpdated) {
        // no-op: alias/password/file fields removed; certificate status is read
        // directly from CryptoProvider in refreshStatus() instead.
    }
}
