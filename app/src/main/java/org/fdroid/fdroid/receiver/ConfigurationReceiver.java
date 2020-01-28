package org.fdroid.fdroid.receiver;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import org.fdroid.fdroid.AddRepoIntentService;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.data.Schema;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

public class ConfigurationReceiver extends BroadcastReceiver {

    private static final String TAG = "ConfigurationReceiver";
    private static final String SEND_CONF_REQUEST = "com.nubo.appstore.SEND_CONF_REQUEST";


    @Override
    public void onReceive(Context context, Intent intent) {


        Log.i(TAG,"onReceive. "+intent.getAction());
        if (SEND_CONF_REQUEST.equals(intent.getAction())) {
            String address = intent.getStringExtra("repo_address");
            if (address == null) {
                Log.e(TAG,"Invalid address");
                return;
            }
            try {
                address = AddRepoIntentService.normalizeUrl(address);

            } catch (URISyntaxException e) {
                // Leave address as it was.
            }

            String fingerprint = intent.getStringExtra("repo_fingerprint");
            String username = intent.getStringExtra("repo_username");
            String password = intent.getStringExtra("repo_password");

            ContentValues values = new ContentValues(4);
            values.put(Schema.RepoTable.Cols.ADDRESS, address);
            if (!TextUtils.isEmpty(fingerprint)) {
                values.put(Schema.RepoTable.Cols.FINGERPRINT, fingerprint.toUpperCase(Locale.ENGLISH));
            }
            if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
                values.put(Schema.RepoTable.Cols.USERNAME, username);
                values.put(Schema.RepoTable.Cols.PASSWORD, password);
            }

            // remove all old repos
            List<Repo> repos = RepoProvider.Helper.all(context);
            for (int i=0; i<repos.size(); i++) {
                Repo repo = repos.get(i);
                Log.i(TAG,"Remove old repo. id: "+repo.getId()+", address: "+repo.address);
                RepoProvider.Helper.remove(context,repo.getId());
            }


            Uri newUri = RepoProvider.Helper.insert(context, values);
            Log.i(TAG,"Add new repo. uri: "+newUri+", address: "+address);

            // force process to stop so new app reload the repos
            Runtime.getRuntime().exit(0);
        }



    }
}
