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
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
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
    protected Map<Integer,Long> fields;


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
        int MTI = 0x0200;
        // And your Bitmap should be calculated like this:
        long BitMap = 0;
        HashMap<Integer, Long> myMap = new HashMap<Integer, Long>();
        myMap.put(3,BitMap);
        myMap.put(11,BitMap);
        myMap.put(12,BitMap);
        myMap.put(13,BitMap);
        myMap.put(43,BitMap);
        myMap.put(63,BitMap);

        setIsoField(myMap);


        initView();
    }
    public void setIsoField(HashMap<Integer, Long>map){
        int biggest_value=0;
        long maxValueInMap=(Collections.max(map.values()));
        for (Map.Entry<Integer, Long> entry : map.entrySet()) {  // Itrate through hashmap
            if (entry.getValue()==maxValueInMap) {
                biggest_value = entry.getKey();
            }
        }
        int[] arr = new int[biggest_value+1];
        long result =0;

        for(Map.Entry<Integer, Long> entry : map.entrySet()){
            result = (arr[arr.length - 1] >> entry.getKey()-1) | 1 ;
            arr[entry.getKey()-1] = (int)result ;
        }
        getBitmap(arr);
    }

    public void getBitmap(int[] bits){

        assert bits.length % 8 == 0;

        byte[] bytes = new byte[bits.length / 8];
        for (int i = 0; i < bytes.length; i++) {
            int b = 0;
            for (int j = 0; j < 8; j++)
                b = (b << 1) + bits[i * 8 + j];
            bytes[i] = (byte)b;
        }

        for (int i = 0; i < bytes.length; i++)
            System.out.printf("%02x ", bytes[i]); // prints: 5e 3d
        System.out.println();
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
                socket.emit("amount",amount);
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
