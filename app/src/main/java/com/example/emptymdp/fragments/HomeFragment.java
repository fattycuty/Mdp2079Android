package com.example.emptymdp.fragments;

import android.content.ClipData;
import android.content.ClipDescription;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;

import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.emptymdp.R;
import com.example.emptymdp.arena.PixelGridView;
import com.example.emptymdp.bluetooth.BluetoothConnectionService;


public class HomeFragment extends Fragment {

    private final String TAG = "debugHomeFrag";
    static TextView tvIncMsgs, tvRoboStatus, tvSelectedObject;
    static StringBuilder messages = new StringBuilder();
    Button btnSendMsg, btnClearMap, btnFastestPath, btnImageRecognition, btnRotateSelected, btnClearSelected; //  btnRotateCar,
    EditText etSendMsg;
    BluetoothConnectionService btConnSvc;
    ImageButton btnTl,btnTr,btnBr,btnBl,btnUp,btnDown;
    RelativeLayout rlMap;
    PixelGridView pixelGridView;
    ImageView ivCar, ivObstacle;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getParentFragmentManager().setFragmentResultListener("homeFragKey", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                parseMessage(bundle);
            }
        });

        // ===================== ui elements =====================
        btnSendMsg = getView().findViewById(R.id.btnSendMsg);
        etSendMsg = getView().findViewById(R.id.etSendMsg);
        tvIncMsgs = getView().findViewById(R.id.tvIncMsgs);
        btnBl = getView().findViewById(R.id.btnBlArrow);
        btnDown = getView().findViewById(R.id.btnDownArrow);
        btnBr = getView().findViewById(R.id.btnBrArrow);
        btnUp = getView().findViewById(R.id.btnTopArrow);
        btnTl = getView().findViewById(R.id.btnTlArrow);
        btnTr = getView().findViewById(R.id.btnTrArrow);
        ivCar = getView().findViewById(R.id.ivCar);
        ivObstacle = getView().findViewById(R.id.ivObstacle);
        btnClearMap = getView().findViewById(R.id.btnClearMap);
        tvRoboStatus = getView().findViewById(R.id.tvRoboStatus);
        btnFastestPath = getView().findViewById(R.id.btnFastestPath);
        btnImageRecognition = getView().findViewById(R.id.btnImageRecognition);
        btnRotateSelected = getView().findViewById(R.id.btnRotateSelected);
        tvSelectedObject = getView().findViewById(R.id.tvSelectedObject);
        btnClearSelected = getView().findViewById(R.id.btnClearSelected);

        // ===================== setup ui elements =====================

        // draggable objects
        ivCar.setTag("NEW_CAR");
        ivObstacle.setTag("NEW_OBSTACLE");

        // chat box
        tvIncMsgs.setText(messages);

        // btn movements
        manualMovement(btnUp, "f");
        manualMovement(btnDown,"r");
        manualMovement(btnTl,"tl");
        manualMovement(btnTr,"tr");
        manualMovement(btnBl,"sl");
        manualMovement(btnBr,"sr");

        // grid map
        pixelGridView = new PixelGridView(getContext());
        rlMap = getView().findViewById(R.id.rlMap);
        pixelGridView.setNumColumns(20);
        pixelGridView.setNumRows(20);


        rlMap.addView(pixelGridView);
        pixelGridView.setSelectedObject(PixelGridView.CellValue.EMPTY);

        // ===================== listeners =====================
        btnClearMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pixelGridView.clearMap();
            }
        });

        btnSendMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = etSendMsg.getText().toString();
                if (msg.equals("")) return;
                sendMessage(msg);
            }
        });

        ivCar.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                dragNewObject(v,ivCar);
                return true;
            }
        });

        pixelGridView.setOnDragListener(new View.OnDragListener() {
            boolean outOfBounds = false;
            @Override
            public boolean onDrag(View v, DragEvent e) {
                switch(e.getAction()){
                    case DragEvent.ACTION_DRAG_STARTED:
                        // Determine whether this View can accept the dragged data.
                        if (e.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {

                            // As an example, apply a blue color tint to the View to
                            // indicate that it can accept data.
                            //((ImageView)v).setColorFilter(Color.BLUE);

                            // Invalidate the view to force a redraw in the new tint.
                            //v.invalidate();

                            // Return true to indicate that the View can accept the dragged
                            // data.
                            outOfBounds = false;
                            return true;

                        }

                        // Return false to indicate that, during the current drag and drop
                        // operation, this View doesn't receive events again until
                        // ACTION_DRAG_ENDED is sent.
                        return false;

                    case DragEvent.ACTION_DRAG_ENTERED:
                        // Return true. The value is ignored.
                        //Log.d(TAG, "onDrag: ACTION_DRAG_ENTERED");
                        outOfBounds = false;
                        return false;

                    case DragEvent.ACTION_DRAG_LOCATION:
                        // Ignore the event.
                        //Log.d(TAG, "onDrag: ACTION_DRAG_LOCATION");
                        outOfBounds = false;
                        return true;

                    case DragEvent.ACTION_DRAG_EXITED:
                        // Return true. The value is ignored.
                        //Log.d(TAG, "onDrag: ACTION_DRAG_EXITED");
                        outOfBounds = true;
                        return false;

                    case DragEvent.ACTION_DROP:
                        // Return true. DragEvent.getResult() returns true.
                        //Log.d(TAG, "onDrag: ACTION_DROP");
                        pixelGridView.receiveOnDrag(e,e.getX(),e.getY());
                        return true;

                    case DragEvent.ACTION_DRAG_ENDED:
                        // Return true. The value is ignored.
                        //Log.d(TAG, "onDrag: ACTION_DRAG_ENDED");

                        if (outOfBounds){
                            pixelGridView.receiveOnDrag(e,-1,-1);

                        }
                        return true;

                    // An unknown action type is received.
                    default:
                        //Log.d(TAG,"Unknown action type received by View.OnDragListener. ivCar");
                        break;
                }
                return false;
            }
        });

        ivObstacle.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                dragNewObject(v,ivObstacle);
                return true;
            }
        });

        btnRotateSelected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rotateSelected();
            }
        });

        btnClearSelected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pixelGridView.clearSelectedValues();
            }
        });

    }
    public void manualMovement(ImageButton ib, String direction){
        ib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(direction);
            }
        });
    }

    public void sendMessage(String msg){
        if (btConnSvc == null) btConnSvc = BluetoothConnectionService.getInstance();
        if (btConnSvc.getState() != BluetoothConnectionService.STATE_CONNECTED){
            Toast.makeText(getContext(), "Device is not connected", Toast.LENGTH_SHORT).show();
            appendFailedMessage(msg);
            return;
        }
        btConnSvc.sendMessage(msg);
    }

    public static class MyDragShadowBuilder extends View.DragShadowBuilder {
        private static Drawable shadow;
        public MyDragShadowBuilder(View view, Drawable img) {
            super(view);

            shadow = img.getConstantState().newDrawable().mutate();
        }

        @Override
        public void onProvideShadowMetrics (Point size, Point touch) {
            int width, height;

//            width = getView().getWidth() / 2;
//            height = getView().getHeight() / 2;

            width = height = 128;

            shadow.setBounds(0, 0, width, height);

            size.set(width, height);

            touch.set(width / 2, height / 2);
        }
        @Override
        public void onDrawShadow(Canvas canvas) {
            shadow.draw(canvas);
        }
    }

    private void dragNewObject(View v, ImageView iv){
        ClipData.Item item = new ClipData.Item((CharSequence) v.getTag());
        ClipData dragData = new ClipData((CharSequence) v.getTag(), new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
        View.DragShadowBuilder myShadow = new MyDragShadowBuilder(iv, iv.getDrawable());
        v.startDragAndDrop(dragData, myShadow, null,0);
    }

    private void parseMessage(Bundle bundle){
        String messageType, message;

        String bundledMessage = bundle.getString("Message");
        String deviceName = bundle.getString("Device Name");

        if (bundledMessage.contains(":")){
            messageType = bundledMessage.split(":")[0];
            message = bundledMessage.split(":")[1];
        } else {
            messageType = "NORMAL_RECEIVED_TEXT";
            message = bundledMessage;
        }

        switch (messageType){
            case "SENT_TEXT":
                message = "Me: "+message+"\n";
                messages.append(message);
                tvIncMsgs.setText(messages);
                //pixelGridView.moveCarWithCommand(message);
                break;
            case "status":
                message = "Status: "+message;
                tvRoboStatus.setText(message);
                break;
            case "NORMAL_RECEIVED_TEXT":
                // received normal text
                message = deviceName+": "+message+"\n";
                messages.append(message);
                tvIncMsgs.setText(messages);
                break;
        }

    }

    public static void setTvSelectedObject(String selected){
        selected = "Selected: "+selected;
        tvSelectedObject.setText(selected);
    }

    private void rotateSelected(){
        int selectedObject = pixelGridView.getSelectedObject();

        switch (selectedObject){
            case PixelGridView.CellValue.EMPTY:
                Toast.makeText(getContext(), "Nothing is selected", Toast.LENGTH_SHORT).show();
                break;
            case PixelGridView.CellValue.CAR:
                Log.d(TAG, "rotateSelected: rotating car");
                pixelGridView.rotateCar();
            case PixelGridView.CellValue.OBSTACLE:
                Log.d(TAG, "rotateSelected: rotating obstacle");
                pixelGridView.rotateObstacle(pixelGridView.getSelectedObstacle());
        }
    }

    private void appendFailedMessage(String message){
        message = "Me: "+message+" (unsent)\n";
        messages.append(message);
        tvIncMsgs.setText(messages);
    }

}