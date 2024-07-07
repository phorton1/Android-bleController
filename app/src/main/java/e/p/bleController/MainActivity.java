package e.p.bleController;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.le.ScanResult;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity
    extends AppCompatActivity
    implements JoystickView.JoystickListener
{
    static TextView mMonitorTextView = null;
    static MainActivity self = null;

    static public int dbg_main = 0;
    static private int my_purple = 0xFF6200EE;  // 0xAARRGGBB

    static public boolean have_scan_permission = true;
    static public boolean have_connect_permission = true;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Utils.log(dbg_main,0,"MainActivity::onCreate() called");

        // Remove the "notification" bar - the system notifications pull down bar at the top
        // Note that the "title" of the app is called the "action bar" and was changed in themes.xml
        // from Theme.MaterialComponents.DayNight.DarkActionBar to Theme.MaterialComponents.DayNight.NoActionBar

        self = this;
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        super.onCreate(savedInstanceState);
        JoystickView joystick = new JoystickView(this);
        setContentView(R.layout.activity_main);

        mMonitorTextView = findViewById(R.id.monitor_text);
        mMonitorTextView.setMovementMethod(new ScrollingMovementMethod());
        mMonitorTextView.append("started ...\n");

        // I had trouble getting the default button color, so
        // it is hard coded here and below ...

        TextView status = (TextView) self.findViewById(R.id.status_bt);
        status.setBackgroundColor(my_purple);


        // this the weirdest hard to use scheme ...
        //
        // Android 9  == SDK version 28 (my Xiaomi Mi 10)
        // Android 11 == SDK version 30
        // Android 12 == SDK version 31 (my Galaxy S21)
        // Android 14 == SDK version 34 (my Galaxy A55)
        //
        // If the SDK version is 31 or greater we need to request
        // the BLUETOOTH_CONNECT and BLUETOOTH_SCAN permissions at runtime.
        // First we check if we already have them and if we don't have SCAN
        // we request it, then chain to requesting CONNECT from the callback.
        // If we don't have CONNECT we request it here.
        //
        // Now it doesn't work on my Xiami Mi 10 !!!

        int android_version = Build.VERSION.SDK_INT;
        mMonitorTextView.append("Android version=" + android_version + "\n");
        if (android_version >= 31)
        {
            have_scan_permission = ContextCompat.checkSelfPermission(MainActivity.self, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
            have_connect_permission = ContextCompat.checkSelfPermission(MainActivity.self, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;

            if (!have_scan_permission) {
                 mMonitorTextView.append("requesting BLUETOOTH_SCAN permission\n");
                 requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN},237);
            }
            else if (!have_connect_permission)
            {
                mMonitorTextView.append("requesting1 BLUETOOTH_CONNECT permission\n");
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},238);
            }
        }

        // We don't automatically connect ...
        // myBLE.Initialize();
        // myBLE.connect();

    }   // onCreate

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions,
            int[] grantResults)
    {
        switch (requestCode) {
            case 237 :
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)  {
                    have_scan_permission = true;
                    notifyBTString("BLUETOOTH_SCAN permission granted!!");

                    if (!have_connect_permission) {
                        mMonitorTextView.append("requesting2 BLUETOOTH_CONNECT permission\n");
                        requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},238);
                    }
                }
                else {
                    notifyBTString("BLUETOOTH_SCAN NOT GRANTED!");
                    showToast("BLUETOOTH_SCAN permissions needed for bleController.  Please use Android Settings application!");
                }
                return;
            case 238 :
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)  {
                    have_connect_permission = true;
                    notifyBTString("BLUETOOTH_CONNECT permission granted!!");
                }
                else {
                    notifyBTString("BLUETOOTH_CONNECT NOT GRANTED!");
                    showToast("BLUETOOTH_CONNECT permissions needed for bleController.  Please use Android Settings application!");
                }
                return;
        }
    }


    public static void showToast(final String msg)
    {
        self.runOnUiThread(new Runnable()
        {
            public void run()
            {
                Toast.makeText(self,msg,Toast.LENGTH_LONG).show();
            }
        });
    }


    public static void notifyBLEStateChanged()
    {
        self.runOnUiThread(new Runnable()
        {
            public void run()
            {
                TextView status = (TextView) self.findViewById(R.id.status_bt);
                String name = myBLE.device_name;
                if (myBLE.connected)
                {
                    status.setBackgroundColor(0xFF00AA00); // 0xAARRGGBB
                }
                else
                {
                    status.setBackgroundColor(my_purple); // 0xAARRGGBB
                    setDefaults();
                    name = "connect";
                }
                status.setText(name);
            }
        });

    }


    static void setDefaults()
    {
        ((TextView) self.findViewById(R.id.status)).setText("");
        ((Button)self.findViewById(R.id.button_A)).setText("A");
        ((Button)self.findViewById(R.id.button_B)).setText("B");
        ((Button)self.findViewById(R.id.button_C)).setText("C");
        ((Button)self.findViewById(R.id.button_D)).setText("D");
        ((Button)self.findViewById(R.id.button_E)).setText("E");
        ((Button)self.findViewById(R.id.button_F)).setText("F");
        ((Button)self.findViewById(R.id.button_G)).setText("G");
        ((Button)self.findViewById(R.id.button_H)).setText("H");
        ((Button)self.findViewById(R.id.button_I)).setText("I");
        ((Button)self.findViewById(R.id.button_J)).setText("J");
        ((Button)self.findViewById(R.id.button_K)).setText("K");
        ((Button)self.findViewById(R.id.button_L)).setText("L");
        ((Button)self.findViewById(R.id.button_M)).setText("M");
        ((Button)self.findViewById(R.id.button_N)).setText("N");
        ((Button)self.findViewById(R.id.button_O)).setText("O");
        JoystickView joy = (JoystickView) self.findViewById(R.id.joystick);
        joy.joy_spring = true;
        joy.joy_step = 4;
        joy.joy_deadzone = 20;
    }

    public static void notifyBTString(final String s)
    {
        if (s.startsWith("joy:"))
        {
            self.runOnUiThread(new Runnable() {
                public void run() {
                    String new_s = s.substring(4, s.length());
                    String parts[] = new_s.split(",");
                    boolean spring = parts.length > 0 && parts[0].equals("1") ? true : false;
                    int step = (parts.length > 1) ? Integer.parseInt(parts[1]) : 4;
                    int deadzone = (parts.length > 2) ? Integer.parseInt(parts[2]) : 20;
                    JoystickView joy = (JoystickView) self.findViewById(R.id.joystick);
                    joy.joy_spring = spring;
                    joy.joy_step = step;
                    joy.joy_deadzone = deadzone;
                }
            });
        }
        if (s.startsWith("button:"))
        {
            self.runOnUiThread(new Runnable() {
                public void run() {
                    String new_s = s.substring(7, s.length());
                    String parts[] = new_s.split(",");
                    int id = -1;
                    switch (parts[0])
                    {
                        case "A" : id = R.id.button_A; break;
                        case "B" : id = R.id.button_B; break;
                        case "C" : id = R.id.button_C; break;
                        case "D" : id = R.id.button_D; break;
                        case "E" : id = R.id.button_E; break;
                        case "F" : id = R.id.button_F; break;
                        case "G" : id = R.id.button_G; break;
                        case "H" : id = R.id.button_H; break;
                        case "I" : id = R.id.button_I; break;
                        case "J" : id = R.id.button_J; break;
                        case "K" : id = R.id.button_K; break;
                        case "L" : id = R.id.button_L; break;
                        case "M" : id = R.id.button_M; break;
                        case "N" : id = R.id.button_N; break;
                        case "O" : id = R.id.button_O; break;
                    }
                    if (id != -1)
                    {
                        Button b = (Button) self.findViewById(id);
                        String name = parts.length>1 ? parts[1] : "";
                        b.setText(name);
                    }
                }
            });
        }
        else if (s.startsWith("<") && s.endsWith(">") && s.length() > 2)
        {
            self.runOnUiThread(new Runnable()
            {
                public void run()
                {
                    String new_s = s.substring(1,s.length()-1);
                    ((TextView)self.findViewById(R.id.status)).setText(new_s);
                    //
                    // String parts[] = new_s.split(",");
                    // // Utils.log(0,0," parts.length=" + parts.length);
                    // if (parts.length >= 8)
                    // {
                    //     // status is parts[0]
                    //     ((TextView)self.findViewById(R.id.status_battery)).setText("" + ((float)Utils.parseInt(parts[1]))/10.0 + "V");
                    //     ((TextView)self.findViewById(R.id.status_angle)).setText(parts[2]);
                    //     ((TextView)self.findViewById(R.id.status_heading)).setText(parts[3]);
                    //     ((TextView)self.findViewById(R.id.status_speed)).setText(parts[4]);
                    //     ((TextView)self.findViewById(R.id.status_turn)).setText(parts[5]);
                    //     ((TextView)self.findViewById(R.id.status_pwm_left)).setText(parts[6]);
                    //     ((TextView)self.findViewById(R.id.status_pwm_right)).setText(parts[7]);
                    // }
                }
            });
        }
        else {
            appendStatusonUiThread(s);
        }
    }


    static void appendStatusonUiThread(final String s)
    {
        self.runOnUiThread(new Runnable()
        {
            public void run()
            {
                int MAX_MONITOR_BYTES = 8192;
                int len = mMonitorTextView.length();
                if (len >= MAX_MONITOR_BYTES)
                {
                    String copy = mMonitorTextView.getText().toString();
                    mMonitorTextView.setText(copy.substring(MAX_MONITOR_BYTES/2,len));
                }
                mMonitorTextView.append(s + "\n");
            }
        });
    }

    public void onButton(View view)
    {
        String send = "";
        switch (view.getId())
        {
            case R.id.button_A: send = "A"; break;
            case R.id.button_B: send = "B"; break;
            case R.id.button_C: send = "C"; break;
            case R.id.button_D: send = "D"; break;
            case R.id.button_E: send = "E"; break;
            case R.id.button_F: send = "F"; break;
            case R.id.button_G: send = "G"; break;
            case R.id.button_H: send = "H"; break;
            case R.id.button_I: send = "I"; break;
            case R.id.button_J: send = "J"; break;
            case R.id.button_K: send = "K"; break;
            case R.id.button_L: send = "L"; break;
            case R.id.button_M: send = "M"; break;
            case R.id.button_N: send = "N"; break;
            case R.id.button_O: send = "O"; break;
            }
        if (send.length()>0)
        {
            Utils.log(dbg_main,0,"Sending command '" + send + "'");
            myBLE.write(send);
        }
    }


    public void onTitleBarPressed(View view)
    {
        if (myBLE.connected) {
            myBLE.disconnect();
        }
        else if (!have_scan_permission || !have_connect_permission) {
            Utils.error("Missing Permissions!! Please give bleController needed permissions!");
            notifyBTString("MISSING PERMISSIONS!");
        }
        else {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Connect HM10");
            LayoutInflater inflater = getLayoutInflater();
            View the_view = (View) inflater.inflate(R.layout.device_list, null);
            builder.setView(the_view);

            ListView the_list = the_view.findViewById(R.id.device_list);
            DeviceListAdapter adapter = new DeviceListAdapter(this,new ArrayList<>());
            the_list.setAdapter((adapter));

            AlertDialog alert = builder.create();

            // Set an OnDismissListener for the AlertDialog
            alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    // Call myBLE.stopScan() when the dialog is dismissed
                    myBLE.stopScan();
                }
            });

            the_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    // Retrieve the selected ScanResult from the adapter
                    ScanResult selectedScanResult = adapter.getItem(position);

                    if (selectedScanResult != null) {
                        alert.dismiss();
                        myBLE.connectDevice(selectedScanResult);
                    }
                }
            });

            alert.show();
            myBLE.listDevices(adapter);
        }
    }



    @Override
    public void onJoystickMoved(int x, int y, int id)
    {
        // if (id == R.id.joystickRight)
        Utils.log(dbg_main+1,0,"Joystick x=" + x + "  y=" + y);
        String send = "" + y + "," + x ;
        myBLE.write(send);
    }

}   // class MainActivity