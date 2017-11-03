package tk.rabidbeaver.swi;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SWIConfig extends AppCompatActivity {
    LocalSocket mSocket;
    DataInputStream is;
    DataOutputStream os;

    private int stored;
    private int[] slots = new int[]{16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 9, 8, 1, 3, 4, 2, 7};

    private byte[] adc = {0, 0, 0, 0, 0, 0};
    private String adcStatus = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swiconfig);

        mSocket = new LocalSocket();

        try {
            mSocket.connect(new LocalSocketAddress("/dev/car/keys", LocalSocketAddress.Namespace.FILESYSTEM));
            is = new DataInputStream(mSocket.getInputStream());
            os = new DataOutputStream(mSocket.getOutputStream());
        } catch (Exception e){
            e.printStackTrace();
            finish();
        }

        final Button swistart = (Button)findViewById(R.id.swi_start);
        final Button swirecord = (Button)findViewById(R.id.swi_record);
        final Button swisave = (Button)findViewById(R.id.swi_save);
        final Button swicancel = (Button)findViewById(R.id.swi_cancel);
        final LinearLayout swicontent = (LinearLayout)findViewById(R.id.swi_content);
        final TextView swiadc = (TextView)findViewById(R.id.swi_adc);
        final TextView swistored = (TextView)findViewById(R.id.swi_stored);

        final byte start_detect[] = {(byte)0xaa, 0x55, 0x02, 0x01, 0x01, 0x02};
        final byte stop_detect[] = {(byte)0xaa, 0x55, 0x02, 0x01, 0x00, 0x03};
        final byte clear[] = {(byte)0xaa, 0x55, 0x02, 0x02, 0x00};
        final byte save[] = {(byte)0xaa, 0x55, 0x02, 0x03, 0x01};

        swistart.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                stored = 0;
                swistored.setText("KEYS STORED: "+stored);
                swicontent.setVisibility(View.VISIBLE);
                swistart.setEnabled(false);
                try {
                    os.write(start_detect, 0, 6);
                    os.write(clear, 0, 5);
                } catch (IOException e){
                    e.printStackTrace();
                }
                swiadc.setText("");
            }
        });

        swirecord.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                byte[] keyadc = {(byte)0xaa, 0x55, 0x02, 0x04, (byte)slots[stored], 0x00};
                keyadc[5] = (byte)((int)keyadc[2] ^ (int)keyadc[3] ^ (int)keyadc[4]);
                try {
                    os.write(keyadc, 0, 6);
                } catch (IOException e){
                    e.printStackTrace();
                }
                stored++;
                swistored.setText("KEYS STORED: "+stored);
                Toast.makeText(getApplicationContext(), "SWI key recorded.", Toast.LENGTH_SHORT).show();
            }
        });

        swisave.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                try {
                    os.write(save, 0, 5);
                    os.write(stop_detect, 0, 6);
                } catch (IOException e){
                    e.printStackTrace();
                }
                swistart.setEnabled(true);
                swicontent.setVisibility(View.GONE);
                Toast.makeText(getApplicationContext(), "SWI programming SAVED.", Toast.LENGTH_SHORT).show();
            }
        });

        swicancel.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                try {
                    os.write(stop_detect, 0, 6);
                } catch (IOException e){
                    e.printStackTrace();
                }
                swistart.setEnabled(true);
                swicontent.setVisibility(View.GONE);
                Toast.makeText(getApplicationContext(), "SWI programming CANCELLED.", Toast.LENGTH_SHORT).show();
            }
        });

        new Thread() {
            @Override
            public void run() {
                int len, i;
                try {
                    while(true) {
                        if (is.readByte() != 0xaa) continue;
                        if (is.readByte() != 0x55) continue;
                        len = (int)is.readByte();
                        for (i=0; i<len; i++) adc[i] = is.readByte();

                        adcStatus = "KEY 1: ["+adc[0]+", "+adc[2]+", "+adc[4]+"], "
                                +"KEY2: ["+adc[1]+", "+adc[3]+", "+adc[5]+"]";
                        runOnUiThread(new Runnable() {
                            public void run() {
                                swiadc.setText(adcStatus);
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }
}
