package demo.tokbox.com.multiparty;

import android.app.Activity;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.OpenTokConfig;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import demo.tokbox.com.multiparty.generated.Configuration;

public class MultipartyActivity
        extends Activity
        implements  View.OnClickListener,
                    Session.SessionListener,
                    Session.ReconnectionListener,
                    Publisher.PublisherListener,
                    Subscriber.VideoListener {
    private static final String            LOGTAG = "[MultipartyActivity]";
    private String                          _id;
    private ArrayList<SubscriberContainer>  _subsrciberLst;
    private LinearLayout                    _subscriberContainer;
    private FrameLayout                     _publisherContainer;
    private Button                          _endCallBtn;
    private Session                         _session;
    private Publisher                       _publisher;
    private Configuration                   _config;

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
        _subsrciberLst = new ArrayList<>();
        _subscriberContainer = (LinearLayout)findViewById(R.id.view_subscriber);
        _publisherContainer = (FrameLayout)findViewById(R.id.view_publisher);
        _endCallBtn = (Button)findViewById(R.id.btn_endcall);
        _config = (new Assets(this)).getConfiguration();
        // connect UI
        _endCallBtn.setOnClickListener(this);
        // connect to session
        try {
            OpenTokConfig.setAPIRootURL(_config.getAPIUrl(), false);
            if (null != _config.getForceSimulcast()) {
                OpenTokConfig.enableSimulcast(_config.getForceSimulcast());
            }
            _connectSession(_config);
        } catch (MalformedURLException e) {
                Log.d(LOGTAG, "EXCEPTION: " + e.getLocalizedMessage());
                e.printStackTrace();
            }
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
        Map<String, Publisher.CameraCaptureResolution> configResMapping =
                new HashMap<String, Publisher.CameraCaptureResolution>() {
            {
                put("LOW", Publisher.CameraCaptureResolution.LOW);
                put("MEDIUM", Publisher.CameraCaptureResolution.MEDIUM);
                put("HIGH", Publisher.CameraCaptureResolution.HIGH);
            }
        };
        _publisher = new Publisher(
                this,
                "MultipartyActivity-demo: publisher-" + _id,
                (null != _config.getPublisherResolution())
                        ? configResMapping.get(_config.getPublisherResolution())
                        : Publisher.CameraCaptureResolution.MEDIUM,
                Publisher.CameraCaptureFrameRate.FPS_30
        );
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
        // create subscriber view
        SubscriberContainer subscriber = _createSubscriberView();
        _subsrciberLst.add(subscriber);
        _subscriberContainer.addView(subscriber.getContainer());
        // subscribe to stream
        _subscribeStream(subscriber, stream);
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        SubscriberContainer removeSubscriber = null;
        for (SubscriberContainer subscriber : _subsrciberLst) {
            if (null != subscriber.getStream() && subscriber.getStream().equals(stream)) {
                _unsubscribeStream(subscriber);
                _subscriberContainer.removeView(subscriber.getContainer());
                removeSubscriber = subscriber;
                break;
            }
        }
        if (null != removeSubscriber) {
            _subsrciberLst.remove(removeSubscriber);
        }
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        Log.i(LOGTAG, "OpenTok Session error:" + opentokError.getMessage());
    }

    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
        Toast.makeText(
                this,
                "Publisher Stream Id: " + stream.getStreamId(),
                Toast.LENGTH_LONG
        ).show();
        Log.d(LOGTAG, "Publisher Stream Id: " + stream.getStreamId() + " Created");
    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {
        Log.d(LOGTAG, "Publisher Stream Id: " + stream.getStreamId() + " Destroyed");
    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {
        Log.i(LOGTAG, "OpenTok Publisher error:" + opentokError.getMessage());
    }

    @Override
    public void onVideoDataReceived(SubscriberKit subscriberKit) {
        for (SubscriberContainer subscriber: _subsrciberLst) {
            if (null != subscriber.getStream() &&
                    subscriber.getStream().equals(subscriberKit.getStream())) {
                subscriber.getSpinner().setVisibility(View.GONE);
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

    @Override
    public void onReconnecting(Session session) {
        Log.i(LOGTAG, "Session Reconnecting...");
    }

    @Override
    public void onReconnected(Session session) {
        Log.i(LOGTAG, "Session Reconnected!");
    }

    private void _connectSession(Configuration config) {
        _session = new Session(
                this,
                config.getSessionKey(),
                config.getSessionID()
        );
        _session.setSessionListener(this);
        _session.setReconnectionListener(this);
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

    private SubscriberContainer _createSubscriberView() {
        // construct layout
        RelativeLayout subscriberLayout = new RelativeLayout(this);
        ProgressBar subscriberSpinner = new ProgressBar(this);
        SubscriberContainer container = new SubscriberContainer(subscriberLayout, subscriberSpinner);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(640, 480);
        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        // wire up view
        subscriberLayout.addView(subscriberSpinner, layoutParams);
        return container;
    }

    private void _subscribeStream(SubscriberContainer container, Stream stream) {
        Subscriber subscriber = new Subscriber(this, stream);
        subscriber.setStyle(
                BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FILL
        );
        subscriber.setVideoListener(this);
        _session.subscribe(subscriber);
        container.setSubscriber(subscriber);
        container.setStream(stream);
        // add subsriber to container view
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(640, 480);
        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        container.getContainer().addView(subscriber.getView(), layoutParams);
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
