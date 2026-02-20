package com.munis.familyotp.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

public class KeyManager {

    private static final String PREFS_NAME = "family_otp_keys";
    private static final String KEY_PUBLIC = "public_key";
    private static final String KEY_PRIVATE = "private_key";
    private static final String KEY_SHEET_URL = "sheet_url";
    private static final String KEY_USER_NAME = "user_name";
    private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";

    private SharedPreferences prefs;

    public KeyManager(Context context) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            prefs = EncryptedSharedPreferences.create(
                    PREFS_NAME,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create encrypted preferences", e);
        }
    }

    public void importKeys(String publicKeyBase64, String privateKeyBase64) {
        prefs.edit()
                .putString(KEY_PUBLIC, publicKeyBase64)
                .putString(KEY_PRIVATE, privateKeyBase64)
                .apply();
    }

    public void saveSheetUrl(String url) {
        prefs.edit().putString(KEY_SHEET_URL, url).apply();
    }

    public String getSheetUrl() {
        return prefs.getString(KEY_SHEET_URL, "");
    }

    public void saveUserName(String name) {
        prefs.edit().putString(KEY_USER_NAME, name).apply();
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "");
    }

    public boolean hasKeys() {
        return prefs.contains(KEY_PUBLIC) && prefs.contains(KEY_PRIVATE);
    }

    public String encrypt(String plainText) {
        try {
            String publicKeyBase64 = prefs.getString(KEY_PUBLIC, "");
            byte[] keyBytes = Base64.decode(publicKeyBase64, Base64.NO_WRAP);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(spec);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes("UTF-8"));
            return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String decrypt(String encryptedText) {
        try {
            String privateKeyBase64 = prefs.getString(KEY_PRIVATE, "");
            byte[] keyBytes = Base64.decode(privateKeyBase64, Base64.NO_WRAP);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(spec);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.decode(encryptedText, Base64.NO_WRAP));
            return new String(decryptedBytes, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
