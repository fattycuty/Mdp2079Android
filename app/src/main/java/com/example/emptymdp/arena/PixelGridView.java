package com.example.emptymdp.arena;

import android.content.ClipData;
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
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.example.emptymdp.R;

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

    public interface CellValue {
        int EMPTY = 0;
        int OBSTACLE = 1;
        int CAR = 2;
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

        int obstacleCount = 1;

        if (numColumns == 0 || numRows == 0) {
            return;
        }

        int matrixBoard[][] = arena.getMatrixBoard();

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
                            RectF rectF = getObstacleDirectionRectF(obstacle);
                            canvas.drawRect(rectF, pObstacleDirection);
                            break;
                        }
                    }

                    // obstacle number
                    canvas.drawText(Integer.toString(obstacleCount++),(2 * row + 1)*cellWidth/2,(2 * col + 1)*cellHeight/2,pAlphaNum);

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

        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                final GestureDetector gestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
                    public void onLongPress(MotionEvent e) {
                        Log.d(TAG, "onLongPress: on touch event fired");
                        //dragCellObject(touchX,touchY, curObj);
                    }
                });
//                if (curObj != CellValue.EMPTY){
//                    return gestureDetector.onTouchEvent(event);
//                }
                colorCell(selectedItem,touchX, touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d(TAG, "onTouchEvent: action move");
                //dragCellObject(touchX, touchY, curObj);
                break;
            case MotionEvent.ACTION_UP:
                Log.d(TAG, "onTouchEvent: action up");
                break;
        }
        return true;
    }

    public void receiveOnDrag(ClipData data, float x, float y){
        String item = data.getItemAt(0).getText().toString();
        int type;
        switch (item){
            case "CAR":
                type = CellValue.CAR;
                colorCell(type,x,y);
                break;
            case "OBSTACLE":
                type = CellValue.OBSTACLE;
                colorCell(type,x,y);
                break;
            default:
                type = CellValue.EMPTY;
                colorCell(type,x,y);
                break;
        }
    }

    private void colorCell(int type, float x, float y) throws ArrayIndexOutOfBoundsException {
        int row = (int)(x / cellWidth);
        int col = (int)(y / cellHeight);
//        Log.d(TAG, "colorCell: (x,y): "+x+" "+y);
//        Log.d(TAG, "colorCell: (row,col): " +row+" "+col);
        boolean check;

        switch (type){
            case CellValue.CAR:
                check = carWithinBoundary(x,y);
                if (!check){
                    Toast.makeText(getContext(), "Car not within boundary", Toast.LENGTH_SHORT).show();
                    break;
                }
                clearCar();
                RoboCar roboCar = new RoboCar(x,y,row,col);
                arena.addRoboCar(roboCar);
                break;
            case CellValue.OBSTACLE:
                check = obstacleWithinBoundary(x,y);
                if (!check){
                    Toast.makeText(getContext(), "Cannot place obstacle here", Toast.LENGTH_SHORT).show();
                    break;
                }
                Obstacle obstacle = new Obstacle(x,y,row,col);
                arena.addObstacle(obstacle);
                break;
        }
        invalidate();
        
    }

    public void setSelectedItem(int item){
        this.selectedItem = item;
    }

    private boolean carWithinBoundary(float x, float y){
        int row = (int)(x / cellWidth);
        int column = (int)(y / cellHeight);
        int[][] matrixArena = arena.getMatrixBoard();

        try {
            for (int r=row-1;r<row+2;r++){
                for (int c=column-1;c<column+2;c++){
                    if (matrixArena[r][c] == CellValue.OBSTACLE) return false;
                }
            }
        } catch (ArrayIndexOutOfBoundsException e){
            Log.e(TAG, "carWithinBoundary: ", e);
            return false;
        }

        return true;
    }

    private boolean obstacleWithinBoundary(float x, float y){
        int row = (int)(x / cellWidth);
        int column = (int)(y / cellHeight);
        int[][] matrixArena = arena.getMatrixBoard();

        try {
            for (int r=row-1;r<row+2;r++){
                for (int c=column-1;c<column+2;c++){
                    if (r<0 || c<0 || c>=numColumns-1 || r>=numRows) continue;
                    if (matrixArena[r][c] == CellValue.CAR) return false;
                }
            }
        } catch (ArrayIndexOutOfBoundsException e){
            Log.e(TAG, "osbtacleWithinBoundary: ", e);
            return false;
        }
        return true;
    }

    public void clearMap(){
        arena = new ArenaProperty();
        arena.setArena(numRows,numColumns);
        invalidate();
    }

    public void clearCar(){
        RoboCar roboCar = arena.getRoboCar();

        if (roboCar==null) return;

        int[][] matrixBoard = arena.getMatrixBoard();
        matrixBoard[roboCar.getRow()][roboCar.getCol()] = CellValue.EMPTY;
        arena.setRoboCar(null);
        invalidate();
    }

    public void rotateCar(){
        RoboCar roboCar = arena.getRoboCar();
        if (roboCar==null) {
            Toast.makeText(getContext(), "Place a car on the arena first", Toast.LENGTH_SHORT).show();
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

    public void dragCellObject(float x, float y, int curObj) throws ArrayIndexOutOfBoundsException{
        Log.d(TAG, "dragCellObject: "+curObj);
        int row = (int)(x / cellWidth);
        int col = (int)(y / cellHeight);
        switch (curObj){
            case CellValue.EMPTY:
                break;
            case CellValue.CAR:
                break;
            case CellValue.OBSTACLE:
                break;
        }
        invalidate();

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
