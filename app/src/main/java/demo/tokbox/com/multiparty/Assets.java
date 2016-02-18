package demo.tokbox.com.multiparty;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import demo.tokbox.com.multiparty.generated.Configuration;

public class Assets {
    private static final String CONFIGURATION = "config.json";
    private Context _ctx;

    public Assets(Context ctx) {
        _ctx = ctx;
        /* copy config file to external location if its not there*/
        try {
            File config = new File(Environment.getExternalStorageDirectory(), CONFIGURATION);
            if (!config.exists()) {
                InputStream inStrm = _ctx.getAssets().open(CONFIGURATION);
                OutputStream outStrm = new FileOutputStream(config);
                byte[] data = new byte[inStrm.available()];
                inStrm.read(data);
                outStrm.write(data);
                inStrm.close();
                outStrm.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Configuration getConfiguration() {
        Configuration configuration = null;
        try {
            File config = new File(Environment.getExternalStorageDirectory(), CONFIGURATION);
            InputStream inStrm = new FileInputStream(config);
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
