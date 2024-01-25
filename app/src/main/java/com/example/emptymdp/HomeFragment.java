package com.example.emptymdp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.nio.charset.Charset;

public class HomeFragment extends Fragment {

    private final String TAG = "homefrag";
    TextView tvIncMsgs;
    StringBuilder messages;
    Button btnSendMsg;
    EditText etSendMsg;
    BluetoothConnectionService btConnSvc;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        btConnSvc = new BluetoothConnectionService(getContext());

        // ===================== getting ui elements =====================
        btnSendMsg = getView().findViewById(R.id.btnSendMsg);
        etSendMsg = getView().findViewById(R.id.etSendMsg);
        tvIncMsgs = getView().findViewById(R.id.tvIncMsgs);
        messages = new StringBuilder();

        // ===================== set on click listeners to buttons, list view =====================
        btnSendMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] bytes = etSendMsg.getText().toString().getBytes(Charset.defaultCharset());
                btConnSvc.write(bytes);
                etSendMsg.setText("");
            }
        });

        // ===================== Register Receivers =====================
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(msgReceiver, new IntentFilter("incomingMessage"));
    }

    BroadcastReceiver msgReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"Check");
            String text = intent.getStringExtra("theMessage");
            messages.append(text+"\n");
            tvIncMsgs.setText(messages);
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(msgReceiver);
    }
}