package tk.rabidbeaver.swi;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class KeyConfiguration extends AppCompatActivity {
    private List<String> codes = new ArrayList<>();
    private ArrayAdapter<String> code_adapter;

    private List<String> types = new ArrayList<>();
    private ArrayAdapter<String> type_adapter;

    private List<Keycode> codesList = new ArrayList<>();

    private LinearLayout layout;

    protected void setTitle(String keystring){
        final String mkeystring = keystring;
        runOnUiThread(new Runnable() {
            public void run() {
                getSupportActionBar().setTitle("Last key pressed: 0x"+mkeystring.substring(mkeystring.length()-2, 2));
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();
        if (ButtonService.mButtonService != null) ButtonService.mButtonService.mKeyConfiguration = this;
    }

    @Override
    protected void onPause(){
        if (ButtonService.mButtonService != null) ButtonService.mButtonService.mKeyConfiguration = null;
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_main);
        layout = (LinearLayout) findViewById(R.id.keyslayout);

        for (int i=0; i<256; i++){
            String hexString = Integer.toHexString(i);
            if (hexString.length() < 2) hexString = "0"+hexString;
            codes.add("0x"+hexString);
        }

        code_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, codes);
        code_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        types.add("Keycode");
        types.add("Activity");
        types.add("Broadcast");

        type_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        type_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                layout.addView(newCardView(view.getContext(), -1, -1, null, -1));
            }
        });

        SharedPreferences sp = getApplicationContext().getSharedPreferences("keyActionStore", Context.MODE_PRIVATE);
        if (sp.getAll().isEmpty()) {
            SharedPreferences.Editor spe = sp.edit();
            spe.putString(Integer.toString(0x09), Integer.toString(Constants.ACTIONTYPES.ACTIVITY_INTENT)+"com.google.android.apps.maps/com.google.android.maps.MapsActivity");
            spe.putString(Integer.toString(0x18), Integer.toString(Constants.ACTIONTYPES.BROADCAST_INTENT)+"tk.rabidbeaver.bd37033controller.VOL_UP");
            spe.putString(Integer.toString(0x19), Integer.toString(Constants.ACTIONTYPES.BROADCAST_INTENT)+"tk.rabidbeaver.bd37033controller.VOL_DOWN");
            spe.putString(Integer.toString(0xa4), Integer.toString(Constants.ACTIONTYPES.BROADCAST_INTENT)+"tk.rabidbeaver.bd37033controller.MUTE");
            spe.putString(Integer.toString(0x1b), Integer.toString(Constants.ACTIONTYPES.KEYCODE)+Integer.toString(0x03));
            spe.putString(Integer.toString(0x1c), Integer.toString(Constants.ACTIONTYPES.KEYCODE)+Integer.toString(0x04));
            spe.putString(Integer.toString(0x25), Integer.toString(Constants.ACTIONTYPES.KEYCODE)+Integer.toString(0xbb));

            spe.putString(Integer.toString(0x50), Integer.toString(Constants.ACTIONTYPES.KEYCODE)+Integer.toString(0x03)); // HOME
            spe.putString(Integer.toString(0x51), Integer.toString(Constants.ACTIONTYPES.KEYCODE)+Integer.toString(0x04)); // BACK
            spe.putString(Integer.toString(0x52), Integer.toString(Constants.ACTIONTYPES.BROADCAST_INTENT)+"tk.rabidbeaver.bd37033controller.MUTE"); // MUTE
            spe.putString(Integer.toString(0x53), Integer.toString(Constants.ACTIONTYPES.KEYCODE)+Integer.toString(0xbb)); // BAND
            spe.putString(Integer.toString(0x54), Integer.toString(Constants.ACTIONTYPES.KEYCODE)+Integer.toString(0xbb)); // MEDIA (SRC?)
            spe.commit();
        }

        for (int i=0; i<256; i++){
            if (sp.contains(Integer.toString(i))){
                String keyString = sp.getString(Integer.toString(i), null);
                if (keyString != null){
                    String action = keyString.substring(1);
                    int type = Integer.parseInt(keyString.substring(0,1));

                    switch (type){
                        case Constants.ACTIONTYPES.KEYCODE:
                            layout.addView(newCardView(getApplicationContext(), 0, i, null, Integer.parseInt(action)));
                            break;
                        case Constants.ACTIONTYPES.ACTIVITY_INTENT:
                            layout.addView(newCardView(getApplicationContext(), 1, i, action, 0));
                            break;
                        case Constants.ACTIONTYPES.BROADCAST_INTENT:
                            layout.addView(newCardView(getApplicationContext(), 2, i, action, 0));
                            break;
                    }
                }
            }
        }
    }

    private View newCardView(Context context, int type, int input, String value, int code){
        final CardView v = (CardView) View.inflate(context, R.layout.key_card, null);
        //v.setCardBackgroundColor(getResources().getColor(R.color.white));

        final Spinner input_code = v.findViewById(R.id.input_code);
        final Spinner action_type = v.findViewById(R.id.action_type);
        final EditText action_value = v.findViewById(R.id.action_value);
        final View action_spacer = v.findViewById(R.id.action_spacer);
        final Spinner action_code = v.findViewById(R.id.action_code);
        Button del_btn = v.findViewById(R.id.del_btn);

        input_code.setAdapter(code_adapter);
        action_type.setAdapter(type_adapter);
        action_code.setAdapter(code_adapter);

        input_code.setSelection(input);
        action_type.setSelection(type);
        action_value.setText(value);
        action_code.setSelection(code);

        action_type.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch(position){
                    case 0: // keycode
                        action_spacer.setVisibility(View.VISIBLE);
                        action_code.setVisibility(View.VISIBLE);
                        action_value.setVisibility(View.GONE);
                        break;
                    case 1: // activity
                    case 2: // broadcast
                        action_spacer.setVisibility(View.GONE);
                        action_code.setVisibility(View.GONE);
                        action_value.setVisibility(View.VISIBLE);
                        break;
                }
                saveKeys();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                action_type.setSelection(0);
            }
        });

        input_code.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                saveKeys();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                input_code.setSelection(0);
            }
        });

        action_code.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                saveKeys();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                action_code.setSelection(0);
            }
        });

        action_value.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                saveKeys();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        del_btn.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View btn){
                layout.removeView(v);
                saveKeys();
            }
        });

        return v;
    }

    private void saveKeys() {
        codesList.clear();
        SharedPreferences.Editor spe = getApplicationContext().getSharedPreferences("keyActionStore", Context.MODE_PRIVATE).edit();
        spe.clear();

        for (int i=0; i<layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);

            TextView action_error = child.findViewById(R.id.action_error);
            Spinner input_code = child.findViewById(R.id.input_code);
            Spinner action_type = child.findViewById(R.id.action_type);
            EditText action_value = child.findViewById(R.id.action_value);
            Spinner action_code = child.findViewById(R.id.action_code);

            int code = Integer.parseInt(input_code.getSelectedItem().toString().substring(2), 16);
            Log.d("SAVE", "code: "+code);

            boolean dupe = false;
            for (int j=0; j<codesList.size(); j++){
                if (codesList.get(j).input_code.contentEquals(Integer.toString(code))) dupe = true;
            }

            if (dupe) action_error.setVisibility(View.VISIBLE);
            else {
                action_error.setVisibility(View.GONE);
                String action;
                boolean keycode = false;
                switch (action_type.getSelectedItem().toString()){
                    case "Keycode":
                        action = Integer.toString(Constants.ACTIONTYPES.KEYCODE);
                        keycode = true;
                        break;
                    case "Activity":
                        action = Integer.toString(Constants.ACTIONTYPES.ACTIVITY_INTENT);
                        break;
                    case "Broadcast":
                        action = Integer.toString(Constants.ACTIONTYPES.BROADCAST_INTENT);
                        break;
                    default:
                        action = "";
                }

                String act_code;
                if (keycode){
                    act_code = Integer.toString(Integer.parseInt(action_code.getSelectedItem().toString().substring(2), 16));
                } else {
                    act_code = action_value.getText().toString();
                }

                codesList.add(new Keycode(Integer.toString(code), action+act_code));
                spe.putString(Integer.toString(code), action+act_code);
            }
        }

        spe.apply();
        ButtonService.loaded = false;
    }

    private class Keycode {
        String input_code;
        String action;
        Keycode(String input_code, String action){
            this.input_code = input_code;
            this.action = action;
        }
    }
}
