package info.lofei.app.fakesmsdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import info.lofei.app.fakesmsdemo.rx.RxBus;
import info.lofei.app.fakesmsdemo.rx.Event.SmsPostEvent;
import info.lofei.app.fakesmsdemo.service.FakeSmsService;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.et_sender)
    EditText mETSender;

    @BindView(R.id.et_text)
    EditText mETText;

    @BindView(R.id.tv_records)
    TextView mTVRecords;

    private Subscription mSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSubscription = RxBus.getDefault().observer(SmsPostEvent.class) //
                .observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<SmsPostEvent>() {
                    @Override
                    public void call(SmsPostEvent smsPostEvent) {
                        String record = String.format("Sender: %s, text: %s, producer: %s\n", smsPostEvent.getSender(), smsPostEvent.getText(),
                                smsPostEvent.getEventProducer());
                        String s = mTVRecords.getText().toString() + record;
                        mTVRecords.setText(s);
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSubscription != null) {
            mSubscription.unsubscribe();
            mSubscription = null;
        }
    }

    @OnClick(R.id.btn_send)
    void send() {
        String sender = mETSender.getText().toString();
        String text = mETText.getText().toString();
        Intent intent = new Intent(this, FakeSmsService.class);
        intent.setAction(FakeSmsService.ACTION_SEND_SMS_MAIN);
        intent.putExtra(FakeSmsService.EXTRA_SENDER, sender);
        intent.putExtra(FakeSmsService.EXTRA_TEXT, text);
        startService(intent);
    }
}
