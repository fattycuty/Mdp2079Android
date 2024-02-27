package com.example.emptymdp.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;

import com.example.emptymdp.R;
import com.example.emptymdp.bluetooth.BluetoothConnectionService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

public class NormalTextFragment extends Fragment {
    private final String TAG = "debugNormalTextFrag";
    static StringBuilder normalTextString = new StringBuilder();
    Button btnSendMsg;
    EditText etSendMsg;
    static TextView tvIncNormalText;
    BluetoothConnectionService btConnSvc;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_normal_text, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // messages from other fragments
        getParentFragmentManager().setFragmentResultListener("btFragToNormalTextFrag", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                Log.d(TAG, "onFragmentResult: "+requestKey);
                String message = bundle.getString("Message");
                assert message != null;
//                message = message.split(":")[1];
//                message = "Me: "+message+"\n";
                normalTextString.append(message);
                tvIncNormalText.setText(normalTextString);
            }
        });

        getParentFragmentManager().setFragmentResultListener("homeFragToNormalTextFrag", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                Log.d(TAG, "onFragmentResult: "+requestKey);
                String msg = bundle.getString("SENT_MESSAGE");
                sendMsg(msg);
            }
        });

        btnSendMsg = view.findViewById(R.id.btnSendMsg);
        etSendMsg = view.findViewById(R.id.etSendMsg);
        tvIncNormalText = view.findViewById(R.id.tvIncNormalText);

        btnSendMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = etSendMsg.getText().toString();
                if (msg.equals("")) return;
                sendMsg(msg);
            }
        });

        final ScrollView scrollview = ((ScrollView) view.findViewById(R.id.svNormalText));
        scrollview.post(new Runnable() {
            @Override
            public void run() {
                scrollview.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    private void sendMsg(String msg){
        if (btConnSvc == null) btConnSvc = BluetoothConnectionService.getInstance();
        if (btConnSvc.getState() != BluetoothConnectionService.STATE_CONNECTED){
            Toast.makeText(getContext(), "Device is not connected", Toast.LENGTH_SHORT).show();
            appendFailedMessage(msg);
            return;
        }

        btConnSvc.sendMessage(msg);
    }

    private static void appendFailedMessage(String message){
        message = "Me: "+message+" (unsent)\n";
        normalTextString.append(message);
        tvIncNormalText.setText(normalTextString);
    }

    public static void updateTextString(String message){
        normalTextString.append(message);
        tvIncNormalText.setText(normalTextString);
    }
}
