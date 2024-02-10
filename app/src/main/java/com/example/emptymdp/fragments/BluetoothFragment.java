package com.example.emptymdp.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.emptymdp.utilities.DeviceListAdapter;
import com.example.emptymdp.R;
import com.example.emptymdp.bluetooth.BluetoothConnectionService;
import com.example.emptymdp.bluetooth.BluetoothPermissions;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class BluetoothFragment extends Fragment {
    // final strings
    private static final String TAG = "debugBtFrag";
    private final static int REQUEST_ENABLE_BT = 1;
    //private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final UUID MY_UUID_SECURE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static BluetoothConnectionService btConnSvc;
    BluetoothAdapter bluetoothAdapter;
    // layout views
    ListView lvPaired, lvAvail;
    Button btnStartScan, btnBtOnOff, btnStopScan, btnMakeDiscoverable;
    static ArrayList<BluetoothDevice> availDeviceList, pairedDeviceList;
    DeviceListAdapter availDeviceAdapter, pairedDeviceAdapter;
    TextView tvBtStatus;
    private String mConnectedDeviceName;
    ProgressDialog mProgressDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // get bt adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btConnSvc==null)
            btConnSvc = new BluetoothConnectionService(getContext(),mHandler);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_bluetooth, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ===================== getting ui elements =====================
        lvAvail = getActivity().findViewById(R.id.lvAvailDevices);
        lvPaired = getActivity().findViewById(R.id.lvPairedDevices);
        btnStartScan = getActivity().findViewById(R.id.btnBtStartScan);
        btnBtOnOff = getActivity().findViewById(R.id.btnBtOnOff);
        btnStopScan = getActivity().findViewById(R.id.btnBtStopScan);
        btnMakeDiscoverable = getActivity().findViewById(R.id.btnBtDiscoverable);
        tvBtStatus = getActivity().findViewById(R.id.tvBtStatus);
        

        // ===================== set on click listeners =====================
        btnStartScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                discoverDevices();
            }
        });

        btnStopScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelDisocvery();
            }
        });

        btnBtOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableDisableBt();
            }
        });

        btnMakeDiscoverable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableDiscoverability();
            }
        });

        availDeviceList = new ArrayList<>();
        pairedDeviceList = new ArrayList<>();

        lvAvail.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long i) {
                if (!BluetoothPermissions.checkBluetoothConnectionPermission(getContext())) {
                    BluetoothPermissions.requestBluetoothPermissions(getActivity());
                }

                BluetoothDevice device = (BluetoothDevice) parent.getItemAtPosition(position);
                Log.d(TAG, "Avail Device Clicked: " + device.getName() + " " + device.getAddress());
                confirmPair(device);
            }
        });

        lvPaired.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!BluetoothPermissions.checkBluetoothConnectionPermission(getContext())) {
                    BluetoothPermissions.requestBluetoothPermissions(getActivity());
                }

                BluetoothDevice device = (BluetoothDevice) parent.getItemAtPosition(position);
                Log.d(TAG, "Paired Device Clicked: " + device.getName() + " " + device.getAddress());
                startConnection(device);
            }
        });

        lvPaired.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice device = (BluetoothDevice) parent.getItemAtPosition(position);
                confirmRemoveConnection(device);
                return true;
            }
        });

        // display paired devices
        if (bluetoothAdapter.isEnabled()) getPairedDevices();

        // ===================== Register Receivers =====================

        // discover devices
        IntentFilter filterDiscover = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        getActivity().registerReceiver(discoveryReceiver, filterDiscover);


        // get result of bt change
        IntentFilter btIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        getActivity().registerReceiver(btStateReceiver, btIntent);

        // bond state changes
        IntentFilter bondFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        getActivity().registerReceiver(bondReceiver, bondFilter);

        // ===================== others =====================

        if (bluetoothAdapter!=null && bluetoothAdapter.isEnabled()){
            if (mConnectedDeviceName==null)
                btConnSvc.start();
            else
                setStatus("Connected to "+ mConnectedDeviceName, Color.GREEN);
        }
    }

    private void enableDisableBt() {
        if (bluetoothAdapter == null) {
            Log.d(TAG, "enableDisableBt: Does not have bt capabilities");
        }

        if (!bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enableDisableBt: enabling bt");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            getPairedDevices();
        }

        if (bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enableDisableBt: disabling bt");
            if (!BluetoothPermissions.checkBluetoothConnectionPermission(getContext())) {
                BluetoothPermissions.requestBluetoothPermissions(getActivity());
            }
            Intent intent = new Intent("android.bluetooth.adapter.action.REQUEST_DISABLE");
            startActivityForResult(intent, 1);

        }
    }

    private void getPairedDevices() {
        if (!isBtTurnedOn()) return;

        if (!BluetoothPermissions.checkBluetoothConnectionPermission(getContext())) {
            BluetoothPermissions.requestBluetoothPermissions(getActivity());
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                pairedDeviceList.add(device);
                pairedDeviceAdapter = new DeviceListAdapter(getContext(), R.layout.custom_list_view, pairedDeviceList);
                lvPaired.setAdapter(pairedDeviceAdapter);
            }
        }
    }

    private void discoverDevices() {
        if (!isBtTurnedOn()) return;

        Log.d(TAG, "discovering devices");
        availDeviceList.clear();

        if (!BluetoothPermissions.checkBluetoothConnectionPermission(getContext())) {
            BluetoothPermissions.requestBluetoothPermissions(getActivity());
        }

        bluetoothAdapter.cancelDiscovery();

        bluetoothAdapter.startDiscovery();
    }

    private void cancelDisocvery() {
        if (!isBtTurnedOn()) return;

        Log.d(TAG, "cancelling discovery");

        if (!BluetoothPermissions.checkBluetoothConnectionPermission(getContext())) {
            BluetoothPermissions.requestBluetoothPermissions(getActivity());
        }

        bluetoothAdapter.cancelDiscovery();
    }

    private void enableDiscoverability() {
        if (!isBtTurnedOn()) return;

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
        btConnSvc.start();
    }

    private boolean isBtTurnedOn() {
        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                Toast.makeText(getContext(), "Bluetooth is turned off", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private void confirmPair(BluetoothDevice device) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User taps OK button.
                bondDevice(device);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancels the dialog.
            }
        });

        builder.setTitle("Pair " + device.getName() + "?");

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void confirmRemoveConnection(BluetoothDevice device){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User taps OK button.
                removeBond(device);
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancels the dialog.
            }
        });

        builder.setTitle("Remove Pair?");

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void bondDevice(BluetoothDevice device) {
        if (!BluetoothPermissions.checkBluetoothConnectionPermission(getContext())) {
            BluetoothPermissions.requestBluetoothPermissions(getActivity());
        }
        bluetoothAdapter.cancelDiscovery();
        device.createBond();
    }

    private void removeBond(BluetoothDevice device){
        if (!BluetoothPermissions.checkBluetoothConnectionPermission(getContext())) {
            BluetoothPermissions.requestBluetoothPermissions(getActivity());
        }
        bluetoothAdapter.cancelDiscovery();
        try {
            device.getClass().getMethod("removeBond", (Class[]) null).invoke(device, (Object[]) null);
        } catch (Exception e){
            Log.e(TAG,e.getMessage());
        }
        pairedDeviceAdapter.remove(device);
        lvPaired.setAdapter(pairedDeviceAdapter);
    }

    private void startConnection(BluetoothDevice device){
        if (!BluetoothPermissions.checkBluetoothConnectionPermission(getContext())) {
            BluetoothPermissions.requestBluetoothPermissions(getActivity());
        }
        bluetoothAdapter.cancelDiscovery();

        if (btConnSvc!=null)
            btConnSvc.connect(device);


    }

    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"Discover: ACTION FOUND");
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (!BluetoothPermissions.checkBluetoothConnectionPermission(getContext())) {
                    BluetoothPermissions.requestBluetoothPermissions(getActivity());
                }
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "Found " + device.getName() + " " + device.getAddress());
                    availDeviceList.add(device);
                    availDeviceAdapter = new DeviceListAdapter(getContext(),R.layout.custom_list_view, availDeviceList);
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    lvAvail.setAdapter(availDeviceAdapter);
                }
            }
        }

    };

    private final BroadcastReceiver bondReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Bonding with device");
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                if (!BluetoothPermissions.checkBluetoothConnectionPermission(getContext())) {
                    BluetoothPermissions.requestBluetoothPermissions(getActivity());
                }
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // bonded already
                if (device.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG,"BondReceiver: BOND_BONDED with "+device.getName());

                    pairedDeviceAdapter.add(device);
                    lvPaired.setAdapter(pairedDeviceAdapter);

                    //setStatus("Connected to "+device.getName(),Color.GREEN);

                }
                // creating a bond
                if (device.getBondState() == BluetoothDevice.BOND_BONDING){
                    Log.d(TAG,"BondReceiver: BOND_BONDING with "+device.getName());
                }
                // breaking a bond
                if (device.getBondState() == BluetoothDevice.BOND_NONE){
                    Log.d(TAG,"BondReceiver: BOND_NONE with "+device.getName());
                }
            }

        }
    };

    private final BroadcastReceiver btStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"Receiving bt state");
            String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG,"bt state off");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG,"bt state turning off");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG,"bt state on");
                        getPairedDevices();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG,"bt state turning on");
                        break;
                }
            }
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().unregisterReceiver(btStateReceiver);
        getActivity().unregisterReceiver(discoveryReceiver);
        getActivity().unregisterReceiver(bondReceiver);
    }

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case BluetoothConnectionService.Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothConnectionService.STATE_CONNECTED:
                            setStatus("Connected to "+ mConnectedDeviceName, Color.GREEN);
                            break;
                        case BluetoothConnectionService.STATE_CONNECTING:
                            mConnectedDeviceName = null;
                            setStatus("Connecting...", Color.YELLOW);
                            break;
                        case BluetoothConnectionService.STATE_LISTEN:
                            //setStatus("Listening...", Color.YELLOW);
                            break;
                        case BluetoothConnectionService.STATE_NONE:
                            mConnectedDeviceName = null;
                            setStatus("Disconnected",Color.RED);
                            break;
                    }
                    break;
                case BluetoothConnectionService.Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    //HomeFragment.getMessage("Me:  " + writeMessage+"\n");
                    writeMessage = "SENT_TEXT:"+writeMessage;
                    sendToHomeFrag("Me",writeMessage,"btFragToNormalTextFrag");
                    break;
                case BluetoothConnectionService.Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    //Log.d(TAG, "handleMessage: "+readMessage);
                    // handle message differently once we agree on the format
                    //HomeFragment.getMessage(mConnectedDeviceName + ":  " + readMessage+"\n");
                    sendToHomeFrag(mConnectedDeviceName,readMessage,"btFragToArenaUpdatesFrag");
                    break;
                case BluetoothConnectionService.Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(BluetoothConnectionService.Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast toast = Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT);
                        toast.show();
                    }
                    break;
                case BluetoothConnectionService.Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        String toastMsg = msg.getData().getString(BluetoothConnectionService.Constants.TOAST);
                        Toast toast = Toast.makeText(activity, toastMsg,
                                Toast.LENGTH_SHORT);
                        toast.show();

                    }
                    break;
            }
        }
    };

    private void setStatus(String status, int color){
        tvBtStatus.setText(status);
        tvBtStatus.setTextColor(color);
    }

    private void sendToHomeFrag(String deviceName, String msg, String who){
        Bundle bundle = new Bundle();
        bundle.putString("Who",who);
        bundle.putString("Message", msg);
        bundle.putString("Device Name",deviceName);

        getParentFragmentManager().setFragmentResult("btFragtoHomeFrag", bundle);
    }

//    private void sendToNormalTextFrag(String msg){
//        Bundle bundle = new Bundle();
//        bundle.putString("SENT_MESSAGE", msg);
//        getParentFragmentManager().setFragmentResult("normalTextFragKey", bundle);
//        Log.d(TAG, "sendToNormalTextFrag: ");
//    }
//
//    private void sendToArenaUpdatesFrag(String deviceName, String msg){
//        Bundle bundle = new Bundle();
//        bundle.putString("Message", msg);
//        bundle.putString("Device Name",deviceName);
//        getParentFragmentManager().setFragmentResult("arenaUpdatesFragKey", bundle);
//        Log.d(TAG, "sendToArenaUpdatesFrag: ");
//    }
}