package com.example.emptymdp.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.emptymdp.R;
import com.example.emptymdp.arena.PixelGridView;

import org.json.JSONObject;

public class ModeManualFragment extends Fragment {
    private final String TAG = "debugManualFrag";
    Button btnFastestPath, btnManualSnap;
    Button btnTl,btnTr,btnUp,btnDown;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_mode_manual, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // ===================== ui elements =====================
        btnDown = getView().findViewById(R.id.btnDownArrow);
        btnUp = getView().findViewById(R.id.btnTopArrow);
        btnTl = getView().findViewById(R.id.btnTlArrow);
        btnTr = getView().findViewById(R.id.btnTrArrow);
        btnFastestPath = getView().findViewById(R.id.btnFastestPath);
        btnManualSnap = getView().findViewById(R.id.btnManualSnap);

        // ===================== setup ui elements =====================
        // btn cmds
        manualMovement(btnUp, "FW--");
        manualMovement(btnDown,"BW--");
        manualMovement(btnTl,"TL--");
        manualMovement(btnTr,"TR--");
        manualCmd(btnManualSnap,"MANSNAP");

        btnFastestPath.setOnClickListener( v-> {
            int arenaLocation  = PixelGridView.getInstance().getArenaLocation();
            String arenaLocationStr;
            if (arenaLocation == PixelGridView.ArenaLocation.INDOOR){
                 arenaLocationStr = "WN01";
            } else {
                arenaLocationStr = "WN02";
            }

            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("cat","manual");
                jsonObject.put("value",arenaLocationStr);
            } catch (Exception e){
                Log.e(TAG, "manualMovement: ", e);
            }

            sendBundle(jsonObject.toString());
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void manualMovement(Button btn, String direction){
        btn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //if (btnSetMode.getText().equals("Mode: Path")) return true;
                JSONObject jsonObject = new JSONObject();
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // send movement command
                        // hold down button
                        try {
                            jsonObject.put("cat","manual");
                            jsonObject.put("value",direction);
                            sendBundle(jsonObject.toString());
                        } catch (Exception e){
                            Log.e(TAG, "imageButton: "+direction);
                            Log.e(TAG, "imageButton: ", e);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        // send stop command
                        try {
                            jsonObject.put("cat","manual");
                            jsonObject.put("value","STOP");
                            sendBundle(jsonObject.toString());
                        } catch (Exception e){
                            Log.e(TAG, "imageButton: STOP");
                            Log.e(TAG, "imageButton: ", e);
                        }
                        break;
                }
                return false;
            }
        });
    }

    private void manualCmd(Button button, String command){
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("cat","manual");
                    jsonObject.put("value",command);
                } catch (Exception e){
                    Log.e(TAG, "manualMovement: ", e);
                }

                sendBundle(jsonObject.toString());
            }
        });
    }

    private void sendBundle(String msg){
        Bundle bundle = new Bundle();
        bundle.putString("SENT_MESSAGE", msg);
        getParentFragmentManager().setFragmentResult("manualFragToHomeFrag", bundle);
    }
}
