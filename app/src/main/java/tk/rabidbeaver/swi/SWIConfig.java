package tk.rabidbeaver.swi;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SWIConfig extends AppCompatActivity {
    private int stored;
    private int[] slots = new int[]{16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 9, 8, 1, 3, 4, 2, 7};
    TextView swiadc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swiconfig);

        final Button swistart = (Button)findViewById(R.id.swi_start);
        final Button swirecord = (Button)findViewById(R.id.swi_record);
        final Button swisave = (Button)findViewById(R.id.swi_save);
        final Button swicancel = (Button)findViewById(R.id.swi_cancel);
        final LinearLayout swicontent = (LinearLayout)findViewById(R.id.swi_content);
        swiadc = (TextView)findViewById(R.id.swi_adc);
        final TextView swistored = (TextView)findViewById(R.id.swi_stored);

        final Button swiassign = (Button)findViewById(R.id.swi_assign);

        swistart.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                stored = 0;
                swistored.setText("KEYS STORED: "+stored);
                swicontent.setVisibility(View.VISIBLE);
                swistart.setEnabled(false);
                try {
                    ButtonService.mButtonService.os.write(Constants.MCUDCOMMANDS.start_detect, 0, 6);
                    ButtonService.mButtonService.os.write(Constants.MCUDCOMMANDS.clear, 0, 5);
                } catch (Exception e){
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
                    ButtonService.mButtonService.os.write(keyadc, 0, 6);
                } catch (Exception e){
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
                    ButtonService.mButtonService.os.write(Constants.MCUDCOMMANDS.save, 0, 5);
                    ButtonService.mButtonService.os.write(Constants.MCUDCOMMANDS.stop_detect, 0, 6);
                } catch (Exception e){
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
                    ButtonService.mButtonService.os.write(Constants.MCUDCOMMANDS.stop_detect, 0, 6);
                } catch (Exception e){
                    e.printStackTrace();
                }
                swistart.setEnabled(true);
                swicontent.setVisibility(View.GONE);
                Toast.makeText(getApplicationContext(), "SWI programming CANCELLED.", Toast.LENGTH_SHORT).show();
            }
        });

        swiassign.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v){
                startActivity(new Intent(getApplicationContext(), KeyConfiguration.class));
            }
        });

        Intent service = new Intent(this, ButtonService.class);
        service.setAction("start");
        startService(service);
    }
}
