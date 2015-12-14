package demo.tokbox.com.multiparty;

import android.app.Activity;
import android.os.Bundle;
import android.provider.Settings;
import android.text.style.SubscriptSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;

import java.util.ArrayList;

import demo.tokbox.com.multiparty.generated.Configuration;

public class MultipartyActivity
        extends Activity
        implements  View.OnClickListener,
                    Session.SessionListener,
                    Publisher.PublisherListener,
                    Subscriber.VideoListener {
    private static final String          LOGTAG = "[MultipartyActivity]";
    private String                          _id;
    private Assets                          _assets;
    private ArrayList<SubscriberContainer>  _subsrciberLst;
    private RelativeLayout                  _publisherContainer;
    private Button                          _endCallBtn;
    private Session                         _session;
    private Publisher                       _publisher;

    private static class SubscriberContainer {
        private Subscriber      _subscriber;
        private Stream          _stream;
        private ProgressBar     _spinner;
        private RelativeLayout  _container;

        public SubscriberContainer(RelativeLayout container, ProgressBar spinner) {
            _subscriber = null;
            _stream     = null;
            _spinner    = spinner;
            _container  = container;
        }

        public Subscriber getSubscriber() {
            return _subscriber;
        }

        public Stream getStream() {
            return _stream;
        }

        public ProgressBar getSpinner() {
            return _spinner;
        }

        public RelativeLayout getContainer() {
            return _container;
        }

        public void setSubscriber(Subscriber subscriber) {
            _subscriber = subscriber;
        }

        public void setStream(Stream stream) {
            _stream = stream;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiparty);
        // construct and/or assign members
        _id = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        _assets = new Assets(this);
        _subsrciberLst = new ArrayList<>();
        _subsrciberLst.add(
                new SubscriberContainer(
                        (RelativeLayout)findViewById(R.id.view_line1),
                        (ProgressBar)findViewById(R.id.line1_spinner)
                )
        );
        _subsrciberLst.add(
                new SubscriberContainer(
                        (RelativeLayout)findViewById(R.id.view_line2),
                        (ProgressBar)findViewById(R.id.line2_spinner)
                )
        );
        _subsrciberLst.add(
                new SubscriberContainer(
                        (RelativeLayout) findViewById(R.id.view_line3),
                        (ProgressBar) findViewById(R.id.line3_spinner)
                )
        );
        _subsrciberLst.add(
                new SubscriberContainer(
                        (RelativeLayout)findViewById(R.id.view_line4),
                        (ProgressBar)findViewById(R.id.line4_spinner)
                )
        );
        _publisherContainer = (RelativeLayout)findViewById(R.id.view_publisher);
        _endCallBtn = (Button)findViewById(R.id.btn_endcall);
        // connect ui callbacks
        _endCallBtn.setOnClickListener(this);
        // connect to session
        _connectSession(_assets.getConfiguration());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (null != _session) {
            _session.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (null != _session) {
            _session.onResume();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isFinishing()) {
            _endSession();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_endcall:
                _endSession();
                System.exit(0);
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (_session != null) {
            _session.disconnect();
        }

        super.onBackPressed();
    }

    @Override
    public void onConnected(Session session) {
        Log.i(LOGTAG, "Connected to OpenTok Session");
        _publisher = new Publisher(this, "MultipartyActivity-demo: publisher-" + _id);
        _publisher.setPublisherListener(this);
        _setupPublisherView(_publisher);
        _session.publish(_publisher);
    }

    @Override
    public void onDisconnected(Session session) {
        if (null != _publisher) {
            _publisherContainer.removeView(_publisher.getView());
        }
        // null everything out
        _publisher = null;
        _session = null;
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        for (SubscriberContainer subscriber: _subsrciberLst) {
            if (null == subscriber.getStream()) {
                _subscribeStream(subscriber, stream);
            }
        }

    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        for (SubscriberContainer subscriber: _subsrciberLst) {
            if (subscriber.getStream().equals(stream)) {
                _unsubscribeStream(subscriber);
            }
        }
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        Log.i(LOGTAG, "OpenTok Session error:" + opentokError.getMessage());
    }

    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
        // TODO
    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {
        // TODO
    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {
        Log.i(LOGTAG, "OpenTok Publisher error:" + opentokError.getMessage());
    }

    @Override
    public void onVideoDataReceived(SubscriberKit subscriberKit) {
        for (SubscriberContainer subscriber: _subsrciberLst) {
            if (subscriber.getStream().equals(subscriberKit.getStream())) {
                _setupSubscriberView(subscriber);
            }
        }
    }

    @Override
    public void onVideoDisabled(SubscriberKit subscriberKit, String s) {
        Log.i(LOGTAG, "Video Disabled: " + s);
    }

    @Override
    public void onVideoEnabled(SubscriberKit subscriberKit, String s) {
        Log.i(LOGTAG, "Video Enabled: " + s);
    }

    @Override
    public void onVideoDisableWarning(SubscriberKit subscriberKit) {
        Log.i(
                LOGTAG,
                "Video may be disabled soon due to network quality degradation. Add UI handling here."
        );
    }

    @Override
    public void onVideoDisableWarningLifted(SubscriberKit subscriberKit) {
        Log.i(
                LOGTAG,
                "Video may no longer be disabled as stream quality improved. Add UI handling here."
        );
    }

    private void _connectSession(Configuration config) {
        _session = new Session(
                this,
                config.getSessionKey(),
                config.getSessionID()
        );
        _session.setSessionListener(this);
        _session.connect(config.getSessionToken());
    }

    private void _endSession() {
        if (null != _session)
            _session = null;
    }

    private void _setupPublisherView(Publisher publisher) {
        publisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(320, 240);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        layoutParams.bottomMargin   = _dpToPx(8);
        layoutParams.rightMargin    = _dpToPx(8);
        _publisherContainer.addView(publisher.getView(), layoutParams);
    }

    private void _setupSubscriberView(SubscriberContainer subscriber) {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(320, 240);
        subscriber.getContainer().removeView(subscriber.getSubscriber().getView());
        subscriber.getContainer().addView(subscriber.getSubscriber().getView(), layoutParams);
        subscriber.getSubscriber().setStyle(
                BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FILL
        );
    }

    private void _subscribeStream(SubscriberContainer container, Stream stream) {
        Subscriber subscriber = new Subscriber(this, stream);
        subscriber.setVideoListener(this);
        _session.subscribe(subscriber);
        container.setSubscriber(subscriber);
        container.setStream(stream);
        // put up spinner
        container.getSpinner().setVisibility(View.VISIBLE);
    }

    private void _unsubscribeStream(SubscriberContainer subscriber) {
        subscriber.getContainer().removeView(subscriber.getSubscriber().getView());
        subscriber.getSpinner().setVisibility(View.GONE);
        subscriber.setStream(null);
    }

    private int _dpToPx(int dp) {
        double screenDensity = getResources().getDisplayMetrics().density;
        return (int) (screenDensity * (double) dp);
    }
}
