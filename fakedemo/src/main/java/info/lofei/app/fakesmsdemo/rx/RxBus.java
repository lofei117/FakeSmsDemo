package info.lofei.app.fakesmsdemo.rx;


import info.lofei.app.fakesmsdemo.rx.Event.Event;
import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

/**
 * RxBus.
 *
 * @author lofei lofei@lofei.info
 * @version 1.0.0 created at: 2016-05-24 10:47
 */
public class RxBus {

    private static volatile RxBus sDefaultInstance;

    private Subject<Event, Event> mBus;

    private RxBus() {
        mBus = new SerializedSubject<>(PublishSubject.<Event>create());
    }

    public static RxBus getDefault() {
        RxBus rxBus = sDefaultInstance;
        if (rxBus == null) {
            synchronized (RxBus.class) {
                rxBus = sDefaultInstance;
                if (rxBus == null) {
                    rxBus = new RxBus();
                    sDefaultInstance = rxBus;
                }
            }
        }
        return rxBus;
    }

    public void post(Event e) {
        mBus.onNext(e);
    }

    public <T extends Event> Observable<T> observer(Class<T> eventType) {
        return mBus.ofType(eventType);
    }
}
