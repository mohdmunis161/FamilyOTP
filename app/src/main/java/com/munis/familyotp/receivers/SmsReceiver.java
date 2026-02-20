package com.munis.familyotp.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.munis.familyotp.network.SheetService;
import com.munis.familyotp.security.KeyManager;

public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "onReceive Triggered! Action: " + intent.getAction());

        if (intent.getAction() != null && intent.getAction().equals(SMS_RECEIVED)) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    for (Object pdu : pdus) {
                        try {
                            SmsMessage smsMessage;
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                String format = bundle.getString("format");
                                smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);
                            } else {
                                smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                            }

                            String sender = smsMessage.getDisplayOriginatingAddress();
                            String messageBody = smsMessage.getMessageBody();

                            Log.e(TAG, "SMS from: " + sender + " -> " + messageBody);

                            KeyManager keyManager = new KeyManager(context);
                            if (!keyManager.hasKeys()) {
                                Log.e(TAG, "No keys imported yet, skipping encryption");
                                return;
                            }

                            String encrypted = keyManager.encrypt(messageBody);
                            if (encrypted == null) {
                                Log.e(TAG, "Encryption failed");
                                return;
                            }

                            Log.e(TAG,
                                    "Encrypted: " + encrypted.substring(0, Math.min(50, encrypted.length())) + "...");

                            String sheetUrl = keyManager.getSheetUrl();
                            if (sheetUrl.isEmpty()) {
                                Log.e(TAG, "No sheet URL configured, skipping upload");
                                return;
                            }

                            String userName = keyManager.getUserName();

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    SheetService sheetService = new SheetService(sheetUrl);
                                    boolean success = sheetService.postOtp(sender, userName, encrypted);
                                    Log.e(TAG, "Upload " + (success ? "SUCCESS" : "FAILED"));
                                }
                            }).start();

                        } catch (Exception e) {
                            Log.e(TAG, "Error processing SMS", e);
                        }
                    }
                }
            }
        }
    }
}
