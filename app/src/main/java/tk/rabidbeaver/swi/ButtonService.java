package tk.rabidbeaver.swi;

import android.accessibilityservice.AccessibilityService;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ButtonService extends AccessibilityService {
    LocalSocket mSocket;
    DataInputStream is;
    DataOutputStream os;
    static ButtonService mButtonService = null;
    SWIConfig mSWIConfig = null;
    private String adcStatus = "";
    private byte[] adc = {0, 0, 0, 0, 0, 0};

    private byte[] sendkey = {(byte)0xaa, 0x55, 0x02, 0x05, 0x00, 0x00};

    protected static boolean loaded = false;
    private static String[] actions = new String[256];
    private static int[] actionTypes = new int[256];

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    private void activity(Context context, String component){
        Intent sIntent = new Intent(Intent.ACTION_MAIN);
        sIntent.setComponent(ComponentName.unflattenFromString(component));
        sIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        sIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(sIntent);
    }

    private void broadcast(Context context, String action){
        Intent sIntent = new Intent();
        sIntent.setAction(action);
        context.sendBroadcast(sIntent);
    }

    private void key(Context context, int keycode){
        Intent buttonServiceIntent;
        switch(keycode){
            case 0x03:
                buttonServiceIntent = new Intent(context, ButtonService.class);
                buttonServiceIntent.setAction("HOME");
                context.startService(buttonServiceIntent);
                break;
            case 0x04:
                buttonServiceIntent = new Intent(context, ButtonService.class);
                buttonServiceIntent.setAction("BACK");
                context.startService(buttonServiceIntent);
                break;
            case 0xbb:
                buttonServiceIntent = new Intent(context, ButtonService.class);
                buttonServiceIntent.setAction("RECENT");
                context.startService(buttonServiceIntent);
                break;
            default:
                sendkey[4] = (byte)keycode;
                sendkey[5] = (byte)(sendkey[2] ^ sendkey[3] ^ sendkey[4]);
                try {
                    os.write(sendkey, 0, 6);
                } catch (IOException e){
                    e.printStackTrace();
                }
        }
    }

    private void execKey(int keycode){
        if (!loaded){
            SharedPreferences keyActionStore = getSharedPreferences("keyActionStore", Context.MODE_PRIVATE);
            for (int i=0; i<256; i++){
                actionTypes[i] = Constants.ACTIONTYPES.NULL;
                if (keyActionStore.contains(Integer.toString(i))){
                    String keyString = keyActionStore.getString(Integer.toString(i), null);
                    if (keyString != null){
                        actions[i] = keyString.substring(1);
                        actionTypes[i] = Integer.parseInt(keyString.substring(0,1));
                    }
                }
            }
            loaded = true;
        }

        if (keycode >= 0 && keycode < 256){
            switch(actionTypes[keycode]){
                case Constants.ACTIONTYPES.BROADCAST_INTENT:
                    broadcast(this, actions[keycode]);
                    break;
                case Constants.ACTIONTYPES.ACTIVITY_INTENT:
                    activity(this, actions[keycode]);
                    break;
                case Constants.ACTIONTYPES.KEYCODE:
                    key(this, Integer.parseInt(actions[keycode]));
                    break;
                default:
                    key(this, keycode);
            }
        }
    }

    @Override
    public void onCreate(){
        super.onCreate();

        mSocket = new LocalSocket();

        try {
            mSocket.connect(new LocalSocketAddress("/dev/car/keys", LocalSocketAddress.Namespace.FILESYSTEM));
            is = new DataInputStream(mSocket.getInputStream());
            os = new DataOutputStream(mSocket.getOutputStream());
        } catch (Exception e){
            e.printStackTrace();
            this.stopSelf();
        }

        try {
            os.write(Constants.MCUDCOMMANDS.stop_detect, 0, 6);
        } catch (IOException e){
            e.printStackTrace();
        }

        new Thread() {
            @Override
            public void run() {
                int len, i, cmd;
                byte b;
                try {
                    while(true) {
                        b = is.readByte();
                        if (b != (byte)0xaa) continue;
                        if (is.readByte() != 0x55) continue;
                        len = (int)is.readByte();
                        cmd = is.readByte();
                        for (i=0; i<len-1; i++) adc[i] = is.readByte();

                        switch (cmd){
                            case 0x01:
                                adcStatus = "KEY 1: [0x" + Integer.toHexString(0xff & adc[0]) + ", 0x" + Integer.toHexString(0xff & adc[2]) + ", 0x" + Integer.toHexString(0xff & adc[4]) + "], "
                                        + "KEY2: [0x" + Integer.toHexString(0xff & adc[1]) + ", 0x" + Integer.toHexString(0xff & adc[3]) + ", 0x" + Integer.toHexString(0xff & adc[5]) + "]";
                                if (mSWIConfig != null)
                                    mSWIConfig.runOnUiThread(new Runnable() {
                                        public void run() {
                                            mSWIConfig.swiadc.setText("ADC" + adcStatus);
                                        }
                                    });
                                break;
                            case 0x02:
                                execKey(adc[0]);
                                break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    @Override
    public void onDestroy(){
        mButtonService = null;
        super.onDestroy();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("ButtonService", "started");

        mButtonService = this;

        boolean result = false;
        String act = intent.getAction();
        switch (act){
            case "BACK":
                result = performGlobalAction(GLOBAL_ACTION_BACK);
                break;
            case "HOME":
                result = performGlobalAction(GLOBAL_ACTION_HOME);
                break;
            case "RECENT":
                result = performGlobalAction(GLOBAL_ACTION_RECENTS);
                break;
        }

        if (!result) Log.d("ButtonService", "result is FALSE");

        return Service.START_STICKY;
    }
}
