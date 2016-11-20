package info.lofei.app.smsdemo.receiver;

import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.support.v4.content.WakefulBroadcastReceiver;

import info.lofei.app.smsdemo.service.SmsService;

public class SmsReceiver extends WakefulBroadcastReceiver {

    public SmsReceiver() {}

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equalsIgnoreCase(intent.getAction())) {
            handleMessageReceived(context, intent);
        }
    }

    private void handleMessageReceived(Context context, Intent intent) {
        intent.setClass(context, SmsService.class);
        startWakefulService(context, intent);
    }
}
