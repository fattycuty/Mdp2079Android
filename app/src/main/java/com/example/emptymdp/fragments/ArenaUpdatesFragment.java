package com.example.emptymdp.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

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

public class ArenaUpdatesFragment extends Fragment {
    private final String TAG = "debugArenaUpdatesFrag";
    static StringBuilder arenaUpdatesString = new StringBuilder();
    static TextView tvIncArenaUpdates;
    ScrollView svArenaUpdates;
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
        svArenaUpdates = view.findViewById(R.id.svArenaUpdates);
    }

    private void parseMessage(Bundle bundle){
        String messageType, message;
        int direction;
        JSONObject jsonObject = new JSONObject();

        String bundledMessage = bundle.getString("Message");
        String deviceName = bundle.getString("Device Name");
        //Log.d(TAG, "parseMessage: "+bundledMessage);

        if (isJSONValid(bundledMessage)){
            try {
                assert bundledMessage != null;
                jsonObject = new JSONObject(bundledMessage);
                messageType = jsonObject.get("cat").toString();
                message = bundledMessage;
            } catch (JSONException e){
                //Log.e(TAG, "parseMessage: ", e);
                messageType = "NORMAL_RECEIVED_TEXT";
                message = bundledMessage;
            }

        } else {
            messageType = "NORMAL_RECEIVED_TEXT";
            message = bundledMessage;
        }

        int col,row;
        switch (messageType){
            case "SENT_TEXT":
                setArenaUpdatesMessage("Me",message);
                break;

            case "status":
                Log.d(TAG, "parseMessage: status "+jsonObject);
                try {
                    message = "Status: "+jsonObject.get("value");
                    HomeFragment.tvRoboStatus.setText(message);
                } catch (JSONException e){
                    Log.e(TAG, "parseMessage: status ", e);
                }
                break;

            case "image-rec":
                Log.d(TAG, "parseMessage: imagerec "+jsonObject);
                try {
                    JSONObject value = new JSONObject(jsonObject.get("value").toString());

                    pixelGridView.receiveTargetInfo(Integer.parseInt(value.get("obstacle_id").toString()),value.get("image_id").toString());

                    setArenaUpdatesMessage(deviceName,message);
                } catch (JSONException e){
                    Log.e(TAG, "parseMessage: imagerec ", e);
                }
                break;

            case "location":
                Log.d(TAG, "parseMessage: location "+jsonObject);
                try {
                    JSONObject value = new JSONObject(jsonObject.get("value").toString());

                    int[] rowCol = PixelGridView.convertRowCol(
                            Integer.parseInt(value.get("x").toString()),
                            Integer.parseInt(value.get("y").toString()));

                    row = rowCol[0];
                    col = rowCol[1];

                    direction = Integer.parseInt(value.get("d").toString());

                    pixelGridView.receiveRobotCoords(row,col,direction);

                    setArenaUpdatesMessage(deviceName,message);
                } catch (JSONException e){
                    Log.e(TAG, "parseMessage: location ", e);
                }
                break;

            case "error":
                Log.d(TAG, "parseMessage: error "+jsonObject);
                try {
                    Toast.makeText(getContext(), jsonObject.get("value").toString(), Toast.LENGTH_SHORT).show();
                } catch (JSONException e){
                    Log.e(TAG, "parseMessage: display error toast ", e);
                }
                break;

            case "info":
                Log.d(TAG, "parseMessage: info "+jsonObject);
                try {
                    message = deviceName+": "+jsonObject.get("value")+"\n";
                    NormalTextFragment.updateTextString(message);
                } catch (JSONException e){
                    Log.e(TAG, "parseMessage: info ", e);
                }
                break;

//            case "ROBOTPOSITION":
//                message = message.replaceAll("\\s+","");
//                col = Integer.parseInt(message.split(",")[0]);
//                row = Integer.parseInt(message.split(",")[1]);
//
//                direction = Integer.parseInt(message.split(",")[2]);
//
//                pixelGridView.receiveRobotCoords(row,col,direction);
//
//                message = deviceName+": ROBOTPOSITION("+message+")\n";
//                arenaUpdatesString.append(message);
//                tvIncArenaUpdates.setText(arenaUpdatesString);
//                break;
//
//            case "ADDOBSTACLE":
//                message = message.replaceAll("\\s+","");
//                col = Integer.parseInt(message.split(",")[0]);
//                row = Integer.parseInt(message.split(",")[1]);
//
//                // handle add
//                pixelGridView.receiveObstacleCoords(row,col,"ADD");
//
//                message = deviceName+": ADDOBSTACLE("+message+")\n";
//                arenaUpdatesString.append(message);
//                tvIncArenaUpdates.setText(arenaUpdatesString);
//                break;
//
//            case "REMOVEOBSTACLE":
//                message = message.replaceAll("\\s+","");
//                col = Integer.parseInt(message.split(",")[0]);
//                row = Integer.parseInt(message.split(",")[1]);
//
//                // handle remove
//                pixelGridView.receiveObstacleCoords(row,col,"REMOVE");
//
//                message = deviceName+": REMOVEOBSTACLE("+message+")\n";
//                arenaUpdatesString.append(message);
//                tvIncArenaUpdates.setText(arenaUpdatesString);
//                break;
//
//            case "TARGET":
//                int obstacleId = Integer.parseInt(message.split(",")[0]);
//                String targetId = message.split(",")[1];
//
//                pixelGridView.receiveTargetInfo(obstacleId,targetId);
//
//                message = deviceName+": TARGET("+message+")\n";
//                arenaUpdatesString.append(message);
//                tvIncArenaUpdates.setText(arenaUpdatesString);
//                break;
//
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

    private void setArenaUpdatesMessage(String deviceName, String message){
        message = deviceName+":"+message+"\n";
        arenaUpdatesString.append(message);
        tvIncArenaUpdates.setText(arenaUpdatesString);
        svArenaUpdates.fullScroll(View.FOCUS_DOWN);
    }
}
