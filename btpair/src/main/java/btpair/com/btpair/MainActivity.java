package btpair.com.btpair;

import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends Activity implements NfcAdapter.CreateNdefMessageCallback {

    private static final String TAG = "MainActivity";

    BroadcastReceiver btPairReceiver;

    private NfcAdapter mNfcAdapter;
    boolean nfcDeviceReady = false;
    private BluetoothAdapter mBluetoothAdapter;
    private String macAddress = null;
    private boolean dispatchNfcAdapter = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initNFCandBT();
    }

    public void initNFCandBT() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            nfcDeviceReady = false;
            finish();
            return;
        } else {
            nfcDeviceReady = true;
        }

        mNfcAdapter.setNdefPushMessageCallback(this, this);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "This device doesn't support Bluetooth.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
            Log.d(TAG, "Enable bluetooth");
        }

        registerReceiver(mBTReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(mPairReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));

        Log.d(TAG, "Enable.isEnabled:" + mBluetoothAdapter.isEnabled());
        if (mBluetoothAdapter.isEnabled()) {
            btPairReceiver = new BTPairingRequest();
            registerReceiver(btPairReceiver, new IntentFilter("android.bluetooth.device.action.PAIRING_REQUEST"));
            macAddress = mBluetoothAdapter.getAddress();
            Log.d(TAG, "MAC Address:" + macAddress);
        }
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent nfcEvent) {
        String message = macAddress;
        Log.d(TAG, "macAddress:" + macAddress);
        NdefRecord ndefRecord = NdefRecord.createMime("text/plain", message.getBytes());
        NdefMessage ndefMessage = new NdefMessage(ndefRecord);
        return ndefMessage;
    }

    public void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};

        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);

        //start activity by filter
        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }

    public void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }

    @Override
    public void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();

        Log.d(TAG, "handleIntent action: " + action);

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMessages != null) {
                NdefMessage message = (NdefMessage) rawMessages[0]; // only one message transferred
                String mac = new String(message.getRecords()[0].getPayload());
                Log.d(TAG, "message: " + mac);

                BluetoothDevice remoteDevice = null;
                remoteDevice = mBluetoothAdapter.getRemoteDevice(mac);
                if (remoteDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "pairDevice");
                    PairUtility.pairDevice(remoteDevice);
                }

            } else {
                byte[] empty = new byte[]{};
                NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty);
                NdefMessage msg = new NdefMessage(new NdefRecord[]{record});
                rawMessages = new NdefMessage[]{msg};
                Log.d(TAG, "rawMessages:" + rawMessages);
            }
        }
    }

    private final BroadcastReceiver mBTReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON) {
                    Log.d(TAG, "BluetoothAdapter.STATE_ON");
                    macAddress = mBluetoothAdapter.getAddress();
                    Log.d(TAG, "MAC Address:" + macAddress);
                } else if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                    Log.d(TAG, "BluetoothAdapter.STATE_OFF");
                    mBluetoothAdapter.enable();
                }
            }
        }
    };

    private final BroadcastReceiver mPairReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    showToast("Paired");
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED) {
                    showToast("Unpaired");
                }
            }
        }
    };

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            if (nfcDeviceReady && !dispatchNfcAdapter) {
                setupForegroundDispatch(this, mNfcAdapter);
                dispatchNfcAdapter = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Dispatch NFC adapter error: " + e.toString());
        }
    }

    @Override
    public void onPause() {
        try {
            if (nfcDeviceReady && dispatchNfcAdapter) {
                stopForegroundDispatch(this, mNfcAdapter);
                dispatchNfcAdapter = false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Stop NFC adapter error: " + e.toString());
        }

        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(btPairReceiver);
            unregisterReceiver(mBTReceiver);
            unregisterReceiver(mPairReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.onDestroy();
    }
}
