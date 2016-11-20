package info.lofei.app.fakesmsdemo.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.provider.Telephony;
import android.support.annotation.Nullable;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;

import info.lofei.app.fakesmsdemo.rx.RxBus;
import info.lofei.app.fakesmsdemo.rx.Event.SmsPostEvent;


public class FakeSmsService extends Service {

    public static final String ACTION_SEND_SMS = "action_send_sms";

    public static final String ACTION_SEND_SMS_ADB = "action_send_sms_adb";

    public static final String ACTION_SEND_SMS_MAIN = "action_send_sms_main";

    public static final String EXTRA_SENDER = "extra_sender";

    public static final String EXTRA_TEXT = "extra_text";

    public FakeSmsService() {}


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onHandleIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    protected void onHandleIntent(Intent intent) {
        if (intent != null && !TextUtils.isEmpty(intent.getAction())) {
            String sender = intent.getStringExtra(EXTRA_SENDER);
            String text = intent.getStringExtra(EXTRA_TEXT);
            createFakeSms(this, sender, text);
            String producer = intent.getAction();
            RxBus.getDefault().post(new SmsPostEvent(sender, text, producer));
        }
    }


    private static void createFakeSms(Context context, String sender, String text) {
        Log.d("test", "Create fake sms--- sender :" + sender);
        Log.d("test", "Create fake sms--- text :" + text);
        Intent intent = getSmsIntent(sender, text);

        // Toast.makeText(context, "Sender:" + sender + ", Content:" + text,
        // Toast.LENGTH_SHORT).show();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
            String content = "";
            for (SmsMessage message : messages) {
                content += message.getMessageBody();
            }
            Log.d("test", "text:" + text);
            Log.d("test", "message:" + content);
        }


        intent.setClassName("info.lofei.app.smsdemo", "info.lofei.app.smsdemo.service.SmsService");
        context.startService(intent);
    }

    public static Intent getSmsIntent(String sender, String body) {
        SmsManager smsManager = SmsManager.getDefault();
        // 防止短信过长
        ArrayList<String> messages = smsManager.divideMessage(body);
        int size = messages.size();
        Object[] objArray = new Object[size];
        for (int i = 0; i < size; ++i) {
            byte[] pdus = createFakeSms(sender, messages.get(i));
            objArray[i] = pdus;
        }
        Intent intent = new Intent();
        intent.setAction("android.provider.Telephony.SMS_RECEIVED");
        intent.putExtra("pdus", objArray);
        intent.putExtra("format", "3gpp");
        intent.putExtra("subscription", 1);
        return intent;
    }

    // 创建pdu
    public static byte[] createFakeSms(String sender, String body) {
        byte[] pdu = null;
        byte[] scBytes = PhoneNumberUtils.networkPortionToCalledPartyBCD("0000000000");
        byte[] senderBytes = PhoneNumberUtils.networkPortionToCalledPartyBCD(sender);
        int lsmcs = scBytes.length;
        // 时间处理，包括年月日时分秒以及时区和夏令时
        byte[] dateBytes = new byte[7];
        Calendar calendar = new GregorianCalendar();
        dateBytes[0] = reverseByte((byte) (calendar.get(Calendar.YEAR)));
        dateBytes[1] = reverseByte((byte) (calendar.get(Calendar.MONTH) + 1));
        dateBytes[2] = reverseByte((byte) (calendar.get(Calendar.DAY_OF_MONTH)));
        dateBytes[3] = reverseByte((byte) (calendar.get(Calendar.HOUR_OF_DAY)));
        dateBytes[4] = reverseByte((byte) (calendar.get(Calendar.MINUTE)));
        dateBytes[5] = reverseByte((byte) (calendar.get(Calendar.SECOND)));
        dateBytes[6] = reverseByte((byte) ((calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET)) / (60 * 1000 * 15)));
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            bo.write(lsmcs);// 短信服务中心长度
            bo.write(scBytes);// 短信服务中心号码
            bo.write(0x04);
            bo.write((byte) sender.length());// 发送方号码长度
            bo.write(senderBytes);// 发送方号码
            bo.write(0x00);// 协议标示，00为普通GSM，点对点方式
            try {
                String className = "com.android.internal.telephony.GsmAlphabet";
                Class<?> clazz = Class.forName(className);
                Method method = clazz.getMethod("stringToGsm7BitPacked", new Class[] {String.class});
                method.setAccessible(true);
                byte[] bodybytes = (byte[]) method.invoke(null, body);

                bo.write(0x00); // encoding: 0 for default 7bit
                bo.write(dateBytes);
                bo.write(bodybytes);
            } catch (Exception e) {
                // 下面是UCS-2编码的处理，中文短信就需要用此种方式
                byte[] bodyBytes = encodeUCS2(body, null);
                bo.write(0x08); // encoding: 8 for UCS-2
                bo.write(dateBytes);
                bo.write(bodyBytes);// 其中encodeUCS2是从系统中复制过来的，并不是我写的
                // 源码具体位置在
                // frameworks/base/telephony/java/com/android/internal/telephony/gsm/SmsMessage.java
            }

            pdu = bo.toByteArray();
        } catch (IOException e) {
        }
        return pdu;
    }

    private static byte reverseByte(byte b) {
        return (byte) ((b & 0xF0) >> 4 | (b & 0x0F) << 4);
    }

    public static byte[] encodeUCS2(String message, byte[] header) throws UnsupportedEncodingException {
        byte[] userData, textPart;
        textPart = message.getBytes("utf-16be");

        if (header != null) {
            // Need 1 byte for UDHL
            userData = new byte[header.length + textPart.length + 1];

            userData[0] = (byte) header.length;
            System.arraycopy(header, 0, userData, 1, header.length);
            System.arraycopy(textPart, 0, userData, header.length + 1, textPart.length);
        } else {
            userData = textPart;
        }
        byte[] ret = new byte[userData.length + 1];
        ret[0] = (byte) (userData.length & 0xff);
        System.arraycopy(userData, 0, ret, 1, userData.length);
        return ret;
    }
}
