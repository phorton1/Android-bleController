package e.p.bleController;

// This DeviceListAdapter class exists to hold BLE ScanResults
// and return the user defined name and MAC address as a textView
// for the list in the dialog box.

import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class DeviceListAdapter  extends ArrayAdapter<ScanResult> {

        public DeviceListAdapter(Context context, List<ScanResult> scanResults) {
            super(context, 0, scanResults);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            ScanResult scanResult = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            }

            TextView textView = convertView.findViewById(android.R.id.text1);
            if (scanResult != null) {
                // Display the user-defined name and MAC address in the list
                textView.setText(scanResult.getScanRecord().getDeviceName() + " - " + scanResult.getDevice().getAddress());
            }

            return convertView;
        }
    }




