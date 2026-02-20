package com.munis.familyotp.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "onReceive Triggered! Action: " + intent.getAction());

        if (intent.getAction() != null && intent.getAction().equals(SMS_RECEIVED)) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                // PDU = Protocol Data Unit (standard format for SMS)
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    for (Object pdu : pdus) {
                        try {
                            SmsMessage smsMessage;
                            // Checking version for PDU format
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                String format = bundle.getString("format");
                                smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);
                            } else {
                                smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                            }

                            String sender = smsMessage.getDisplayOriginatingAddress();
                            String messageBody = smsMessage.getMessageBody();
                            long timestamp = smsMessage.getTimestampMillis();

                            Log.e(TAG, "SMS DECODED SUCCESS: " + sender + " -> " + messageBody);

                            // FORCE TOAST ON MAIN THREAD
                            Toast.makeText(context, "OTP DETECTED:\n" + messageBody, Toast.LENGTH_LONG).show();

                            // TODO: Trigger WorkManager to encrypt and upload

                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing SMS", e);
                        }
                    }
                } else {
                    Log.e(TAG, "Bundle has no PDUs!");
                }
            }
        }
    }
}
