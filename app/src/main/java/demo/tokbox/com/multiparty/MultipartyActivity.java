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

import java.util.ArrayList;

import demo.tokbox.com.multiparty.generated.Configuration;

public class MultipartyActivity
        extends Activity
        implements  View.OnClickListener,
                    Session.SessionListener,
                    Publisher.PublisherListener {
    private static final String         LOGTAG = "[MultipartyActivity]";
    private String                      _id;
    private Assets                      _assets;
    private ArrayList<RelativeLayout>   _subsrciberContainterLst;
    private ArrayList<ProgressBar>      _subscriberSpinnerLst;
    private RelativeLayout              _publisherContainer;
    private Button                      _endCallBtn;
    private Session                     _session;
    private Publisher                   _publisher;
    private ArrayList<Subscriber>       _subscriberLst;
    private ArrayList<Stream>           _streamLst;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiparty);
        // construct and/or assign members
        _id = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        _assets = new Assets(this);
        _subsrciberContainterLst = new ArrayList<>();
        _subsrciberContainterLst.add((RelativeLayout)findViewById(R.id.view_line1));
        _subsrciberContainterLst.add((RelativeLayout)findViewById(R.id.view_line2));
        _subsrciberContainterLst.add((RelativeLayout)findViewById(R.id.view_line3));
        _subsrciberContainterLst.add((RelativeLayout)findViewById(R.id.view_line4));
        _subscriberSpinnerLst = new ArrayList<>();
        _subscriberSpinnerLst.add((ProgressBar)findViewById(R.id.line1_spinner));
        _subscriberSpinnerLst.add((ProgressBar)findViewById(R.id.line2_spinner));
        _subscriberSpinnerLst.add((ProgressBar)findViewById(R.id.line3_spinner));
        _subscriberSpinnerLst.add((ProgressBar)findViewById(R.id.line4_spinner));
        _publisherContainer = (RelativeLayout)findViewById(R.id.view_publisher);
        _endCallBtn = (Button)findViewById(R.id.btn_endcall);
        _subscriberLst = new ArrayList<>();
        _streamLst  = new ArrayList<>();
        // connect ui callbacks
        _endCallBtn.setOnClickListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // TODO: pause session
    }

    @Override
    protected void onResume() {
        super.onResume();
        // TODO: resume session
    }

    @Override
    protected void onStop() {
        super.onStop();
        // TODO: end session
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_endcall:
                // TODO: end session
                break;
        }
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
        _streamLst.add(stream);
        _subscribeStream(stream);
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
       // TODO
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        // TODO
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
        // TODO
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
        // TODO
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

    private void _subscribeStream(Stream stream) {
        Subscriber subscriber = new Subscriber(this, stream);
        subscriber.setVideoListener(this);
        _session.subscribe(subscriber);
        _subscriberLst.add(subscriber);
        // put up spinner
        ProgressBar spinner = _subscriberSpinnerLst.get(_subscriberLst.size() - 1);
        spinner.setVisibility(View.VISIBLE);
    }

    private int _dpToPx(int dp) {
        double screenDensity = getResources().getDisplayMetrics().density;
        return (int) (screenDensity * (double) dp);
    }


}
