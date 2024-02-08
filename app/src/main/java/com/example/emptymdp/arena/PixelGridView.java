package com.example.emptymdp.arena;

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
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.emptymdp.R;
import com.example.emptymdp.bluetooth.BluetoothConnectionService;
import com.example.emptymdp.fragments.HomeFragment;

import java.util.ArrayList;
import java.util.HashMap;

public class PixelGridView extends View {
    private final String TAG = "debugMap";
    private int numColumns, numRows; //  0-indexed:  int row = (int)(touchX / cellWidth); int col = (int)(touchY / cellHeight);
    private int cellWidth, cellHeight;
    private Paint pObstacle = new Paint();
    private Paint pObstacleDirection = new Paint();
    private Paint pAlphaNumSmall = new Paint();
    private Paint pAlphaNumBig = new Paint();
    private Paint pCar1 = new Paint();
    private Paint pCar2 = new Paint();
    private Paint pBorder = new Paint();
    private Paint pSelectedBorder = new Paint();
    private int selectedObject;
    private Obstacle selectedObstacle;
    Bitmap bmCar;
    private ArenaProperty arena;
    private int numOfObstacles = 0;
    private BluetoothConnectionService btConnSvc;
    GestureDetector gestureDetector;

    public interface CellValue {
        int EMPTY = 0;
        int OBSTACLE = 1;
        int CAR = 2;
        int MOVE_OBSTACLE = 3;
        int MOVE_CAR = 4;

    }

    public interface Direction {
        int NORTH = 0;
        int EAST = 90;
        int SOUTH = 180;
        int WEST = 270;
    }

    public PixelGridView(Context context) {
        this(context, null);
    }
    
    public PixelGridView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // touch events
        gestureDetector = new GestureDetector(context, new GestureListener());

        // paints
        pBorder.setColor(Color.BLACK);
        pAlphaNumSmall.setColor(Color.BLACK);
        pAlphaNumBig.setColor(Color.BLACK);
        pAlphaNumBig.setTextSize(24F);
        pCar1.setColor(Color.RED);
        pCar2.setColor(Color.GRAY);
        pObstacle.setColor(Color.YELLOW);
        pObstacleDirection.setColor(Color.RED);
        pSelectedBorder.setColor(Color.BLACK);
        pSelectedBorder.setStrokeWidth(3.0F);

        // car image
        bmCar = BitmapFactory.decodeResource(getResources(), R.drawable.ic_car_up);

        // create arena
        arena = new ArenaProperty();
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
        canvas.drawColor(Color.CYAN);

        boolean placedCar = false;

        if (numColumns == 0 || numRows == 0) {
            return;
        }

        int[][] matrixBoard = arena.getMatrixBoard();

        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numColumns; col++) {
                //canvas.drawText("r"+row+",c"+col,(2 * col + 1)*cellWidth/2,(2 * row + 1)*cellHeight/2,pAlphaNum);

                if (matrixBoard[row][col] == CellValue.OBSTACLE) {
                    drawObstacle(canvas,row,col);

                } else if (matrixBoard[row][col] == CellValue.CAR && !placedCar) {
                    drawCar(canvas,row,col);
                    placedCar = true;
                }

            }
        }

        int width = getWidth();
        int height = getHeight();

        for (int i = 1; i < numColumns; i++) {
            canvas.drawLine(i * cellWidth, 0, i * cellWidth, height, pBorder);
        }

        for (int i = 1; i < numRows; i++) {
            canvas.drawLine(0, i * cellHeight, width, i * cellHeight, pBorder);
        }
    }

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
            col = (int)(e.getX() / cellWidth);
            row = (int)(e.getY() / cellHeight);

            if (row>=numRows || col>=numColumns) return false;

            curObj = arena.getMatrixBoard()[row][col];

            if (curObj == CellValue.OBSTACLE){
                //Log.d(TAG, "onDown: selected obstacle");
                setSelectedObject(CellValue.OBSTACLE, arena.getObstacle(row,col));

            } else if (curObj == CellValue.CAR){
                //Log.d(TAG, "onDown: selected car");
                setSelectedObject(CellValue.CAR,null);

            } else if (curObj == CellValue.EMPTY){
                //Log.d(TAG, "onDown: selected empty cell");
                setSelectedObject(CellValue.EMPTY,null);
            }

            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) throws ArrayIndexOutOfBoundsException{
            Log.d(TAG, "onDoubleTap: "+curObj);

            if (curObj!=CellValue.EMPTY)
                showContextMenu(e.getX(), e.getY());

            return true;
        }
    }

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
                clearCar(false);
                RobotCar robotCar = new RobotCar(x,y,row,col, Direction.NORTH);
                arena.addRobotCar(robotCar);
                break;
            case CellValue.OBSTACLE:
                check = legalPlacementBoundary(x,y, CellValue.OBSTACLE,0,0);
                if (!check){
                    Toast.makeText(getContext(), "Cannot place obstacle here", Toast.LENGTH_SHORT).show();
                    break;
                }
                Obstacle obstacle = new Obstacle(x,y,row,col,++numOfObstacles, false);
                arena.addObstacle(obstacle);
                sendObstacleChanges(obstacle);
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
                int direction = arena.getRobotCar().getDirection();
                clearCar(false);
                RobotCar robotCar = new RobotCar(x,y,row,col,direction);
                arena.addRobotCar(robotCar);
                break;

            case CellValue.MOVE_OBSTACLE:
                Obstacle obstacle = (Obstacle) object;
                String sOldRow = Integer.toString(obstacle.getRow());
                String sOldCol = Integer.toString(obstacle.getCol());

                int oldRow = Integer.parseInt(sOldRow);
                int oldCol = Integer.parseInt(sOldCol);

                int obstacleId = arena.getObstacle(oldRow,oldCol).getObstacleId();

                if (x==-1 && y==-1){
                    clearSelectedObject();
                    removeObstacle(oldRow,oldCol);
                    Toast.makeText(getContext(), "Removed Obstacle "+obstacleId, Toast.LENGTH_SHORT).show();
                    break;
                }

                check = legalPlacementBoundary(x,y, CellValue.MOVE_OBSTACLE,0,0);
                if (!check){
                    Toast.makeText(getContext(), "Cannot move obstacle here", Toast.LENGTH_SHORT).show();
                    break;
                }

                updateObstacleLocation(oldRow,oldCol,row,col);
                sendObstacleChanges(obstacle);

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
                    for (int r=row;r<row+2;r++){
                        for (int c=col;c<col+2;c++){
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
            matrixBoard[rowCol[0]][rowCol[1]] = CellValue.EMPTY;
        }

        arena.setRobotCar(null);
        
        invalidate();
    }

    private void clearSelectedObject(){
        selectedObstacle = null;
        selectedObject = CellValue.EMPTY;
        HomeFragment.setTvSelectedObject("None");
    }

    private void removeObstacle(int row, int col){
        Obstacle obstacle = arena.getObstacle(row,col);

        if (obstacle==null) return;

        arena.getObstacleArrayList().remove(obstacle);
        arena.getMatrixBoard()[row][col] = CellValue.EMPTY;

        //Toast.makeText(getContext(), "Removed Obstacle "+obstacle.getObstacleId(), Toast.LENGTH_SHORT).show();

        invalidate();
    }

    public void removeAllObstacles(){
        for (Obstacle obstacle:arena.getObstacleArrayList()){
            arena.getMatrixBoard()[obstacle.getRow()][obstacle.getCol()] = CellValue.EMPTY;
            break;
        }
        arena.setObstacleArrayList(new ArrayList<>());

        clearSelectedObject();

        Toast.makeText(getContext(), "Removed all obstacles", Toast.LENGTH_SHORT).show();
        
        invalidate();
    }

    private void updateObstacleLocation(int oldRow, int oldCol, int newRow, int newCol){
        Obstacle obstacle = arena.getObstacle(oldRow,oldCol);

        if (obstacle==null) return;

        obstacle.setRow(newRow);
        obstacle.setCol(newCol);
        arena.getMatrixBoard()[oldRow][oldCol] = CellValue.EMPTY;
        arena.getMatrixBoard()[newRow][newCol] = CellValue.OBSTACLE;

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
                Obstacle curObstacle = arena.getObstacle(row,col);
                item = new ClipData.Item("MOVE_OBSTACLE");
                dragData = new ClipData("MOVE_OBSTACLE", new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
                myShadow = new HomeFragment.MyDragShadowBuilder(v, getResources().getDrawable(R.drawable.ic_obstacle, getContext().getTheme()));
                v.startDragAndDrop(dragData, myShadow, curObstacle,0);
                //Log.d(TAG, "dragCellObject: dragging obstacle "+curObstacle.getObstacleId());
                break;

            case CellValue.CAR:
                item = new ClipData.Item("MOVE_CAR");
                dragData = new ClipData("MOVE_CAR", new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
                myShadow  = new HomeFragment.MyDragShadowBuilder(v, getResources().getDrawable(R.drawable.ic_car_up, getContext().getTheme()));
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
                bottom = (row + 0.2F) * cellHeight;
                rectF.set(left, top, right, bottom);
                break;
            case Direction.EAST:
                left = (col+0.8F) * cellWidth;
                top =  row * cellHeight;
                right = (col + 1) * cellWidth;
                bottom = (row + 1) * cellHeight;
                rectF.set(left, top, right, bottom);
                break;
            case Direction.SOUTH:
                left = col * cellWidth;
                top = (row+0.8F) * cellHeight;
                right = (col + 1) * cellWidth;
                bottom = (row + 1) * cellHeight;
                rectF.set(left, top, right, bottom);
                break;
            case Direction.WEST:
                left = col * cellWidth;
                top =  row * cellHeight;
                right = (col + 0.2F) * cellWidth;
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

    public int getSelectedObject(){
        return selectedObject;
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

    public void moveCarWithCommand(String command){
        // moves car onscreen when user sends movement command
    }

    private void sendObstacleChanges(Obstacle obstacle){
        if (btConnSvc == null) btConnSvc = BluetoothConnectionService.getInstance();
        if (btConnSvc.getState() != BluetoothConnectionService.STATE_CONNECTED){
            Toast.makeText(getContext(), "Device is not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        String msg = "OBSTACLE,"+obstacle.getObstacleId()+","+obstacle.getRow()+","+obstacle.getCol()+","+obstacle.getTargetAlphaNum();
        btConnSvc.sendMessage(msg);
    }
    
    public void receiveRobotCoords(int row, int col, int direction){
        if (!legalPlacementBoundary(0.0F,0.0F,CellValue.CAR,row,col)){
            Toast.makeText(getContext(), "Car moved to an illegal spot", Toast.LENGTH_SHORT).show();
            return;
        }

        clearCar(false);

        RobotCar robotCar = new RobotCar(col,row,row,col,direction);
        arena.addRobotCar(robotCar);

        invalidate();
    }

    private void drawObstacle(Canvas canvas, int row, int col){
        Obstacle obstacle = arena.getObstacle(row,col);

        // draw obstacle
        canvas.drawRect(col * cellWidth, row * cellHeight,
                (col + 1) * cellWidth, (row + 1) * cellHeight,
                pObstacle);

        // draw obstacle direction
        RectF rectF = getObstacleDirectionRectF(obstacle);
        //if (obstacle.isDirectionVisible())
            canvas.drawRect(rectF, pObstacleDirection);

        // draw obstacle number
        String targetAlphaNum = obstacle.getTargetAlphaNum();
        if (targetAlphaNum == null){
            canvas.drawText(Integer.toString(obstacle.getObstacleId()),(2 * col + 1)*cellWidth/2,(2 * row + 1)*cellHeight/2, pAlphaNumSmall);
        } else {
            canvas.drawText(targetAlphaNum,(2 * col + 1)*cellWidth/2,(2 * row + 1)*cellHeight/2, pAlphaNumSmall);
        }

        // draw selected outline
        if (selectedObject == CellValue.OBSTACLE && selectedObstacle.getObstacleId() == obstacle.getObstacleId()){
            drawSelectedBorder(canvas, col * cellWidth, row * cellHeight, (col + 1) * cellWidth, (row + 1) * cellHeight);
        }
    }

    private void drawCar(Canvas canvas, int row, int col){
        // draw car
//                    RectF rect = new RectF((row-1) * cellWidth, (col-1) * cellHeight,(row + 2) * cellWidth, (col + 2) * cellHeight); // 3x3
        RectF rect = new RectF(col * cellWidth, row * cellHeight,(col + 2) * cellWidth, (row + 2) * cellHeight); // 2x2
        bmCar = getCarBitmap();
        canvas.drawBitmap(bmCar, null, rect, null);

        // draw selected outline
        if (selectedObject == CellValue.CAR){
            drawSelectedBorder(canvas, col * cellWidth, row * cellHeight,(col + 2) * cellWidth, (row + 2) * cellHeight);
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
                HomeFragment.setTvSelectedObject("Obstacle "+selectedObstacle.getObstacleId());
                break;
        }
        invalidate();
    }

}
