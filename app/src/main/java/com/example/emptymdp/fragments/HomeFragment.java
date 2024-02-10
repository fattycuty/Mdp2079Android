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
import androidx.viewpager2.widget.ViewPager2;

import android.view.ContextMenu;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.emptymdp.R;
import com.example.emptymdp.arena.PixelGridView;
import com.example.emptymdp.utilities.MessagePagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Objects;

public class HomeFragment extends Fragment {

    private final String TAG = "debugHomeFrag";
    static TextView tvRoboStatus, tvSelectedObject;
    Button btnClearMap, btnFastestPath, btnImageRecognition,
            btnClearRobotCar, btnClearObstacles, btnArenaInfo;
    ImageButton btnTl,btnTr,btnBr,btnBl,btnUp,btnDown;
    RelativeLayout rlMap;
    PixelGridView pixelGridView;
    ImageView ivCar, ivObstacle;
    ViewPager2 vpMessages;
    MessagePagerAdapter messagePagerAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // message fragment
        messagePagerAdapter = new MessagePagerAdapter(this);
        vpMessages = view.findViewById(R.id.vpMessages);
        vpMessages.setAdapter(messagePagerAdapter);
        TabLayout tabLayout = getView().findViewById(R.id.tlMessages);
        new TabLayoutMediator(tabLayout, vpMessages,
                new TabLayoutMediator.TabConfigurationStrategy() {
                    @Override
                    public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                        switch(position) {
                            case 0:
                                tab.setText("Normal Text");
                                break;
                            case 1:
                                tab.setText("Arena Updates");
                                break;
                        }
                    }
                }
        ).attach();

        // receive message from bluetooth fragment
        getParentFragmentManager().setFragmentResultListener("btFragtoHomeFrag", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                String who = bundle.getString("Who");
                bundle.remove("Who");

                assert who != null;
                getChildFragmentManager().setFragmentResult(who, bundle);
            }
        });

        // ===================== ui elements =====================
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
        tvSelectedObject = getView().findViewById(R.id.tvSelectedObject);
        btnClearObstacles = getView().findViewById(R.id.btnClearObstacles);
        btnClearRobotCar = getView().findViewById(R.id.btnClearRobotCar);
        btnArenaInfo = getView().findViewById(R.id.btnArenaInfo);

        // ===================== setup ui elements =====================

        // draggable objects
        ivCar.setTag("NEW_CAR");
        ivObstacle.setTag("NEW_OBSTACLE");

        // btn cmds
        manualMovement(btnUp, "f");
        manualMovement(btnDown,"r");
        manualMovement(btnTl,"tl");
        manualMovement(btnTr,"tr");
        manualMovement(btnBl,"sl");
        manualMovement(btnBr,"sr");
        buttonCmd(btnFastestPath,"beginFastest");
        buttonCmd(btnImageRecognition,"beginExplore");
        buttonCmd(btnArenaInfo,"sendArena");

        // grid map
        rlMap = getView().findViewById(R.id.rlMap);
        pixelGridView = new PixelGridView(getContext());
        registerForContextMenu(pixelGridView);
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
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, @Nullable ContextMenu.ContextMenuInfo menuInfo) {
        // to inflate object context menu
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = requireActivity().getMenuInflater();
        inflater.inflate(R.menu.obstacle_context_menu,menu);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        // obstacle context menu
        int itemId = item.getItemId();

        if (itemId == R.id.miSetNorth) {
            pixelGridView.setDirectionFromContextMenu(PixelGridView.Direction.NORTH);
            return true;
        } else if (itemId == R.id.miSetEast) {
            pixelGridView.setDirectionFromContextMenu(PixelGridView.Direction.EAST);
            return true;
        } else if (itemId == R.id.miSetSouth) {
            pixelGridView.setDirectionFromContextMenu(PixelGridView.Direction.SOUTH);
            return true;
        } else if (itemId == R.id.miSetWest) {
            pixelGridView.setDirectionFromContextMenu(PixelGridView.Direction.WEST);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    private void manualMovement(ImageButton ib, String direction){
        ib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(direction);
            }
        });
    }

    private void buttonCmd(Button button, String command){
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(command);
            }
        });
    }

    private void sendMessage(String msg){
        Bundle bundle = new Bundle();
        bundle.putString("SENT_MESSAGE", msg);
        getChildFragmentManager().setFragmentResult("homeFragToNormalTextFrag", bundle);
    }

    public static class MyDragShadowBuilder extends View.DragShadowBuilder {
        private static Drawable shadow;
        public MyDragShadowBuilder(View view, Drawable img) {
            super(view);

            shadow = Objects.requireNonNull(img.getConstantState()).newDrawable().mutate();
        }

        @Override
        public void onProvideShadowMetrics (Point size, Point touch) {
            int width, height;

            width = height = 128;

            shadow.setBounds(0, 0, width, height);

            size.set(width, height);

            touch.set(width / 2, height / 2);
        }
        @Override
        public void onDrawShadow(@NonNull Canvas canvas) {
            shadow.draw(canvas);
        }
    }

    private void dragNewObject(View v, ImageView iv){
        ClipData.Item item = new ClipData.Item((CharSequence) v.getTag());
        ClipData dragData = new ClipData((CharSequence) v.getTag(), new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
        View.DragShadowBuilder myShadow = new MyDragShadowBuilder(iv, iv.getDrawable());
        v.startDragAndDrop(dragData, myShadow, null,0);
    }

    public static void setTvSelectedObject(String selected){
        selected = "Selected: "+selected;
        tvSelectedObject.setText(selected);
    }

}