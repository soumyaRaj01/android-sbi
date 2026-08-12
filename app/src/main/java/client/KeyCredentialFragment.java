package client;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Locale;
import io.android.sbi.R;
import io.android.sbi.crypto.AndroidKeystoreCryptoProvider;
import io.android.sbi.crypto.CryptoProvider;
import io.android.sbi.secureLib.DeviceKeystore;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class KeyCredentialFragment extends Fragment {

    public static final String KEY_TYPE_DEVICE = "Device Key";
    public static final String KEY_TYPE_FTM = "FTM Key";
    public static final String KEY_TYPE_IDA = "IDA_FIR Certificate";

    // the fragment initialization parameters
    public static final String ARG_KEY_LABEL = "keyLabel";

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    private TextView keyTextView;
    private TextView statusTextView;
    private TextView issuedOnTextView;
    private TextView expiresOnTextView;
    private String keyLabel;
    private CryptoProvider cryptoProvider;
    private DeviceKeystore deviceKeystore;

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

        cryptoProvider = new AndroidKeystoreCryptoProvider(requireContext().getApplicationContext());
        deviceKeystore = new DeviceKeystore(requireContext().getApplicationContext());
        keyLabel = getArguments() != null ? getArguments().getString(ARG_KEY_LABEL) : null;

        if (keyLabel != null) {
            keyTextView.setText(keyLabel);
        }

        refreshStatus();

        return rootView;
    }

    public void setKeyLabel(String keyLabel) {
        this.keyLabel = keyLabel;
        if (keyTextView != null) {
            keyTextView.setText(keyLabel);
            refreshStatus();
        }
    }

    public void showCheckingStatus() {
        statusTextView.setText(R.string.key_status_checking);
        issuedOnTextView.setText("");
        expiresOnTextView.setText("");
    }

    public void refreshStatus() {
        X509Certificate certificate;
        if (KEY_TYPE_FTM.equals(keyLabel)) {
            certificate = cryptoProvider.getFtmCertificate();
        } else if (KEY_TYPE_IDA.equals(keyLabel)) {
            certificate = deviceKeystore.getIdaCertificate();
        } else {
            certificate = cryptoProvider.getDeviceCertificate();
        }

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
}
