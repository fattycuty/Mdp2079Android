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

import com.example.emptymdp.R;
import com.example.emptymdp.fragments.HomeFragment;

import java.util.ArrayList;

public class PixelGridView extends View {
    private final String TAG = "debugMap";
    private int numColumns, numRows; //  0-indexed:  int row = (int)(touchX / cellWidth); int col = (int)(touchY / cellHeight);
    private int cellWidth, cellHeight;
    private Paint pObstacle = new Paint();
    private Paint pObstacleDirection = new Paint();
    private Paint pAlphaNum = new Paint();
    private Paint pCar1 = new Paint();
    private Paint pCar2 = new Paint();
    private Paint pBorder = new Paint();
    private int selectedItem;
    Bitmap bmCar;
    private ArenaProperty arena;
    private int numOfObstacles = 0;

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
        pBorder.setColor(Color.BLACK);
        pAlphaNum.setColor(Color.BLACK);
        pCar1.setColor(Color.RED);
        pCar2.setColor(Color.GRAY);
        pObstacle.setColor(Color.YELLOW);
        pObstacleDirection.setColor(Color.RED);
        bmCar = BitmapFactory.decodeResource(getResources(), R.drawable.ic_car_up);
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

        //int obstacleCount = 1;
        Obstacle curObstacle = null;

        if (numColumns == 0 || numRows == 0) {
            return;
        }

        int[][] matrixBoard = arena.getMatrixBoard();

        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numColumns; col++) {
                if (matrixBoard[row][col] == CellValue.OBSTACLE) {

                    // obstacle
                    canvas.drawRect(row * cellWidth, col * cellHeight,
                            (row + 1) * cellWidth, (col + 1) * cellHeight,
                            pObstacle);

                    // obstacle direction
                    for (Obstacle obstacle:arena.getObstacleArrayList()){
                        if (obstacle.getRow()==row && obstacle.getCol()==col){
                            curObstacle = obstacle;
                            break;
                        }
                    }

                    RectF rectF = getObstacleDirectionRectF(curObstacle);
                    canvas.drawRect(rectF, pObstacleDirection);

                    // obstacle number
                    canvas.drawText(Integer.toString(curObstacle.getObstacleId()),(2 * row + 1)*cellWidth/2,(2 * col + 1)*cellHeight/2,pAlphaNum);

                } else if (matrixBoard[row][col] == CellValue.CAR) {
                    // car
                    RectF rect = new RectF((row-1) * cellWidth, (col-1) * cellHeight,(row + 2) * cellWidth, (col + 2) * cellHeight);
                    canvas.drawBitmap(bmCar, null, rect, null);
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
        float touchX = event.getX();
        float touchY = event.getY();
        int row = (int)(touchX / cellWidth);
        int col = (int)(touchY / cellHeight);

        int curObj = arena.getMatrixBoard()[row][col];

        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                final GestureDetector gestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
                    public void onLongPress(MotionEvent e) {
                        Log.d(TAG, "onLongPress: on touch event fired");
                        dragCellObject(row,col,curObj);
                    }
                });

                if (curObj == CellValue.OBSTACLE){
                    rotateObstacle(row,col);
                }

                if (curObj == CellValue.CAR){
                    rotateCar();
                }

                if (curObj != CellValue.EMPTY){
                    return gestureDetector.onTouchEvent(event);
                }

                newCell(selectedItem,touchX, touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                //Log.d(TAG, "onTouchEvent: action move");
                break;
            case MotionEvent.ACTION_UP:
                //Log.d(TAG, "onTouchEvent: action up");
                break;
        }
        return true;
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
            } else if (object instanceof RoboCar){
                type = CellValue.MOVE_CAR;
                moveCell(type,x,y, dragEvent.getLocalState());
            }
        }
    }

    private void newCell(int type, float x, float y) throws ArrayIndexOutOfBoundsException {
        int row = (int)(x / cellWidth);
        int col = (int)(y / cellHeight);
//        Log.d(TAG, "newCell: (x,y): "+x+" "+y);
//        Log.d(TAG, "newCell: (row,col): " +row+" "+col);
        boolean check;

        switch (type){
            case CellValue.CAR:
                // same code as move cell car
                check = legalNewBoundary(x,y,CellValue.CAR);
                if (!check){
                    Toast.makeText(getContext(), "Cannot place car here", Toast.LENGTH_SHORT).show();
                    break;
                }
                clearCar();
                RoboCar roboCar = new RoboCar(x,y,row,col);
                arena.addRoboCar(roboCar);
                break;
            case CellValue.OBSTACLE:
                check = legalNewBoundary(x,y, CellValue.OBSTACLE);
                if (!check){
                    Toast.makeText(getContext(), "Cannot place obstacle here", Toast.LENGTH_SHORT).show();
                    break;
                }
                Obstacle obstacle = new Obstacle(x,y,row,col,++numOfObstacles);
                arena.addObstacle(obstacle);
                break;
        }
        invalidate();
        
    }

    private void moveCell(int type, float x, float y, Object object) throws ArrayIndexOutOfBoundsException {
        int row = (int)(x / cellWidth);
        int col = (int)(y / cellHeight);
        boolean check;

        switch (type){
            case CellValue.MOVE_CAR:

                if (x==-1 && y==-1){
                    clearCar();
                    Toast.makeText(getContext(), "Removed car", Toast.LENGTH_SHORT).show();
                    break;
                }

                // same code as new cell car
                check = legalMoveBoundary(x,y,CellValue.MOVE_CAR);
                if (!check){
                    Toast.makeText(getContext(), "Cannot move car here", Toast.LENGTH_SHORT).show();
                    break;
                }
                clearCar();
                RoboCar roboCar = new RoboCar(x,y,row,col);
                arena.addRoboCar(roboCar);
                break;

            case CellValue.MOVE_OBSTACLE:
                Obstacle obstacle = (Obstacle) object;
                String sOldRow = Integer.toString(obstacle.getRow());
                String sOldCol = Integer.toString(obstacle.getCol());

                int oldRow = Integer.parseInt(sOldRow);
                int oldCol = Integer.parseInt(sOldCol);

                if (x==-1 && y==-1){
                    removeObstacle(oldRow,oldCol);
                    Toast.makeText(getContext(), "Removed obstacle", Toast.LENGTH_SHORT).show();
                    break;
                }

                check = legalMoveBoundary(x,y, CellValue.MOVE_OBSTACLE);
                if (!check){
                    Toast.makeText(getContext(), "Cannot move obstacle here", Toast.LENGTH_SHORT).show();
                    break;
                }

                updateObstacle(oldRow,oldCol,row,col);
                break;
        }
        invalidate();
    }

    public void setSelectedItem(int item){
        this.selectedItem = item;
    }

    private boolean legalNewBoundary(float x, float y, int type) {
        int row = (int)(x / cellWidth);
        int column = (int)(y / cellHeight);
        int[][] matrixArena = arena.getMatrixBoard();

        try {
            for (int r=row-1;r<row+2;r++){
                for (int c=column-1;c<column+2;c++){
                    switch(type){
                        case CellValue.OBSTACLE:
                            if (r<0 || c<0 || c>=numColumns-1 || r>=numRows) continue;
                            if (matrixArena[r][c] == CellValue.CAR) return false;
                            break;
                        case CellValue.CAR:
                            if (matrixArena[r][c] == CellValue.OBSTACLE) return false;
                            break;
                    }

                }
            }
        } catch (ArrayIndexOutOfBoundsException e){
            Log.e(TAG, "legalNewBoundary: ", e);
            return false;
        }

        return true;
    }

    private boolean legalMoveBoundary(float x, float y, int type) {
        int row = (int)(x / cellWidth);
        int column = (int)(y / cellHeight);
        int[][] matrixArena = arena.getMatrixBoard();

        try {
            for (int r=row-1;r<row+2;r++){
                for (int c=column-1;c<column+2;c++){
                    switch(type){
                        case CellValue.MOVE_OBSTACLE:
                            if (r<0 || c<0 || c>=numColumns-1 || r>=numRows) continue;
                            if (matrixArena[r][c] == CellValue.CAR) return false;
                            break;
                        case CellValue.MOVE_CAR:
                            if (matrixArena[r][c] == CellValue.OBSTACLE) return false;
                            break;
                    }

                }
            }
        } catch (ArrayIndexOutOfBoundsException e){
            Log.e(TAG, "legalNewBoundary: ", e);
            return false;
        }

        return true;
    }

    public void clearMap(){
        arena = new ArenaProperty();
        arena.setArena(numRows,numColumns);
        invalidate();
    }

    private void clearCar(){
        RoboCar roboCar = arena.getRoboCar();

        if (roboCar==null) return;

        int[][] matrixBoard = arena.getMatrixBoard();
        matrixBoard[roboCar.getRow()][roboCar.getCol()] = CellValue.EMPTY;
        arena.setRoboCar(null);
        invalidate();
    }

    private void removeObstacle(int row, int col){
        for (Obstacle obstacle:arena.getObstacleArrayList()){
            if (obstacle.getRow()==row && obstacle.getCol()==col){
                arena.getObstacleArrayList().remove(obstacle);
                arena.getMatrixBoard()[row][col] = CellValue.EMPTY;
                break;
            }
        }
        invalidate();
    }

    private void removeAllObstacles(){
        for (Obstacle obstacle:arena.getObstacleArrayList()){
            arena.getMatrixBoard()[obstacle.getRow()][obstacle.getCol()] = CellValue.EMPTY;
            break;
        }
        arena.setObstacleArrayList(new ArrayList<>());
        invalidate();
    }

    private void updateObstacle(int oldRow, int oldCol, int newRow, int newCol){
        for (Obstacle obstacle:arena.getObstacleArrayList()){
            if (obstacle.getRow()==oldRow && obstacle.getCol()==oldCol){
                obstacle.setRow(newRow);
                obstacle.setCol(newCol);
                arena.getMatrixBoard()[oldRow][oldCol] = CellValue.EMPTY;
                arena.getMatrixBoard()[newRow][newCol] = CellValue.OBSTACLE;
                break;
            }
        }
        invalidate();
    }

    private void rotateObstacle(int row, int col){
        for (Obstacle obstacle:arena.getObstacleArrayList()){
            if (obstacle.getRow()==row && obstacle.getCol()==col) {
                switch (obstacle.getDirection()){
                    case Direction.NORTH:
                        obstacle.setDirection(Direction.EAST);
                        break;
                    case Direction.EAST:
                        obstacle.setDirection(Direction.SOUTH);
                        break;
                    case Direction.SOUTH:
                        obstacle.setDirection(Direction.WEST);
                        break;
                    case Direction.WEST:
                        obstacle.setDirection(Direction.NORTH);
                        break;
                }
                break;
            }
        }
        invalidate();
    }

    public void rotateCar(){
        RoboCar roboCar = arena.getRoboCar();
        if (roboCar==null) {
            Toast.makeText(getContext(), "Place a car in the arena first", Toast.LENGTH_SHORT).show();
            return;
        }
        switch (roboCar.getDirection()){
            case Direction.NORTH:
                roboCar.setDirection(Direction.EAST);
                break;
            case Direction.EAST:
                roboCar.setDirection(Direction.SOUTH);
                break;
            case Direction.SOUTH:
                roboCar.setDirection(Direction.WEST);
                break;
            case Direction.WEST:
                roboCar.setDirection(Direction.NORTH);
                break;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        bmCar = Bitmap.createBitmap(bmCar,0,0,bmCar.getWidth(),bmCar.getHeight(),matrix,true);
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
                Obstacle curObstacle = null;
                for (Obstacle obstacle:arena.getObstacleArrayList()){
                    if (obstacle.getRow()==row && obstacle.getCol()==col){
                        curObstacle = obstacle;
                        break;
                    }
                }
                item = new ClipData.Item("MOVE_OBSTACLE");
                dragData = new ClipData("MOVE_OBSTACLE", new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
                myShadow = new HomeFragment.MyDragShadowBuilder(v, getResources().getDrawable(R.drawable.ic_obstacle, getContext().getTheme()));
                v.startDragAndDrop(dragData, myShadow, curObstacle,0);
                break;

            case CellValue.CAR:
                item = new ClipData.Item("MOVE_CAR");
                dragData = new ClipData("MOVE_OBSTACLE", new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
                myShadow  = new HomeFragment.MyDragShadowBuilder(v, getResources().getDrawable(R.drawable.ic_car_up, getContext().getTheme()));
                v.startDragAndDrop(dragData, myShadow, arena.getRoboCar(),0);
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
                left = row * cellWidth;
                top = col * cellHeight;
                right = (row + 1) * cellWidth;
                bottom = (float) ((col + 0.2) * cellHeight);
                rectF.set(left, top, right, bottom);
                break;
            case Direction.EAST:
                left = (float) ((row+0.8) * cellWidth);
                top =  col * cellHeight;
                right = (row + 1) * cellWidth;
                bottom = (col + 1) * cellHeight;
                rectF.set(left, top, right, bottom);
                break;
            case Direction.SOUTH:
                left = row * cellWidth;
                top = (float) ((col+0.8) * cellHeight);
                right = (row + 1) * cellWidth;
                bottom = (col + 1) * cellHeight;
                rectF.set(left, top, right, bottom);
                break;
            case Direction.WEST:
                left = row * cellWidth;
                top =  col * cellHeight;
                right = (float) ((row + 0.2) * cellWidth);
                bottom = (col + 1) * cellHeight;
                rectF.set(left, top, right, bottom);
                break;
        }

        return rectF;
    }
}
