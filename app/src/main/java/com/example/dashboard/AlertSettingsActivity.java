package com.example.dashboard;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

/**
 * Simple settings screen to choose how safety alerts notify the caregiver.
 * Options:
 *  - Sound + Vibrate
 *  - Vibrate only
 *  - Silent
 *
 * The choice is stored in SharedPreferences under key "alert_mode".
 */
public class AlertSettingsActivity extends AppCompatActivity {

    public static final String PREF_KEY_ALERT_MODE = "alert_mode";
    public static final String MODE_SOUND_VIBRATE = "SOUND_VIBRATE";
    public static final String MODE_VIBRATE_ONLY = "VIBRATE_ONLY";
    public static final String MODE_SILENT = "SILENT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_settings);

        ImageButton backButton = findViewById(R.id.backButton);
        RadioGroup radioGroup = findViewById(R.id.alertModeGroup);
        RadioButton soundVibrate = findViewById(R.id.radioSoundVibrate);
        RadioButton vibrateOnly = findViewById(R.id.radioVibrateOnly);
        RadioButton silent = findViewById(R.id.radioSilent);

        backButton.setOnClickListener(v -> finish());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String currentMode = prefs.getString(PREF_KEY_ALERT_MODE, MODE_SOUND_VIBRATE);

        if (MODE_VIBRATE_ONLY.equals(currentMode)) {
            vibrateOnly.setChecked(true);
        } else if (MODE_SILENT.equals(currentMode)) {
            silent.setChecked(true);
        } else {
            soundVibrate.setChecked(true);
        }

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String newMode = MODE_SOUND_VIBRATE;
            if (checkedId == R.id.radioVibrateOnly) {
                newMode = MODE_VIBRATE_ONLY;
            } else if (checkedId == R.id.radioSilent) {
                newMode = MODE_SILENT;
            }

            prefs.edit().putString(PREF_KEY_ALERT_MODE, newMode).apply();
        });
    }
}


