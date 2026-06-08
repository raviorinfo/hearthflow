package com.example.hearthflow.security;

import android.content.Context;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class EncryptionManager {
    private static final String PREF_NAME = "secure_prefs";
    private static final String KEY_USER_DATA = "user_data";

    public static void saveUserData(Context context, String data) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ).edit().putString(KEY_USER_DATA, data).apply();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    public static String getUserData(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ).getString(KEY_USER_DATA, null);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
