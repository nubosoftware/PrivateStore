package org.fdroid.fdroid.nubo;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class NuboUserApps {

    private static final String TAG = "NuboUserApps";
    private static NuboUserApps instance;



    public static NuboUserApps tryGetInstance() {
        return instance;
    }
    public static NuboUserApps getInstance(Context context) {
        if (instance == null) {
            instance = new NuboUserApps(context);
        }
        return instance;
    }

    private Context mContext;
    private BroadcastReceiver mReceiver;
    private Map<String,String> mCanInstallMap;
    private SharedPreferences mPref;
    private String lastUpdateHash;
    private String currentHash;
    private boolean changed;

    public NuboUserApps(Context context) {
        super();
        mContext = context;
        mCanInstallMap = new HashMap<>();


        mPref = context.getSharedPreferences(TAG, 0);
        lastUpdateHash = mPref.getString("last_hash","");
        changed = false;


    }

    public void update() {

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.nubo.nubosettings.CAN_INSTALL_APP_LIST_RESP");

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                Log.e(TAG,"onReceive");
                int status = intent.getIntExtra("com.nubo.nubosettings.STATUS",-1);
                if (status != 1) {
                    Log.e(TAG,"Error getting list. status: "+status);
                } else {
                    String list = intent.getStringExtra("com.nubo.nubosettings.APP_LIST");
                    if (list != null) {
                        Log.e(TAG,"Found app list: "+list);
                        currentHash = md5(list);
                        changed = (!currentHash.equals(lastUpdateHash));
                        Log.e(TAG,"currentHash: "+currentHash+", lastUpdateHash: "+lastUpdateHash+", changed: "+changed);
                        String[] arr = list.split(",");
                        synchronized (mCanInstallMap) {
                            mCanInstallMap.clear();
                            for (int i=0; i<arr.length; i++) {
                                mCanInstallMap.put(arr[i],"1");
                            }
                        }
                    } else {
                        Log.e(TAG,"Not found app list");
                    }
                }
                synchronized (NuboUserApps.this) {
                    NuboUserApps.this.notifyAll();
                }
            }
        };
        mContext.registerReceiver(mReceiver, filter,"com.nubo.nubosettings.CAN_INSTALL_APP_LIST_RESP_PERM",null);

        final Intent intent=new Intent();
        intent.setAction("com.nubo.nubosettings.CAN_INSTALL_APP_LIST");
        intent.setComponent(
            new ComponentName("com.nubo.nubosettings","com.nubo.nubosettings.receivers.AppHelper"));
        mContext.sendBroadcast(intent,"com.nubo.nubosettings.CAN_INSTALL_APP_LIST_PERM");

        Log.e(TAG,"Waiting for update...");
        synchronized (this) {
            try {
                this.wait(10000); // limit wait to 10 seconds
            } catch (InterruptedException e) {

            }
        }
        Log.e(TAG,"Finished. Changed: "+changed);

    }

    public boolean canInstallApp(String packageId) {
        synchronized (mCanInstallMap) {
            String res = mCanInstallMap.get(packageId);
            if (res != null && res.equals("1")) {
                return true;
            } else {
                return false;
            }
        }
    }

    private static final String md5(final String s) {
        final String MD5 = "MD5";
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance(MD5);
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG,"Error",e);
        }
        return "";
    }

    public boolean hasChanged() {
        return changed;
    }

    public void setUpdated() {
        lastUpdateHash = currentHash;
        changed = false;
        SharedPreferences.Editor editor = mPref.edit();
        editor.putString("last_hash",currentHash);
        editor.commit();
    }


}
