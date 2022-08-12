package com.ppltech.pax.Util;

import static com.ppltech.pax.Util.StaticFields.SERVER_URL;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.pax.dal.IDAL;
import com.pax.neptunelite.api.NeptuneLiteUser;

import java.net.URISyntaxException;


import io.socket.client.IO;
import io.socket.client.Socket;
/**
 * Created by Gökhan Koç on 26,Temmuz,2022
 */
public class MyApplication extends Application {

    private static MyApplication _instance;
    private SharedPreferences _preferences;
    private static final String TAG = MyApplication.class
            .getSimpleName();
    public static MyApplication get() {
        return _instance;
    }
    public static synchronized MyApplication getInstance() {
        return _instance;
    }
    private static IDAL dal;
    private static Context appContext;

    private Socket socket;
    {
        try {
            IO.Options options = new IO.Options();
            SocketSSL.set(options);
            socket = IO.socket(SERVER_URL, options);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public Socket getSocket() {
        return socket;
    }

    @Override
    public void onCreate() {
        super.onCreate();


        _instance = this;
        _preferences = PreferenceManager.getDefaultSharedPreferences(this);
        appContext = getApplicationContext();
        dal = getDal();

    }
    public static IDAL getDal(){
        if(dal == null){
            try {
                long start = System.currentTimeMillis();
                dal = NeptuneLiteUser.getInstance().getDal(appContext);
                Log.i("Test","get dal cost:"+(System.currentTimeMillis() - start)+" ms");
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(appContext, "error occurred,DAL is null.", Toast.LENGTH_LONG).show();
            }
        }
        return dal;
    }

    public SharedPreferences getPreferences() {
        return _preferences;
    }
    public SharedPreferences.Editor getPreferencesEditor() {
        return _preferences.edit();
    }
}