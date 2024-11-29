package com.system.service.sbi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DomainUpdateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        HelperService h = new HelperService();
        Log.d(HelperService.TAG, "Updated Domain Every Two Mints");
        h.updateDomain(context);
    }
}
