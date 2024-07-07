package e.p.bleController;

/*
    Rewritten July 2024 to work with HM-10 BLE module

    (1) MainActivity creates a dialog with a list adapter and calls listDevices()
    (2) listDevices() starts a scan which calls back to leScanCallback::onScanResult()
    (3) onScanResult() maintains found_devicees by MAC and adds new HM-10's
        that it finds (with a non-null device name and the HM10_UUID) to
        the list adapter.
    (4) The scan will stop after 6 seconds, when a device is selected for
        connection, or the dialog is dismissed
    (5) If a device is selected by the user, MainActivity will call connectDevice()
        with a ScanResult that is placed in the_result and connectGatt is
        called which results in callbacks to gattCallback::onConnectionStateChange().
        connectDevice() will also stop the scan.
    (6) onConnectionStateChange(CONNECTED) calls gatt.discoverServices(), which
        results in calls to onServicesDiscovered() which then looks for the
        magic_characteristic service by UUID
    (7) if the magic characteristic service is found, "connected" is set
        to true, everyone is notified, and the device is presumed to
        be working and connected.

    Any failures should result in Utils.error() calls which show toasts
    as well as debug console error messages and/or showing them in the
    window.
 */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.os.ParcelUuid;
import android.util.Log;
import android.os.Handler;
import android.widget.ArrayAdapter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;




public class myBLE {

    public static int dbg_ble = 0;

    public static boolean connected = false;
    public static String device_name = "";

    private static final long MAX_SCAN_PERIOD = 6000;
    private static final String HM10_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
    private static final String HM10_MAGIC_CHARACTERISTIC = "0000ffe1-0000-1000-8000-00805f9b34fb";

    private static BluetoothAdapter bluetoothAdapter = null;
    private static BluetoothLeScanner bluetoothLeScanner = null;
    private static Handler handler = new Handler();
    private static boolean scanning = false;
    private static Hashtable<String, ScanResult> found_devices = new Hashtable<>();
    private static ArrayAdapter<ScanResult>  the_adapter;

    private static ScanResult the_result = null;
    private static BluetoothGatt the_gatt = null;
    private static BluetoothGattCharacteristic magic_characteristic = null;
    private static String hm10_string;

    // SEND BYTES TO ARDUINO

    public static boolean write(String send)
        // sending a sting amounts to setting the characteristic
        // and is limited to 20 bytes, though we bracket them with
        // <> for the Arduino app to disambiguate them, resulting
        // in a maximum of 18 bytes ....
    {
        if (send.length() > 18)
        {
            Utils.error("myBLE.write(): too many bytes(" + send + ")");
            return false;
        }
        if (magic_characteristic == null) {
            Utils.warning(0, 0, "mBLE.write(" + send + ") without magic_charactistic");
            return false;
        }

        send = "<" + send + ">";
        magic_characteristic.setValue(send);
        boolean success = the_gatt.writeCharacteristic(magic_characteristic);
        return success;
    }


    private static BluetoothGattCallback gattCallback = new BluetoothGattCallback()
    {
        // RECEIVE BYTES FROM ARDUINO

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
        // on the other hand, when receiving text from the Arduino, we build them up until
        // we find cr-lf in the part at which point we forward them to the MainActivity,
        // so there is no 18-20 byte limit in that direction ...
        {
            Log.d("blec", "onCharacteristicChanged() = " + characteristic.getStringValue(0));
            boolean done = false;
            String part = characteristic.getStringValue(0);
            if (part.contains("\n"))
            {
                done = true;
            }
            hm10_string += part;
            if (done)
            {
                hm10_string = hm10_string.replace("\r\n","");
                MainActivity.notifyBTString(hm10_string);
                hm10_string = "";
            }
        }

        // THE REST OF THIS IS CONNECTION LOGIC ...

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Utils.log(dbg_ble,0,"onConnectionStateChange Status: " + status + " newState=" + newState);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Utils.log(dbg_ble,0,"STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Utils.log(dbg_ble,0,"STATE_DISCONNECTED");
                    gatt.close();
                    // the_gatt = null;
                    break;
                default:
                    Utils.log(dbg_ble,0,"STATE_OTHER");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            for (int i=0; i<services.size(); i++)
            {
                BluetoothGattService svc = services.get(i);
                Utils.log(dbg_ble,0,"Service[" + i + "]-" + svc.getUuid().toString());
                List<BluetoothGattCharacteristic> atts = svc.getCharacteristics();
                for (int j=0; j<atts.size(); j++) {
                    BluetoothGattCharacteristic att = atts.get(j);
                    Utils.log(dbg_ble,1,"Characteristic(" + j + ")  " + att.getWriteType() + ":" + att.getUuid().toString());
                    if (att.getUuid().equals(UUID.fromString(HM10_MAGIC_CHARACTERISTIC))) {
                        Utils.log(dbg_ble,2,"found magic_characteristic at svc[" + i + "] att[" + j + "]");
                        magic_characteristic = att; // Found the desired characteristic
                    }
                }
            }

            if (magic_characteristic == null)
            {
                Utils.error("Could not find magic_characteristic!");
                init();
                return;
            }

            boolean rslt  = gatt.readCharacteristic(magic_characteristic);
            Log.d("blec","gatt.readCharacteristic returned " + rslt);

            if (gatt.setCharacteristicNotification(magic_characteristic, true) == true)
            {
                Log.d("blec","gatt.setCharacteristicNotification SUCCESS!");
                ScanRecord rec = the_result.getScanRecord();
                device_name = rec.getDeviceName();
                MainActivity.notifyBTString("connected to " + device_name);
                MainActivity.showToast("Connected to " + device_name);
                connected = true;
                MainActivity.notifyBLEStateChanged();
            }
            else
            {
                Log.d("blec","gatt.setCharacteristicNotification FAILURE!");
                Utils.error("Could not setCharacteristicNotification for " + the_result.getScanRecord().getDeviceName());
                init();
            }
        }
    };  // gattCallback


    public static void connectDevice(ScanResult result)
    {
        stopScan();
        MainActivity.notifyBTString("connecting to " + result.getScanRecord().getDeviceName());
        BluetoothDevice device = result.getDevice();
        the_gatt = device.connectGatt(
            MainActivity.self,
            true,
            gattCallback);
        if (the_gatt == null) {
            Utils.error("Could not connectGatt to " + the_result.getScanRecord().getDeviceName());
            init();
        }
        else
        {
            the_result = result;
        }
    }


    private static ScanCallback leScanCallback =
        new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {

                // result.mDevice = "4C:24:98:CF:77:F5"
                // result.mScanRecord.mServiceUuids =
                //      [ScanRecord [
                //          mAdvertiseFlags=6,
                //          mServiceUuids=[0000ffe0-0000-1000-8000-00805f9b34fb],
                //          mServiceSolicitationUuids=[],
                //          mManufacturerSpecificData={19784=[76, 36, -104, -49, 119, -11]},
                //          mServiceData={0000b000-0000-1000-8000-00805f9b34fb=[0, 0, 0, 0]},
                //          mTxPowerLevel=0,
                //          mDeviceName=HMSoft,
                //          mTransportBlocks=[]]
                //
                // I unpack these one level at a time and check for nulls for safety

                ScanRecord rec = result.getScanRecord();
                List<ParcelUuid> uuids = rec == null ? null : rec.getServiceUuids();
                ParcelUuid uuid0 = uuids == null ? null : uuids.get(0);
                String uuid_str = uuid0 == null ? null : uuid0.toString();
                String name = rec.getDeviceName();  // result.getDevice().getName();

                // Note that ScanRecord.getDeviceName() is the name the user assigns
                // to the HM-10, whereas result.getDevice().getName() always returns "HMSoft"

                if (name != null && uuid_str.equals(HM10_UUID))
                {
                    BluetoothDevice device = result.getDevice();
                    String mac = device.getAddress();
                    Utils.log(dbg_ble, 0, "found(" + name + ") at mac(" + mac + ")", 1);

                    // there is probably a way to use the_adapter to prevent
                    // duplicates, but the found_devices list pre-dates the
                    // dialog box and so remains in place ...

                    if (!found_devices.containsKey(mac)) {
                        the_adapter.add(result);
                        found_devices.put(mac, result);
                        MainActivity.notifyBTString("found " + name + ": " + mac);
                    }
                }
            }
        };



    public static void stopScan()
    {
        if (scanning)
        {
            scanning = false;
            MainActivity.notifyBTString("scan stopped");
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    }


    public static void listDevices(ArrayAdapter<ScanResult> adapter)
    {
        the_adapter = adapter;

        MainActivity.notifyBTString("preparing ...");

        init();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();


        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (scanning) {
                    scanning = false;
                    MainActivity.notifyBTString("scan finished");
                    bluetoothLeScanner.stopScan(leScanCallback);
                }
            }
        }, MAX_SCAN_PERIOD);

        MainActivity.notifyBTString("scanning ...");
        scanning = true;
        bluetoothLeScanner.startScan(leScanCallback);
    }


    public static void disconnect()
    {
        MainActivity.notifyBTString("disconnecting ...");
        if (the_gatt != null)
            the_gatt.disconnect();
        init();
        MainActivity.notifyBLEStateChanged();
    }

    private static void init()
    {
        the_gatt = null;
        the_result = null;
        magic_characteristic = null;
        found_devices.clear();
        hm10_string = "";
        device_name = "";
        connected = false;
    }

}   // class myBLE
