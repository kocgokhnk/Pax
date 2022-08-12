package com.ppltech.pax;

import androidx.appcompat.app.AppCompatActivity;


import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import com.pax.dal.IDAL;
import com.pax.dal.IPrinter;
import com.pax.dal.exceptions.PrinterDevException;
import com.pax.neptunelite.api.NeptuneLiteUser;
import com.ppltech.pax.Util.MyApplication;
import com.ppltech.pax.databinding.ActivityMainBinding;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainAct";
    private Socket socket;
    private ActivityMainBinding mainBinding;
    private String amount;
    private IPrinter printer;
    private IDAL idal;

    public static HashMap <String, String> isofields =new HashMap<String, String>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        try {
            idal = NeptuneLiteUser.getInstance().getDal(getApplicationContext());
            if (idal != null) {
                printer = idal.getPrinter();
                printer.init();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        MyApplication app = (MyApplication) getApplicationContext();
        socket = app.getSocket();
        socket.on(Socket.EVENT_CONNECT,onConnect);
        socket.on("resendAmount", resendAmount);
        socket.on(Socket.EVENT_DISCONNECT,onDisconnect);
        socket.connect();

        //3,11,12,13,43,63 which bits must be full
        //bitmap 0010000000111000000000000000000000000000001000000000000000000010
        isofields.put("MTI", "0200");
        isofields.put("BITMAP", "0010000000111000000000000000000000000000001000000000000000000010");

        initView();
    }


    public void initView(){
        mainBinding.amount.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                }
                return false;
            }
        });
        mainBinding.sale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                amount= mainBinding.amount.getText().toString().trim();
                socket.emit("amount",isofields);
                printReceiv(amount);
                mainBinding.returnPay.setVisibility(View.VISIBLE);
            }
        });
        mainBinding.returnPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),"Reimbursed Amount : "+amount,Toast.LENGTH_LONG).show();
            }
        });
    }
    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.wtf(TAG, "Socket Connected!");
                    socket.emit("register", 1);//1 is a id like userId
                }
            });
        }
    }; // connect to socket server
    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "diconnected");
                    socket.disconnect();
                    socket.off(Socket.EVENT_CONNECT, onConnect);
                    socket.off(Socket.EVENT_DISCONNECT, onDisconnect);
                }
            });
        }
    }; // disconnect to socket server
    private Emitter.Listener resendAmount = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(args[0].toString().length()>0){
                        Log.wtf(TAG, args[0].toString());
                        mainBinding.viewAmount.setText("Entered Amount : "+args[0].toString());
                    }
                }
            });
        }
    };// get amount from socket

    public void printReceiv(String amount){
        try {
            IPrinter printer= MyApplication.getDal().getPrinter();
            printer.printStr(amount,null);
            printer.step(150);// step for begining
            printer.start();
            PrintReset();
        } catch (PrinterDevException e) {
            e.printStackTrace();
        }
    }// print receive

    public void PrintReset() {
        try {
            printer.init();
        } catch (PrinterDevException e) {
            e.printStackTrace();
        }
    }//reset print
}
