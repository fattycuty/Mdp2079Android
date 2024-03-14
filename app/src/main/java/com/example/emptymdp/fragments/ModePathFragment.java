package com.example.emptymdp.fragments;

import android.content.ClipData;
import android.content.ClipDescription;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.emptymdp.R;
import com.example.emptymdp.arena.PixelGridView;

import org.json.JSONObject;

public class ModePathFragment extends Fragment {
    private final String TAG = "debugPathFragment";
    Button btnClearMap,btnClearRobotCar, btnClearObstacles, btnImageRecognition, btnCalcPath;
    ImageView ivCar, ivObstacle;
    PixelGridView pixelGridView;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_mode_path, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ===================== ui elements =====================
        ivCar = getView().findViewById(R.id.ivCar);
        ivObstacle = getView().findViewById(R.id.ivObstacle);
        btnClearMap = getView().findViewById(R.id.btnClearMap);
        btnClearObstacles = getView().findViewById(R.id.btnClearObstacles);
        btnClearRobotCar = getView().findViewById(R.id.btnClearRobotCar);
        btnImageRecognition = getView().findViewById(R.id.btnImageRecognition);
        btnCalcPath = getView().findViewById(R.id.btnCalcPath);

        // ===================== setup ui elements =====================
        // draggable objects
        ivCar.setTag("NEW_CAR");
        ivObstacle.setTag("NEW_OBSTACLE");

        // ===================== grid map =====================
        pixelGridView = PixelGridView.getInstance();

        // ===================== listeners =====================
        btnClearMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pixelGridView.clearMap();
            }
        });

        btnClearRobotCar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pixelGridView.clearCar(true);
            }
        });

        btnClearObstacles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pixelGridView.removeAllObstacles();
            }
        });

        ivCar.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                dragNewObject(v,ivCar);
                return true;
            }
        });

        ivObstacle.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                dragNewObject(v,ivObstacle);
                return true;
            }
        });
        btnImageRecognition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("cat","control");
                    jsonObject.put("value","start");
                } catch (Exception e){
                    Log.e(TAG, "btnImageRecognition: ", e);
                }

                sendBundle(jsonObject.toString());
            }
        });

        btnCalcPath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("cat","obstacles_fin");
                    jsonObject.put("value","finish");
                } catch (Exception e){
                    Log.e(TAG, "btnCalcPath: ", e);
                }

                sendBundle(jsonObject.toString());
            }
        });
    }

    private void dragNewObject(View v, ImageView iv){
        ClipData.Item item = new ClipData.Item((CharSequence) v.getTag());
        ClipData dragData = new ClipData((CharSequence) v.getTag(), new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
        View.DragShadowBuilder myShadow = new HomeFragment.MyDragShadowBuilder(iv, iv.getDrawable());
        v.startDragAndDrop(dragData, myShadow, null,0);
    }

    private void sendBundle(String msg){
        Bundle bundle = new Bundle();
        bundle.putString("SENT_MESSAGE", msg);
        getParentFragmentManager().setFragmentResult("pathFragToHomeFrag", bundle);
    }
}
