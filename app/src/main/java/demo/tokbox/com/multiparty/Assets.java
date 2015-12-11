package demo.tokbox.com.multiparty;

import android.content.Context;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import demo.tokbox.com.multiparty.generated.Configuration;

/**
 * Created by ardy on 12/10/15.
 */
public class Assets {
    private static final String CONFIGURATION = "config.json";
    private Context _ctx;

    public Assets(Context ctx) {
        _ctx = ctx;
    }

    public Configuration getConfiguration() {
        Configuration configuration = null;
        try {
            InputStream inStrm = _ctx.getAssets().open(CONFIGURATION);
            String jsonFile = IOUtils.toString(inStrm);
            configuration = new Configuration(new JSONObject(jsonFile));
            inStrm.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (DecoderException e) {
            e.printStackTrace();
        }
        return configuration;
    }
}
