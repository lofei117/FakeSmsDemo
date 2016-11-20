package info.lofei.app.smsdemo.service;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import info.lofei.app.smsdemo.receiver.SmsReceiver;

public class SmsService extends Service {

    private static final String TAG = SmsService.class.getName();

    public SmsService() {}


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equalsIgnoreCase(intent.getAction())) {
                handleMessageReceived(intent);
            }
            SmsReceiver.completeWakefulIntent(intent);
        }
        return START_STICKY;
    }

    private void handleMessageReceived(Intent intent) {
        if (intent == null) {
            return;
        }
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            SmsMessage[] messages = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
            } else {
                messages = getMessagesFromIntent(intent);
            }
            if (messages != null) {
                String sender = messages[0].getDisplayOriginatingAddress();
                String body = "";
                for (SmsMessage message : messages) {
                    body += message.getMessageBody();
                }
                Log.d(TAG, "Received msg: {Sender:" + sender + ", body:" + body + "}");
                Toast.makeText(this, "Received msg: {Sender:" + sender + ", body:" + body + "}", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Read the PDUs out of an SMS_RECEIVED_ACTION or DATA_SMS_RECEIVED_ACTION intent.
     *
     * @param intent the intent to read from
     * @return an array of SmsMessages for the PDUs
     */
    public static final SmsMessage[] getMessagesFromIntent(Intent intent) {
        Object[] messages = (Object[]) intent.getSerializableExtra("pdus");
        if (messages == null) {
            return null;
        }
        if (messages.length == 0) {
            return null;
        }

        byte[][] pduObjs = new byte[messages.length][];

        for (int i = 0; i < messages.length; i++) {
            pduObjs[i] = (byte[]) messages[i];
        }
        byte[][] pdus = new byte[pduObjs.length][];
        int pduCount = pdus.length;
        SmsMessage[] msgs = new SmsMessage[pduCount];
        for (int i = 0; i < pduCount; i++) {
            pdus[i] = pduObjs[i];
            msgs[i] = SmsMessage.createFromPdu(pdus[i]);
        }
        return msgs;
    }

}
