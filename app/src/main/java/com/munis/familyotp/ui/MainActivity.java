package com.munis.familyotp.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.munis.familyotp.R;
import com.munis.familyotp.network.SheetService;
import com.munis.familyotp.security.KeyManager;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private TextView statusText;
    private TextView otpDisplayText;
    private Button grantPermissionsButton;
    private Button fetchOtpButton;
    private KeyManager keyManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.status_text);
        otpDisplayText = findViewById(R.id.otp_display_text);
        grantPermissionsButton = findViewById(R.id.btn_grant_permissions);
        fetchOtpButton = findViewById(R.id.btn_fetch_otp);

        keyManager = new KeyManager(this);

        setupTestConfig();

        grantPermissionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAndRequestPermissions();
            }
        });

        fetchOtpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchAndDecryptLastOtp();
            }
        });

        checkAndRequestPermissions();
    }

    private void setupTestConfig() {
        keyManager.saveSheetUrl(
                "https://script.google.com/macros/s/AKfycbwzPpBw9gKqJK6UrfVfiDLe3nDAHaP_yQ3cea4AehnPmskEvLpre_w3aUHn4WDGlSFfjg/exec");
        keyManager.saveUserName("TestUser");
        if (!keyManager.hasKeys()) {
            try {
                java.security.KeyPairGenerator generator = java.security.KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                java.security.KeyPair keyPair = generator.generateKeyPair();
                String publicKeyBase64 = android.util.Base64.encodeToString(keyPair.getPublic().getEncoded(),
                        android.util.Base64.NO_WRAP);
                String privateKeyBase64 = android.util.Base64.encodeToString(keyPair.getPrivate().getEncoded(),
                        android.util.Base64.NO_WRAP);
                keyManager.importKeys(publicKeyBase64, privateKeyBase64);
                Log.e(TAG, "Test keys generated and imported");
            } catch (Exception e) {
                Log.e(TAG, "Failed to generate test keys", e);
            }
        }
    }

    private void checkAndRequestPermissions() {
        String[] permissions = {
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.CAMERA
        };

        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
            statusText.setText("Permissions Required");
            grantPermissionsButton.setVisibility(View.VISIBLE);
        } else {
            statusText.setText("All Permissions Granted");
            grantPermissionsButton.setVisibility(View.GONE);
            showMainContent();
        }
    }

    private void showMainContent() {
        if (keyManager.hasKeys() && !keyManager.getSheetUrl().isEmpty()) {
            fetchOtpButton.setVisibility(View.VISIBLE);
            fetchAndDecryptLastOtp();
        } else {
            statusText.setText("Keys not configured. Please set up the app first.");
        }
    }

    private void fetchAndDecryptLastOtp() {
        statusText.setText("Fetching latest OTP...");
        fetchOtpButton.setEnabled(false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                SheetService sheetService = new SheetService(keyManager.getSheetUrl());
                String encrypted = sheetService.fetchLastOtp();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        fetchOtpButton.setEnabled(true);

                        if (encrypted == null || encrypted.isEmpty()) {
                            statusText.setText("No OTP found in sheet");
                            otpDisplayText.setVisibility(View.GONE);
                            return;
                        }

                        String decrypted = keyManager.decrypt(encrypted);
                        if (decrypted != null) {
                            statusText.setText("Last OTP:");
                            otpDisplayText.setText(decrypted);
                            otpDisplayText.setVisibility(View.VISIBLE);
                        } else {
                            statusText.setText("Failed to decrypt OTP");
                            otpDisplayText.setVisibility(View.GONE);
                        }
                    }
                });
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                statusText.setText("All Permissions Granted");
                grantPermissionsButton.setVisibility(View.GONE);
                Toast.makeText(this, "Permissions Granted!", Toast.LENGTH_SHORT).show();
                showMainContent();
            } else {
                statusText.setText("Permissions Denied");
                Toast.makeText(this, "Permissions are required for the app to function.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
