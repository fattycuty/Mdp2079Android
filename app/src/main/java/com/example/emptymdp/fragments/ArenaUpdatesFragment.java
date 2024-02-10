package com.example.emptymdp.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;

import com.example.emptymdp.R;
import com.example.emptymdp.arena.PixelGridView;
import com.example.emptymdp.bluetooth.BluetoothConnectionService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

public class ArenaUpdatesFragment extends Fragment {
    private final String TAG = "debugArenaUpdatesFrag";
    static StringBuilder arenaUpdatesString = new StringBuilder();
    static TextView tvIncArenaUpdates;
    BluetoothConnectionService btConnSvc;
    private PixelGridView pixelGridView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_arena_updates, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pixelGridView = PixelGridView.getInstance();
        
        // receive message from home fragment
        getParentFragmentManager().setFragmentResultListener("btFragToArenaUpdatesFrag", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                Log.d(TAG, "onFragmentResult: arena updates frag");
                parseMessage(bundle);
            }
        });

        tvIncArenaUpdates = view.findViewById(R.id.tvIncArenaUpdates);

        final ScrollView scrollview = ((ScrollView) view.findViewById(R.id.svArenaUpdates));
        scrollview.post(new Runnable() {
            @Override
            public void run() {
                scrollview.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    private void parseMessage(Bundle bundle){
        String messageType, message;
        ArrayList<String> messageArray;

        String bundledMessage = bundle.getString("Message");
        String deviceName = bundle.getString("Device Name");
        Log.d(TAG, "parseMessage: "+bundledMessage);

        if (isJSONValid(bundledMessage)){
            // for status
            try {
                messageArray = jsonObjToArray(bundledMessage);
                messageType = messageArray.get(0);
                message = messageArray.get(1);
                Log.d(TAG, "parseMessage: "+message);
            } catch (JSONException e){
                //Log.e(TAG, "parseMessage: ", e);
                messageType = "NORMAL_RECEIVED_TEXT";
                message = bundledMessage;
            }

        } else {
            try {
                assert bundledMessage != null;
                messageType = bundledMessage.split(":")[0];
                message = bundledMessage.split(":")[1];
            } catch (Exception e){
                //Log.e(TAG, "parseMessage: ", e);
                messageType = "NORMAL_RECEIVED_TEXT";
                message = bundledMessage;
            }
        }

        int col,row;
        switch (messageType){
            case "SENT_TEXT":
                message = "Me: "+message+"\n";
                arenaUpdatesString.append(message);
                tvIncArenaUpdates.setText(arenaUpdatesString);
                pixelGridView.moveCarWithCommand(message);
                break;

            case "status":
                message = "Status: "+message;
                HomeFragment.tvRoboStatus.setText(message);
                break;

            case "ROBOTPOSITION":
                message = message.replaceAll("\\s+","");
                col = Integer.parseInt(message.split(",")[0]);
                row = Integer.parseInt(message.split(",")[1]);

                int direction = Integer.parseInt(message.split(",")[2]);

                pixelGridView.receiveRobotCoords(row,col,direction);

                message = deviceName+": ROBOTPOSITION("+message+")\n";
                arenaUpdatesString.append(message);
                tvIncArenaUpdates.setText(arenaUpdatesString);
                break;

            case "ADDOBSTACLE":
                message = message.replaceAll("\\s+","");
                col = Integer.parseInt(message.split(",")[0]);
                row = Integer.parseInt(message.split(",")[1]);

                // handle add
                pixelGridView.receiveObstacleCoords(row,col,"ADD");

                message = deviceName+": ADDOBSTACLE("+message+")\n";
                arenaUpdatesString.append(message);
                tvIncArenaUpdates.setText(arenaUpdatesString);
                break;

            case "REMOVEOBSTACLE":
                message = message.replaceAll("\\s+","");
                col = Integer.parseInt(message.split(",")[0]);
                row = Integer.parseInt(message.split(",")[1]);

                // handle remove
                pixelGridView.receiveObstacleCoords(row,col,"REMOVE");

                message = deviceName+": REMOVEOBSTACLE("+message+")\n";
                arenaUpdatesString.append(message);
                tvIncArenaUpdates.setText(arenaUpdatesString);
                break;

            case "TARGET":
                int obstacleId = Integer.parseInt(message.split(",")[0]);
                String targetId = message.split(",")[1];

                pixelGridView.receiveTargetInfo(obstacleId,targetId);

                message = deviceName+": TARGET("+message+")\n";
                arenaUpdatesString.append(message);
                tvIncArenaUpdates.setText(arenaUpdatesString);
                break;

            case "NORMAL_RECEIVED_TEXT":
                // received normal text
                message = deviceName+": "+message+"\n";
                NormalTextFragment.updateTextString(message);

                break;
        }

    }

    private boolean isJSONValid(String string) {
        try {
            new JSONObject(string);
        } catch (JSONException ex) {
            try {
                new JSONArray(string);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }

    private ArrayList<String> jsonObjToArray(String json) throws JSONException {
        // only works with a single key in the json
        JSONObject jsonObj = new JSONObject(json);
        ArrayList<String> messageArray = new ArrayList<>();

        Iterator<String> keys= jsonObj.keys();
        String keyValue, valueString;
        while (keys.hasNext())
        {
            keyValue = keys.next();
            valueString = jsonObj.getString(keyValue);
            messageArray.add(keyValue);
            messageArray.add(valueString);
        }

        return messageArray;
    }
}
