package btpair.com.btpair;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

/**
 * Created by Jerry_Wu on 2016/7/26.
 */
public class PairUtility {
    private static String TAG = "PairUtility";

    public static void unpairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void unpairAllDevice(BluetoothAdapter mBluetoothAdapter) {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        ArrayList<BluetoothDevice> list = new ArrayList<BluetoothDevice>();
        list.addAll(pairedDevices);
        if (list.size() != 0) {
            for (int deviceNum = 0; deviceNum < list.size(); deviceNum++) {
                unpairDevice(list.get(deviceNum));
                Log.d(TAG, "unPair:" + list.get(deviceNum));
            }
        }
    }

    public static void pairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
            Log.d(TAG, "pairDevice OK");
        } catch (Exception e) {
            Log.d(TAG, "pairDeviceException");
            e.printStackTrace();
        }
    }
}
