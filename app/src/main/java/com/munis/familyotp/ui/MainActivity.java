package com.munis.familyotp.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.munis.familyotp.R;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private TextView statusText;
    private Button grantPermissionsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.status_text);
        grantPermissionsButton = findViewById(R.id.btn_grant_permissions);

        grantPermissionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAndRequestPermissions();
            }
        });

        checkAndRequestPermissions();
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
            // TODO: Proceed to next logic (Biometric check)
        }
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
            } else {
                statusText.setText("Permissions Denied");
                Toast.makeText(this, "Permissions are required for the app to function.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
