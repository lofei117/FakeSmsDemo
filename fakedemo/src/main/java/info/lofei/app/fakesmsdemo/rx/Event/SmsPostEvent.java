package info.lofei.app.fakesmsdemo.rx.Event;

/**
 * SmsPostEvent.
 *
 * @author lofei lofei@lofei.info
 * @version 1.0.0 created at: 2016-11-20 14:21
 */
public class SmsPostEvent extends Event {

    private String mSender;

    private String mText;

    private String mEventProducer;

    public SmsPostEvent(String sender, String text, String eventProducer) {
        mSender = sender;
        mText = text;
        mEventProducer = eventProducer;
    }

    public void setSender(String sender) {
        mSender = sender;
    }

    public void setText(String text) {
        mText = text;
    }

    public void setEventProducer(String eventProducer) {
        mEventProducer = eventProducer;
    }

    public String getSender() {
        return mSender;
    }

    public String getText() {
        return mText;
    }

    public String getEventProducer() {
        return mEventProducer;
    }
}
