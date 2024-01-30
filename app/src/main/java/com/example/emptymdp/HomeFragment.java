package com.example.emptymdp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.Tag;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private final String TAG = "homefrag";
    static TextView tvIncMsgs;
    static StringBuilder messages = new StringBuilder();
    Button btnSendMsg;
    EditText etSendMsg;
    BluetoothConnectionService btConnSvc;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getParentFragmentManager().setFragmentResultListener("homeFragKey", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                String result = bundle.getString("bundleKey");
                Toast.makeText(getContext(), result, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btConnSvc = BluetoothConnectionService.getInstance();

        // ===================== getting ui elements =====================
        btnSendMsg = getView().findViewById(R.id.btnSendMsg);
        etSendMsg = getView().findViewById(R.id.etSendMsg);
        tvIncMsgs = getView().findViewById(R.id.tvIncMsgs);

        // ===================== set on click listeners to buttons, list view =====================
        btnSendMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btConnSvc==null  || (btConnSvc.getState() != BluetoothConnectionService.STATE_CONNECTED)){
                    Toast.makeText(getContext(), "Device is not connected", Toast.LENGTH_SHORT).show();
                    return;
                }
                String msg = etSendMsg.getText().toString();
                if (msg.equals("")) return;
                btConnSvc.sendMessage(msg);
            }
        });
        tvIncMsgs.setText(messages);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public static void getMessage(String msg){
        messages.append(msg);
        tvIncMsgs.setText(messages);
    }



}