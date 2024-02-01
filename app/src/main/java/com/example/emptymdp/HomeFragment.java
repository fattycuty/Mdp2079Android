package com.example.emptymdp;

import static android.view.DragEvent.ACTION_DROP;

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


public class HomeFragment extends Fragment {

    private final String TAG = "debugHomeFrag";
    static TextView tvIncMsgs;
    static StringBuilder messages = new StringBuilder();
    Button btnSendMsg, btnClearMap, btnRotateCar;
    EditText etSendMsg;
    BluetoothConnectionService btConnSvc;
    ImageButton btnTl,btnTr,btnBr,btnBl,btnUp,btnDown;
    RelativeLayout rlMap;
    PixelGridView pixelGridView;
    ImageView ivCar, ivObstacle;
    TextView tvPlaceStatus;

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
        tvPlaceStatus = getView().findViewById(R.id.tvPlaceStatus);
        btnClearMap = getView().findViewById(R.id.btnClearMap);
        btnRotateCar = getView().findViewById(R.id.btnRotateCar);

        // ===================== setup ui elements =====================

        // draggable objects
        ivCar.setTag("CAR");
        ivObstacle.setTag("OBSTACLE");

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
        rlMap.addView(pixelGridView);  pixelGridView.setSelectedItem(PixelGridView.CellValue.EMPTY);

        // ===================== listeners =====================
        btnRotateCar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pixelGridView.rotateCar();
            }
        });
        btnClearMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pixelGridView.clearMap();
            }
        });
        btnSendMsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btConnSvc==null  || (btConnSvc.getState() != BluetoothConnectionService.STATE_CONNECTED)){
                    Toast.makeText(getContext(), "Device is not connected", Toast.LENGTH_SHORT).show();
                    return;
                }
                String msg = etSendMsg.getText().toString();
                if (msg.equals("")) return;
                sendMessage(msg);
            }
        });

        ivCar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCellStatus(PixelGridView.CellValue.CAR);
            }
        });

        ivCar.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                setCellStatus(PixelGridView.CellValue.CAR);
                ClipData.Item item = new ClipData.Item((CharSequence) v.getTag());
                ClipData dragData = new ClipData((CharSequence) v.getTag(), new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
                View.DragShadowBuilder myShadow = new MyDragShadowBuilder(ivCar, ivCar.getDrawable());
                v.startDragAndDrop(dragData, myShadow, null,0);
                return true;
            }
        });

        ivCar.setOnDragListener(new View.OnDragListener() {
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
                            return true;

                        }

                        // Return false to indicate that, during the current drag and drop
                        // operation, this View doesn't receive events again until
                        // ACTION_DRAG_ENDED is sent.
                        return false;

                    case DragEvent.ACTION_DRAG_ENTERED:

                        // Apply a green tint to the View.
                        //((ImageView)v).setColorFilter(Color.GREEN);

                        // Invalidate the view to force a redraw in the new tint.
                        //v.invalidate();

                        // Return true. The value is ignored.
                        return true;

                    case DragEvent.ACTION_DRAG_LOCATION:

                        // Ignore the event.
                        return true;

                    case DragEvent.ACTION_DRAG_EXITED:

                        // Reset the color tint to blue.
                        //((ImageView)v).setColorFilter(Color.BLUE);

                        // Invalidate the view to force a redraw in the new tint.
                        //v.invalidate();

                        // Return true. The value is ignored.
                        return true;

                    case ACTION_DROP:

                        // Get the item containing the dragged data.
                        //ClipData.Item item = e.getClipData().getItemAt(0);

                        // Get the text data from the item.
                        //CharSequence dragData = item.getText();

                        // Display a message containing the dragged data.
                        //Toast.makeText(getContext(), "Dragged data is " + dragData, Toast.LENGTH_SHORT).show();

                        // Turn off color tints.
                        //((ImageView)v).clearColorFilter();

                        // Invalidate the view to force a redraw.
                        //v.invalidate();

                        // Return true. DragEvent.getResult() returns true.
                        return false;

                    case DragEvent.ACTION_DRAG_ENDED:

                        // Turn off color tinting.
                        //((ImageView)v).clearColorFilter();

                        // Invalidate the view to force a redraw.
                        //v.invalidate();

                        // Do a getResult() and displays what happens.
                        if (e.getResult()) {
                            Toast.makeText(getContext(), "The drop was handled.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "The drop didn't work.", Toast.LENGTH_SHORT).show();
                        }

                        // Return true. The value is ignored.
                        return true;

                    // An unknown action type is received.
                    default:
                        Log.d(TAG,"Unknown action type received by View.OnDragListener. ivCar");
                        break;
                }
                return false;
            }
        });

        pixelGridView.setOnDragListener(new View.OnDragListener() {
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
                            return true;

                        }

                        // Return false to indicate that, during the current drag and drop
                        // operation, this View doesn't receive events again until
                        // ACTION_DRAG_ENDED is sent.
                        return false;

                    case DragEvent.ACTION_DRAG_ENTERED:

                        // Return true. The value is ignored.
                        return false;

                    case DragEvent.ACTION_DRAG_LOCATION:
                        // Ignore the event.
                        return true;

                    case DragEvent.ACTION_DRAG_EXITED:
                        // Return true. The value is ignored.
                        return true;

                    case ACTION_DROP:
                        // Return true. DragEvent.getResult() returns true.
                        //log(e.getX()+" "+e.getY());
                        pixelGridView.receiveOnDrag(e.getClipData(),e.getX(),e.getY());
                        return true;

                    case DragEvent.ACTION_DRAG_ENDED:
                        // Return true. The value is ignored.
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
                setCellStatus(PixelGridView.CellValue.OBSTACLE);
                ClipData.Item item = new ClipData.Item((CharSequence) v.getTag());
                ClipData dragData = new ClipData((CharSequence) v.getTag(), new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
                View.DragShadowBuilder myShadow = new MyDragShadowBuilder(ivObstacle, ivObstacle.getDrawable());
                v.startDragAndDrop(dragData, myShadow, null,0);
                return true;
            }
        });

        ivObstacle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCellStatus(PixelGridView.CellValue.OBSTACLE);
            }
        });

        tvPlaceStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCellStatus(PixelGridView.CellValue.EMPTY);
            }
        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public static void getMessage(String msg){
        messages.append(msg);
        tvIncMsgs.setText(messages);
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
        if (btConnSvc==null  || (btConnSvc.getState() != BluetoothConnectionService.STATE_CONNECTED)){
            Toast.makeText(getContext(), "Device is not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        btConnSvc.sendMessage(msg);
    }

    private static class MyDragShadowBuilder extends View.DragShadowBuilder {

        // The drag shadow image, defined as a drawable object.
        private static Drawable shadow;

        // Constructor.
        public MyDragShadowBuilder(View view, Drawable img) {

            // Store the View parameter.
            super(view);

            // Create a draggable image that fills the Canvas provided by the
            // system.
            //shadow = new ColorDrawable(Color.LTGRAY);
            shadow = img.getConstantState().newDrawable().mutate();
        }

        // Define a callback that sends the drag shadow dimensions and touch point
        // back to the system.
        @Override
        public void onProvideShadowMetrics (Point size, Point touch) {

            // Define local variables.
            int width, height;

            // Set the width of the shadow to half the width of the original
            // View.
            width = getView().getWidth() / 2;

            // Set the height of the shadow to half the height of the original
            // View.
            height = getView().getHeight() / 2;

            // The drag shadow is a ColorDrawable. Set its dimensions to
            // be the same as the Canvas that the system provides. As a result,
            // the drag shadow fills the Canvas.
            shadow.setBounds(0, 0, width, height);

            // Set the size parameter's width and height values. These get back
            // to the system through the size parameter.
            size.set(width, height);

            // Set the touch point's position to be in the middle of the drag
            // shadow.
            touch.set(width / 2, height / 2);
        }

        // Define a callback that draws the drag shadow in a Canvas that the system
        // constructs from the dimensions passed to onProvideShadowMetrics().
        @Override
        public void onDrawShadow(Canvas canvas) {

            // Draw the ColorDrawable on the Canvas passed in from the system.
            shadow.draw(canvas);
        }
    }

    private void log (String logMessage){
        Log.d(TAG,logMessage);
    }

    private void setCellStatus(int cellValue){
        switch (cellValue){
            case PixelGridView.CellValue.EMPTY:
                tvPlaceStatus.setText("Placing: None");
                pixelGridView.setSelectedItem(PixelGridView.CellValue.EMPTY);
                break;
            case PixelGridView.CellValue.CAR:
                tvPlaceStatus.setText("Placing: Car");
                pixelGridView.setSelectedItem(PixelGridView.CellValue.CAR);
                break;
            case PixelGridView.CellValue.OBSTACLE:
                tvPlaceStatus.setText("Placing: Obstacle");
                pixelGridView.setSelectedItem(PixelGridView.CellValue.OBSTACLE);
                break;
        }
    }
}