package com.example.emptymdp.arena;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.example.emptymdp.R;
import com.example.emptymdp.bluetooth.BluetoothConnectionService;
import com.example.emptymdp.fragments.HomeFragment;
import com.example.emptymdp.fragments.NormalTextFragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class PixelGridView extends View {
    private final String TAG = "debugMap";
    private int numColumns, numRows; //  0-indexed:  int row = (int)(touchX / cellWidth); int col = (int)(touchY / cellHeight);
    private int cellWidth, cellHeight;
    private final Paint pObstacle = new Paint();
    private final Paint pObstacleDirection = new Paint();
    private final Paint pAlphaNumSmall = new Paint();
    private final Paint pAlphaNumBig = new Paint();
    private final Paint testPaint = new Paint();
    private final Paint pBorder = new Paint();
    private final Paint pSelectedBorder = new Paint();
    private final Paint pAxisPaintLight = new Paint();
    private final Paint pAxisPaintDark = new Paint();
    private int selectedObject;
    private Obstacle selectedObstacle;
    private ArrayDeque<Integer> draggedOutQueue;
    private int placementCellValue = CellValue.EMPTY;
    Bitmap bmCar;
    private ArenaProperty arena;
    private int arenaLocation = ArenaLocation.INDOOR;
    private int numOfObstacles = 0;
    private BluetoothConnectionService btConnSvc;
    GestureDetector gestureDetector;
//    MenuItemClickListener menuItemClickListener;
//    OnCreateContextMenuListener onCreateContextMenuListener;
    public static PixelGridView pixelGridView;

    public interface CellValue {
        int EMPTY = 0;
        int OBSTACLE = 1;
        int CAR = 2;
        int MOVE_OBSTACLE = 3;
        int MOVE_CAR = 4;
    }

    public interface Direction {
        int NORTH = 0;
        int EAST = 2;
        int SOUTH = 4;
        int WEST = 6;
    }

    public interface ArenaLocation {
        int INDOOR = 0;
        int OUTDOOR = 1;
    }

    public PixelGridView(Context context) {
        this(context, null);
    }
    
    public PixelGridView(Context context, AttributeSet attrs) {
        super(context, attrs);

        pixelGridView = this;

        // touch events
        gestureDetector = new GestureDetector(context, new GestureListener());
//        onCreateContextMenuListener = new CreateContextMenuListener();
//        menuItemClickListener = new MenuItemClickListener();

        // paints
        testPaint.setColor(Color.BLACK);
        pBorder.setColor(Color.BLACK);
        pAlphaNumSmall.setColor(Color.MAGENTA);
        pAlphaNumSmall.setTextAlign(Paint.Align.CENTER);
        pAlphaNumBig.setColor(Color.MAGENTA);
        pAlphaNumBig.setTextSize(36F);
        pAlphaNumBig.setTextAlign(Paint.Align.CENTER);
        pAlphaNumBig.setFakeBoldText(true);
        pObstacle.setColor(Color.BLACK);
        pObstacleDirection.setColor(Color.YELLOW);
        pSelectedBorder.setColor(Color.GREEN);
        pSelectedBorder.setStrokeWidth(4.0F);
        pAxisPaintLight.setColor(Color.WHITE);
        pAxisPaintLight.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        pAxisPaintDark.setColor(Color.BLUE);
        pAxisPaintDark.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        // car image
        bmCar = BitmapFactory.decodeResource(getResources(), R.drawable.ic_car_up);

        // create arena
        arena = new ArenaProperty();

        // create queue
        draggedOutQueue = new ArrayDeque<>();
    }

    public static PixelGridView getInstance(){
        return pixelGridView;
    }

    public void setNumColumns(int numColumns) {
        this.numColumns = numColumns;
        calculateDimensions();
    }

    public int getNumColumns() {
        return numColumns;
    }

    public void setNumRows(int numRows) {
        this.numRows = numRows;
        calculateDimensions();
    }

    public int getNumRows() {
        return numRows;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calculateDimensions();
    }

    private void calculateDimensions() {
        if (numColumns < 1 || numRows < 1) {
            return;
        }

        arena.setArena(numRows,numColumns);

        cellWidth = getWidth() / numColumns;
        cellHeight = getHeight() / numRows;

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.WHITE);

        boolean placedCar = false;

        if (numColumns == 0 || numRows == 0) {
            return;
        }

        int[][] matrixBoard = arena.getMatrixBoard();

        // draw obstacles and car
        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numColumns; col++) {

                if (matrixBoard[row][col] == CellValue.OBSTACLE) {
                    drawObstacle(canvas,row,col);

                } else if (matrixBoard[row][col] == CellValue.CAR) {
                    int[] middleRowCol = arena.getRobotCar().getRowColMap().get(RobotCar.RobotMapArea.MIDDLE_MIDDLE);
                    assert middleRowCol != null;
                    if (middleRowCol[0] == row && middleRowCol[1] == col)
                        drawCar(canvas,row,col);
                }

            }
        }

        int width = getWidth();
        int height = getHeight();

        // draw axis
        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numColumns; col++) {
                int[] newRowCol = convertRowCol(row, col);
                //canvas.drawText("r"+rowCol[0]+",c"+rowCol[1],(2 * col + 1)*cellWidth/2,(2 * row + 1)*cellHeight/2,testPaint);
                Obstacle obstacle = arena.getObstacle(row,col,-1);
                if (newRowCol[0] == 0) {
                    if (obstacle==null)
                        canvas.drawText(Integer.toString(newRowCol[1]), col * cellWidth, (row + 1) * cellHeight, pAxisPaintDark);
                    else
                        canvas.drawText(Integer.toString(newRowCol[1]), col * cellWidth, (row + 1) * cellHeight, pAxisPaintLight);
                } else if (newRowCol[1] == 0) {
                    if (obstacle==null)
                        canvas.drawText(Integer.toString(newRowCol[0]), col * cellWidth, (row + 1) * cellHeight, pAxisPaintDark);
                    else
                        canvas.drawText(Integer.toString(newRowCol[0]), col * cellWidth, (row + 1) * cellHeight, pAxisPaintLight);
                }
            }
        }

        // draw borders
        for (int i = 0; i <= numColumns; i++) {
            canvas.drawLine(i * cellWidth, 0, i * cellWidth, height, pBorder);
        }
        for (int i = 0; i <= numRows; i++) {
            canvas.drawLine(0, i * cellHeight, width, i * cellHeight, pBorder);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) throws ArrayIndexOutOfBoundsException{
        return gestureDetector.onTouchEvent(event);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private int col, row, curObj;
        @Override
        public void onLongPress(@NonNull MotionEvent e) throws ArrayIndexOutOfBoundsException {
            super.onLongPress(e);
            if (curObj != CellValue.EMPTY)
                dragCellObject(row, col, curObj);

        }

        @Override
        public boolean onDown(MotionEvent e) throws ArrayIndexOutOfBoundsException {
            //Log.d(TAG, "onDown: ");
            col = (int)(e.getX() / cellWidth);
            row = (int)(e.getY() / cellHeight);

            if (row>=numRows || col>=numColumns) return false;

            curObj = arena.getMatrixBoard()[row][col];

            if (curObj == CellValue.OBSTACLE){
                //Log.d(TAG, "onDown: selected obstacle");
                setSelectedObject(CellValue.OBSTACLE, arena.getObstacle(row,col,-1));

            } else if (curObj == CellValue.CAR){
                //Log.d(TAG, "onDown: selected car");
                setSelectedObject(CellValue.CAR,null);

            } else if (curObj == CellValue.EMPTY){
                //Log.d(TAG, "onDown: selected empty cell");
                if (placementCellValue == CellValue.EMPTY)
                    setSelectedObject(CellValue.EMPTY,null);
                else {
                    int type;
                    switch (placementCellValue){
                        case CellValue.CAR:
                            type = CellValue.CAR;
                            newCell(type,e.getX(),e.getY());
                            break;
                        case CellValue.OBSTACLE:
                            type = CellValue.OBSTACLE;
                            newCell(type,e.getX(),e.getY());
                            break;
                    }
                }
            }

            return true;
        }

        @Override
        public boolean onDoubleTap(@NonNull MotionEvent e) throws ArrayIndexOutOfBoundsException{
            Log.d(TAG, "onDoubleTap: "+curObj);

            if (curObj!=CellValue.EMPTY)
                showContextMenu(e.getX(), e.getY());

            return true;
        }
    }

    // testing
//    private class CreateContextMenuListener implements OnCreateContextMenuListener {
//        @Override
//        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
//            MenuInflater menuInflater = getActivity().getMenuInflater();
//            menuInflater.inflate(R.menu.obstacle_context_menu,menu);
//        }
//    };
//
//    private class MenuItemClickListener implements MenuItem.OnMenuItemClickListener {
//        @Override
//        public boolean onMenuItemClick(@NonNull MenuItem item) {
//            Log.d(TAG, "onMenuItemClick: "+item.getItemId());
//            return false;
//        }
//    };
//
//    private Activity getActivity() {
//        Context context = getContext();
//        while (context instanceof ContextWrapper) {
//            if (context instanceof Activity) {
//                return (Activity)context;
//            }
//            context = ((ContextWrapper)context).getBaseContext();
//        }
//        return null;
//    }

    public void receiveOnDrag(DragEvent dragEvent, float x, float y){
        int type;
        ClipData clipData = dragEvent.getClipData();

        if (clipData!=null){
            // dragged onto canvas
            String item = clipData.getItemAt(0).getText().toString();
            switch (item){
                case "NEW_CAR":
                    type = CellValue.CAR;
                    newCell(type,x,y);
                    break;
                case "NEW_OBSTACLE":
                    type = CellValue.OBSTACLE;
                    newCell(type,x,y);
                    break;
                case "MOVE_CAR":
                    type = CellValue.MOVE_CAR;
                    moveCell(type,x,y, dragEvent.getLocalState());
                    break;
                case "MOVE_OBSTACLE":
                    type = CellValue.MOVE_OBSTACLE;
                    moveCell(type,x,y, dragEvent.getLocalState());
                    break;
            }
        } else {
            // dragged outside canvas
            Object object = dragEvent.getLocalState();
            if (object instanceof Obstacle){
                type = CellValue.MOVE_OBSTACLE;
                moveCell(type,x,y, dragEvent.getLocalState());
            } else if (object instanceof RobotCar){
                type = CellValue.MOVE_CAR;
                moveCell(type,x,y, dragEvent.getLocalState());
            }
            setPlacementCellValue(CellValue.EMPTY, true);

        }
    }

    private void newCell(int type, float x, float y) throws ArrayIndexOutOfBoundsException {
        int col = (int)(x / cellWidth);
        int row = (int)(y / cellHeight);
//        Log.d(TAG, "newCell: (x,y): "+x+" "+y);
//        Log.d(TAG, "newCell: (row,col): " +row+" "+col);
        boolean check;

        switch (type){
            case CellValue.CAR:
                // same code as move cell car
                check = legalPlacementBoundary(x,y,CellValue.CAR, 0, 0);
                if (!check){
                    Toast.makeText(getContext(), "Cannot place car here", Toast.LENGTH_SHORT).show();
                    break;
                }

                updateCarPosition(row,col,Direction.NORTH);
                setPlacementCellValue(CellValue.CAR, true);
                break;
            case CellValue.OBSTACLE:
                check = legalPlacementBoundary(x,y, CellValue.OBSTACLE,0,0);
                if (!check){
                    Toast.makeText(getContext(), "Cannot place obstacle here", Toast.LENGTH_SHORT).show();
                    break;
                }

                int obstacleId;

                if (!draggedOutQueue.isEmpty()){
                    obstacleId = draggedOutQueue.remove();
                } else {
                    obstacleId = ++numOfObstacles;
                }

                Obstacle obstacle = new Obstacle(x,y,row,col,obstacleId, false);
                arena.addObstacle(obstacle);
                sendObstacleChanges();
                setPlacementCellValue(CellValue.OBSTACLE, true);
                break;
        }
        
        invalidate();
        
    }

    private void moveCell(int type, float x, float y, Object object) throws ArrayIndexOutOfBoundsException {
        int col = (int)(x / cellWidth);
        int row = (int)(y / cellHeight);
        boolean check;

        switch (type){
            case CellValue.MOVE_CAR:

                if (x==-1 && y==-1){
                    clearCar(true);
                    clearSelectedObject();
                    break;
                }

                // same code as new cell car
                check = legalPlacementBoundary(x,y,CellValue.MOVE_CAR,0,0);
                if (!check){
                    Toast.makeText(getContext(), "Cannot move car here", Toast.LENGTH_SHORT).show();
                    break;
                }

                updateCarPosition(row,col,arena.getRobotCar().getDirection());
                setPlacementCellValue(CellValue.EMPTY, true);
                break;

            case CellValue.MOVE_OBSTACLE:
                Obstacle obstacle = (Obstacle) object;
                String sOldRow = Integer.toString(obstacle.getRow());
                String sOldCol = Integer.toString(obstacle.getCol());

                int oldRow = Integer.parseInt(sOldRow);
                int oldCol = Integer.parseInt(sOldCol);

                int obstacleId = arena.getObstacle(oldRow,oldCol,-1).getObstacleId();

                if (x==-1 && y==-1){
                    clearSelectedObject();
                    removeObstacle(oldRow,oldCol);
                    sendObstacleChanges();
                    Toast.makeText(getContext(), "Removed Obstacle "+obstacleId, Toast.LENGTH_SHORT).show();
                    draggedOutQueue.add(obstacleId);
                    break;
                }

                check = legalPlacementBoundary(x,y, CellValue.MOVE_OBSTACLE,0,0);
                if (!check){
                    Toast.makeText(getContext(), "Cannot move obstacle here", Toast.LENGTH_SHORT).show();
                    break;
                }

                updateObstacleLocation(oldRow,oldCol,row,col);
                sendObstacleChanges();
                setPlacementCellValue(CellValue.EMPTY, true);
                break;
        }
        invalidate();
    }

    private boolean legalPlacementBoundary(float x, float y, int type, int row, int col) {
        // use row and col argument only if x and y is 0.0F (null)
        if (x==0.0F && y==0.0F) {
            if (col < 0 || row < 0 || col >= getNumColumns() || row >= getNumRows()) return false;
        } else {
            col = (int)(x / cellWidth);
            row = (int)(y / cellHeight);
        }

        int[][] matrixArena = arena.getMatrixBoard();
        try {
            switch(type){
                case CellValue.OBSTACLE:
                case CellValue.MOVE_OBSTACLE:
                    if (matrixArena[row][col] != CellValue.EMPTY) return false;
                    break;
                case CellValue.CAR:
                case CellValue.MOVE_CAR:
                    // 3x3 uses row/col -1
                    // 2x2 uses row/col
                    for (int r=row-1;r<row+2;r++){
                        for (int c=col-1;c<col+2;c++){
                            if (matrixArena[r][c] == CellValue.OBSTACLE) return false;
                        }
                    }
                    break;
            }

        } catch (ArrayIndexOutOfBoundsException e){
            Log.e(TAG, "legalPlacementBoundary: ", e);
            return false;
        }

        return true;
    }

    public void clearMap(){
        arena = new ArenaProperty();
        arena.setArena(numRows,numColumns);
        numOfObstacles = 0;
        clearSelectedValues();
        sendObstacleChanges();
        draggedOutQueue = new ArrayDeque<>();
        setPlacementCellValue(CellValue.EMPTY, false);
        Toast.makeText(getContext(), "Cleared the map", Toast.LENGTH_SHORT).show();
        
        invalidate();
    }

    public void clearCar(boolean displayToast){
        RobotCar robotCar = arena.getRobotCar();

        if (displayToast) Toast.makeText(getContext(), "Removed RobotCar", Toast.LENGTH_SHORT).show();

        if (robotCar ==null) return;

        int[][] matrixBoard = arena.getMatrixBoard();

        int[] rowCol;
        HashMap<Integer, int[]> rowColMap = robotCar.getRowColMap();
        for (int i=0;i<rowColMap.size();i++){
            rowCol = rowColMap.get(i);
            assert rowCol != null;
            matrixBoard[rowCol[0]][rowCol[1]] = CellValue.EMPTY;
        }

        arena.setRobotCar(null);
        
        invalidate();
    }

    private void updateCarPosition(int row, int col, int direction) {
        clearCar(false);
        RobotCar robotCar = new RobotCar(row,col,direction);
        arena.addRobotCar(robotCar);
        invalidate();
    }

    private void clearSelectedObject(){
        selectedObstacle = null;
        selectedObject = CellValue.EMPTY;
        HomeFragment.setTvSelectedObject("None");
    }

    private void removeObstacle(int row, int col){
        Obstacle obstacle = arena.getObstacle(row,col,-1);

        if (obstacle==null) return;

        arena.getObstacleArrayList().remove(obstacle);
        arena.getMatrixBoard()[row][col] = CellValue.EMPTY;

        //Toast.makeText(getContext(), "Removed Obstacle "+obstacle.getObstacleId(), Toast.LENGTH_SHORT).show();

        invalidate();
    }

    public void clearAllObstacles(){
        for (Obstacle obstacle:arena.getObstacleArrayList()){
            arena.getMatrixBoard()[obstacle.getRow()][obstacle.getCol()] = CellValue.EMPTY;
        }
        arena.setObstacleArrayList(new ArrayList<>());

        clearSelectedObject();

        numOfObstacles = 0;

        sendObstacleChanges();

        draggedOutQueue = new ArrayDeque<>();

        setPlacementCellValue(CellValue.EMPTY, false);

        Toast.makeText(getContext(), "Removed all obstacles", Toast.LENGTH_SHORT).show();

        invalidate();
    }

    private void updateObstacleLocation(int oldRow, int oldCol, int newRow, int newCol){
        Obstacle obstacle = arena.getObstacle(oldRow,oldCol,-1);

        if (obstacle==null) return;

        obstacle.setRow(newRow);
        obstacle.setCol(newCol);
        arena.getMatrixBoard()[oldRow][oldCol] = CellValue.EMPTY;
        arena.getMatrixBoard()[newRow][newCol] = CellValue.OBSTACLE;

        invalidate();
    }

    public void resetObstacleValues(){
        for (Obstacle obstacle:arena.getObstacleArrayList()){
            obstacle.setTargetAlphaNum(null);
        }
        Toast.makeText(getContext(), "Obstacle values reset successfully", Toast.LENGTH_SHORT).show();

        invalidate();
    }

    private void dragCellObject(int row, int col, int curObj) throws ArrayIndexOutOfBoundsException{
        //Log.d(TAG, "dragCellObject: entered");

        ClipData.Item item;
        ClipData dragData;
        View.DragShadowBuilder myShadow;

        View v = getRootView();

        switch(curObj){

            case CellValue.OBSTACLE:
                Obstacle curObstacle = arena.getObstacle(row,col,-1);
                item = new ClipData.Item("MOVE_OBSTACLE");
                dragData = new ClipData("MOVE_OBSTACLE", new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
                myShadow = new HomeFragment.MyDragShadowBuilder(v, Objects.requireNonNull(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_obstacle, getContext().getTheme())));
                v.startDragAndDrop(dragData, myShadow, curObstacle,0);
                //Log.d(TAG, "dragCellObject: dragging obstacle "+curObstacle.getObstacleId());
                break;

            case CellValue.CAR:
                item = new ClipData.Item("MOVE_CAR");
                dragData = new ClipData("MOVE_CAR", new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
                myShadow  = new HomeFragment.MyDragShadowBuilder(v, Objects.requireNonNull(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_car_up, getContext().getTheme())));
                v.startDragAndDrop(dragData, myShadow, arena.getRobotCar(),0);
                //Log.d(TAG, "dragCellObject: dragging robot car");
                break;
        }

    }

    private RectF getObstacleDirectionRectF(Obstacle obstacle){
        RectF rectF = new RectF();

        float left,top,right,bottom;

        int direction = obstacle.getDirection();
        int row = obstacle.getRow();
        int col = obstacle.getCol();

        switch(direction){
            case Direction.NORTH:
                left = col * cellWidth;
                top = row * cellHeight;
                right = (col + 1) * cellWidth;
                bottom = (row + 0.1F) * cellHeight;
                rectF.set(left, top, right, bottom);
                break;
            case Direction.EAST:
                left = (col+0.9F) * cellWidth;
                top =  row * cellHeight;
                right = (col + 1) * cellWidth;
                bottom = (row + 1) * cellHeight;
                rectF.set(left, top, right, bottom);
                break;
            case Direction.SOUTH:
                left = col * cellWidth;
                top = (row+0.9F) * cellHeight;
                right = (col + 1) * cellWidth;
                bottom = (row + 1) * cellHeight;
                rectF.set(left, top, right, bottom);
                break;
            case Direction.WEST:
                left = col * cellWidth;
                top =  row * cellHeight;
                right = (col + 0.1F) * cellWidth;
                bottom = (row + 1) * cellHeight;
                rectF.set(left, top, right, bottom);
                break;
        }

        return rectF;
    }

    public Obstacle getSelectedObstacle(){
        return selectedObstacle;
    }

    public void setSelectedObject(int item){
        this.selectedObject = item;
    }

    public void clearSelectedValues(){
        selectedObject = CellValue.EMPTY;
        selectedObstacle = null;
        HomeFragment.setTvSelectedObject("None");
        invalidate();
    }

    private void drawSelectedBorder(Canvas canvas, float left, float top, float right, float bottom){
        float[] pts = {
                left,top,right,top,
                right,top,right,bottom,
                right,bottom,left,bottom,
                left,bottom,left,top
        };
        canvas.drawLines(pts,pSelectedBorder);
    }

    private void sendObstacleChanges(){
        JSONObject jsonMain = new JSONObject();
        try {
            // category
            jsonMain.put("cat","obstacles");

            // obstacles
            JSONObject jsonValue = new JSONObject();
            JSONArray obstacleArray = new JSONArray();
            JSONObject obstacleAttributes;
            for (Obstacle obstacle1: arena.getObstacleArrayList()){
                obstacleAttributes = new JSONObject();
                int[] newRowCol = convertRowCol(obstacle1.getRow(),obstacle1.getCol());
                obstacleAttributes.put("x",Integer.toString(newRowCol[1]));
                obstacleAttributes.put("y",Integer.toString(newRowCol[0]));
                obstacleAttributes.put("id",Integer.toString(obstacle1.getObstacleId()));
                obstacleAttributes.put("d",obstacle1.getDirection());
                obstacleArray.put(obstacleAttributes);
            }
            jsonValue.put("obstacles",obstacleArray);
            jsonValue.put("mode",Integer.toString(arenaLocation)); // mode=0 (indoors) , mode=1 (outdoors)
            jsonMain.put("value",jsonValue);

        } catch (Exception e){
            Log.e(TAG, "sendObstacleChanges: ", e);
        }

        if (btConnSvc == null) btConnSvc = BluetoothConnectionService.getInstance();
        if (btConnSvc.getState() != BluetoothConnectionService.STATE_CONNECTED){
            Toast.makeText(getContext(), "Device is not connected", Toast.LENGTH_SHORT).show();
            NormalTextFragment.appendFailedMessage(jsonMain.toString());
            return;
        }

        btConnSvc.sendMessage(jsonMain.toString());
    }
    
    public void receiveRobotCoords(int row, int col, int direction){
        if (!legalPlacementBoundary(0.0F,0.0F,CellValue.CAR,row,col)){
            Toast.makeText(getContext(), "Car moved to an illegal spot", Toast.LENGTH_SHORT).show();
            return;
        }

        updateCarPosition(row,col,direction);

        invalidate();
    }

    public void receiveObstacleCoords(int row, int col, String option){
        switch (option){
            case "ADD":
                if (!legalPlacementBoundary(0.0F,0.0F,CellValue.OBSTACLE,row,col)){
                    Toast.makeText(getContext(), "Attempted to add obstacle on an illegal spot", Toast.LENGTH_SHORT).show();
                    return;
                }

                float x = col * cellWidth;
                float y = row * cellHeight;

                newCell(CellValue.OBSTACLE,x,y);

                break;
            case "REMOVE":
                Obstacle obstacle = arena.getObstacle(row,col,-1);
                if (obstacle==null){
                    Toast.makeText(getContext(), "Attempted to remove a non-existing obstacle", Toast.LENGTH_SHORT).show();
                    return;
                }
                removeObstacle(row,col);
                break;
        }

        invalidate();
    }


    public void receiveTargetInfo(int obstacleId, String targetId){
        Obstacle obstacle = arena.getObstacle(-1,-1,obstacleId);
        if (obstacle==null){
            Toast.makeText(getContext(), "Obstacle "+obstacleId+" does not exist!", Toast.LENGTH_SHORT).show();
            return;
        }
        obstacle.setTargetAlphaNum(targetId);
        invalidate();
    }

    private void drawObstacle(Canvas canvas, int row, int col){
        Obstacle obstacle = arena.getObstacle(row,col,-1);

//        if (obstacle==null) {
//            Log.e(TAG, "drawObstacle: row,col "+row+" "+col);
//            return;
//        }

        // draw obstacle
        canvas.drawRect(col * cellWidth, row * cellHeight,
                (col + 1) * cellWidth, (row + 1) * cellHeight,
                pObstacle);

        // draw obstacle direction
        RectF rectF = getObstacleDirectionRectF(obstacle);
        //if (obstacle.isDirectionVisible()) // comment if to debug
            canvas.drawRect(rectF, pObstacleDirection);

        // draw obstacle number
        String targetAlphaNum = obstacle.getTargetAlphaNum();
        if (targetAlphaNum == null){
            canvas.drawText(Integer.toString(obstacle.getObstacleId()),(2 * col + 1)*cellWidth/2.0F,(2 * row + 1)*cellHeight/2.0F, pAlphaNumSmall);
        } else {
            canvas.drawText(targetAlphaNum,((2 * col + 1)*cellWidth/2.0F),((2 * row + 1)*cellHeight/2.0F), pAlphaNumBig);
        }

        // draw selected outline
        if (selectedObject == CellValue.OBSTACLE && selectedObstacle.getObstacleId() == obstacle.getObstacleId()){
            drawSelectedBorder(canvas, col * cellWidth, row * cellHeight, (col + 1) * cellWidth, (row + 1) * cellHeight);
        }
    }

    private void drawCar(Canvas canvas, int row, int col){
        // draw car
        RectF rect = new RectF((col-1) * cellWidth, (row-1) * cellHeight,(col + 2) * cellWidth, (row + 2) * cellHeight); // 3x3
        //RectF rect = new RectF(col * cellWidth, row * cellHeight,(col + 2) * cellWidth, (row + 2) * cellHeight); // 2x2
        bmCar = getCarBitmap();
        canvas.drawBitmap(bmCar, null, rect, null);

        // draw selected outline
        if (selectedObject == CellValue.CAR){
            drawSelectedBorder(canvas, (col-1) * cellWidth, (row-1) * cellHeight,(col + 2) * cellWidth, (row + 2) * cellHeight);
            //drawSelectedBorder(canvas, col * cellWidth, row * cellHeight,(col + 2) * cellWidth, (row + 2) * cellHeight); // 2x2
        }
    }

    private Bitmap getCarBitmap(){
        RobotCar robotCar = arena.getRobotCar();

        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_car_up);
        Matrix matrix = new Matrix();

        switch (robotCar.getDirection()){
            case Direction.NORTH:
                matrix.postRotate(0);
                break;
            case Direction.EAST:
                matrix.postRotate(90);
                break;
            case Direction.SOUTH:
                matrix.postRotate(180);
                break;
            case Direction.WEST:
                matrix.postRotate(270);
                break;
        }

        return Bitmap.createBitmap(bm,0,0,bmCar.getWidth(),bmCar.getHeight(),matrix,true);
    }

    public void setDirectionFromContextMenu(int directionResponse){
        String directionString = getDirectionString(directionResponse);
        int curObj = selectedObject;
        switch (curObj){
            case CellValue.CAR:
                RobotCar robotCar = arena.getRobotCar();
                robotCar.setDirection(directionResponse);
                Toast.makeText(getContext(),
                        "RobotCar is now facing "+directionString, Toast.LENGTH_SHORT).show();
                break;
            case CellValue.OBSTACLE:
                Obstacle obstacle = getSelectedObstacle();
                obstacle.setDirection(directionResponse);
                Toast.makeText(getContext(),
                        "Obstacle "+obstacle.getObstacleId()+" is now facing "+directionString, Toast.LENGTH_SHORT).show();
                break;
        }
        sendObstacleChanges();
        invalidate();
    }

    private String getDirectionString(int direction){
        switch(direction){
            case Direction.NORTH:
                return "North";
            case Direction.EAST:
                return "East";
            case Direction.SOUTH:
                return "South";
            case Direction.WEST:
                return "West";
            default:
                return "???";
        }
    }

    private void setSelectedObject(int cellValue, @Nullable Obstacle obstacle){
        switch (cellValue){
            case CellValue.EMPTY:
                selectedObject = CellValue.EMPTY;
                selectedObstacle = null;
                HomeFragment.setTvSelectedObject("None");
                break;
            case CellValue.CAR:
                selectedObject = CellValue.CAR;
                selectedObstacle = null;
                HomeFragment.setTvSelectedObject("RobotCar");
                break;
            case CellValue.OBSTACLE:
                selectedObject = CellValue.OBSTACLE;
                selectedObstacle = obstacle;
                assert selectedObstacle != null;
                HomeFragment.setTvSelectedObject("Obstacle "+selectedObstacle.getObstacleId());
                break;
        }
        invalidate();
    }

    public static int[] convertRowCol(int row, int col){
        int[] newRowCol = new int[2];
        newRowCol[0] = Math.abs(row-19);
        newRowCol[1] = col;
        return newRowCol;
    }

    public void setPlacementCellValue(int cellValue, boolean isDragged){
        switch (cellValue){
            case CellValue.CAR:
                if (placementCellValue == CellValue.CAR && !isDragged){
                    HomeFragment.setTvPlacementOption("None");
                    placementCellValue = CellValue.EMPTY;
                } else {
                    HomeFragment.setTvPlacementOption("Car");
                    placementCellValue = CellValue.CAR;
                }
                break;
            case CellValue.OBSTACLE:
                if (placementCellValue == CellValue.OBSTACLE && !isDragged){
                    HomeFragment.setTvPlacementOption("None");
                    placementCellValue = CellValue.EMPTY;
                } else {
                    int obstacleNum;

                    if (!draggedOutQueue.isEmpty()){
                        obstacleNum = draggedOutQueue.peek();
                    } else {
                        obstacleNum = PixelGridView.getInstance().numOfObstacles+1;
                    }

                    HomeFragment.setTvPlacementOption("Obstacle "+obstacleNum);
                    placementCellValue = CellValue.OBSTACLE;
                }
                break;
            case CellValue.EMPTY:
                HomeFragment.setTvPlacementOption("None");
                placementCellValue = CellValue.EMPTY;
                break;
        }
    }

    public void toggleLocation(){
        if (arenaLocation == ArenaLocation.INDOOR){
            arenaLocation = ArenaLocation.OUTDOOR;
            HomeFragment.setTvLocation("Outdoor");
        } else {
            arenaLocation = ArenaLocation.INDOOR;
            HomeFragment.setTvLocation("Indoor");
        }
        sendObstacleChanges();
    }

    public int getArenaLocation(){
        return arenaLocation;
    }

}
