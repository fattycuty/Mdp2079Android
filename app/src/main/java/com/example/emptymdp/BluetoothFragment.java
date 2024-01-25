package com.example.emptymdp;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.nfc.Tag;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class BluetoothFragment extends Fragment {

    private static final String TAG = "btlog";
    private final static int REQUEST_ENABLE_BT = 1;
    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothConnectionService btConnSvc;
    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice btDevice;
    ListView lvPaired, lvAvail;
    Button btnStartScan, btnBtOnOff, btnStopScan, btnMakeDiscoverable;
    ArrayList<BluetoothDevice> availDeviceList, pairedDeviceList;
    DeviceListAdapter availDeviceAdapter, pairedDeviceAdapter;
    static TextView tvBtStatus, tvConnectedTo;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_bluetooth, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // get bt adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // ===================== getting ui elements =====================
        lvAvail = getView().findViewById(R.id.lvAvailDevices);
        lvPaired = getView().findViewById(R.id.lvPairedDevices);
        btnStartScan = getView().findViewById(R.id.btnBtStartScan);
        btnBtOnOff = getView().findViewById(R.id.btnBtOnOff);
        btnStopScan = getView().findViewById(R.id.btnBtStopScan);
        btnMakeDiscoverable = getView().findViewById(R.id.btnBtDiscoverable);
        tvBtStatus = getView().findViewById(R.id.tvBtStatus);
        tvConnectedTo = getView().findViewById(R.id.tvConnectedTo);

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
                confirmConnection(device);
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
                confirmConnection(device);
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

    }

    private void enableDisableBt() {
        if (bluetoothAdapter == null) {
            Log.d(TAG, "enableDisableBt: Does not have bt capabilities");
        }
        //Toast.makeText(getContext(),"enabledisablebt pressed",Toast.LENGTH_SHORT).show();
        if (!bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enableDisableBt: enabling bt");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            //arlBtOnOff.launch(enableBtIntent);
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

    private void confirmConnection(BluetoothDevice device) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setPositiveButton("Connect", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User taps OK button.
                bondDevice(device);
                startConnection();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancels the dialog.
            }
        });

        builder.setTitle("Connect to " + device.getName() + "?");

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

        builder.setTitle("Remove connection?");

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void bondDevice(BluetoothDevice device) {
        if (!BluetoothPermissions.checkBluetoothConnectionPermission(getContext())) {
            BluetoothPermissions.requestBluetoothPermissions(getActivity());
        }
        bluetoothAdapter.cancelDiscovery();
        device.createBond();
        btDevice = device;
        btConnSvc = new BluetoothConnectionService(getContext());
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
    }

    private void startBtConnection(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startBtConnection: Init RFCOM Bt Conn");

        btConnSvc.startClient(device,uuid);
    }

    private void startConnection(){
        startBtConnection(btDevice, MY_UUID_INSECURE);
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
                    btDevice = device;
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
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG,"bt state turning on");
                        break;
                }
            }
        }
    };

    ActivityResultLauncher<Intent> arlBtOnOff = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult o) {
                    if (o.getResultCode() == Activity.RESULT_OK){
                        Intent data = o.getData();
                    }
                }
            }
    );

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().unregisterReceiver(btStateReceiver);
        getActivity().unregisterReceiver(discoveryReceiver);
        getActivity().unregisterReceiver(bondReceiver);
    }

    public static TextView getTvBtStatus(){
        return tvBtStatus;
    }

    public static TextView getTvConnectedTo(){
        return tvConnectedTo;
    }




}