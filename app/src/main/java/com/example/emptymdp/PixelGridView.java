package com.example.emptymdp;

import android.content.ClipData;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;

public class PixelGridView extends View {
    private final String TAG = "debugMap";
    private int numColumns, numRows;
    private int cellWidth, cellHeight;
    private Paint pObstacle = new Paint();
    private Paint pAlphaNum = new Paint();
    private Paint pCar1 = new Paint();
    private Paint pCar2 = new Paint();
    private Paint pBorder = new Paint();
    private int selectedItem;
    private int[][] cellValue;
    private int[] carPosition = {-1,-1};
    private int carDirection = Direction.NORTH;
    private int draggingObject = CellValue.EMPTY;
    Bitmap bmCar;

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
        bmCar = BitmapFactory.decodeResource(getResources(),R.drawable.ic_car_up);
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

        cellWidth = getWidth() / numColumns;
        cellHeight = getHeight() / numRows;

        cellValue = new int[numColumns][numRows];

        for (int i = 0; i < numColumns; i++) {
            for (int j = 0; j < numRows; j++) {
                cellValue[i][j] = CellValue.EMPTY;
            }
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.CYAN);

        int obstacleCount = 1;

        if (numColumns == 0 || numRows == 0) {
            return;
        }

        for (int i = 0; i < numColumns; i++) {
            for (int j = 0; j < numRows; j++) {
                int cell = cellValue[i][j];
                if (cell == CellValue.OBSTACLE) {
                    canvas.drawRect(i * cellWidth, j * cellHeight,
                            (i + 1) * cellWidth, (j + 1) * cellHeight,
                            pObstacle);
                    //canvas.drawText(Integer.toString(obstacleCount),i * cellWidth, j * cellHeight,pAlphaNum);
                } else if (cell == CellValue.CAR){
                    RectF rect = new RectF((i-1) * cellWidth, (j-1) * cellHeight,(i + 2) * cellWidth, (j + 2) * cellHeight);
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
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();
        int row = (int)(touchX / cellWidth);
        int col = (int)(touchY / cellHeight);
        int curObj = cellValue[row][col];

        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                final GestureDetector gestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
                    public void onLongPress(MotionEvent e) {
                        log("long press");
                        dragCellObject(touchX,touchY, curObj);
                    }
                });
                if (curObj != CellValue.EMPTY){
                    //draggingObject = curObj;
                    return gestureDetector.onTouchEvent(event);
                }
                colorCell(selectedItem,touchX, touchY);
                break;
            case MotionEvent.ACTION_MOVE:
                log("action move");
                this.dragCellObject(touchX, touchY, curObj);
                break;
            case MotionEvent.ACTION_UP:
                log("action up");
                draggingObject = CellValue.EMPTY;
                break;
        }
        return true;
    }

    private void log(String logMessage){
        Log.d(TAG,logMessage);
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
        int column = (int)(y / cellHeight);

        switch (type){
            case CellValue.CAR:
                boolean check = carWithinBoundary(x,y);
                if (!check){
                    Toast.makeText(getContext(), "Car not within boundary", Toast.LENGTH_SHORT).show();
                    break;
                }
                clearCar();
                cellValue[row][column] = CellValue.CAR;
                break;
            case CellValue.OBSTACLE:
                cellValue[row][column] = CellValue.OBSTACLE;
                break;
            default:
                cellValue[row][column] = CellValue.EMPTY;
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
        try {
            for (int r=row-1;r<row+2;r++){
                for (int c=column-1;c<column+2;c++){
                    if (cellValue[r][c] == CellValue.OBSTACLE) return false;
                }
            }
        } catch (ArrayIndexOutOfBoundsException e){
            log(e.getMessage());
            return false;
        }
        return true;
    }

    public void clearMap(){
        for (int i = 0; i < numColumns; i++) {
            for (int j = 0; j < numRows; j++) {
                cellValue[i][j] = CellValue.EMPTY;
            }
        }
        invalidate();
    }

    public void clearCar(){
        for (int i = 0; i < numColumns; i++) {
            for (int j = 0; j < numRows; j++) {
                if (cellValue[i][j] == CellValue.CAR)
                    cellValue[i][j] = CellValue.EMPTY;
            }
        }
        invalidate();
    }

    public void rotateCar(){
        switch (carDirection){
            case Direction.NORTH:
                carDirection = Direction.EAST;
                break;
            case Direction.EAST:
                carDirection = Direction.SOUTH;
                break;
            case Direction.SOUTH:
                carDirection = Direction.WEST;
                break;
            case Direction.WEST:
                carDirection = Direction.NORTH;
                break;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        bmCar = Bitmap.createBitmap(bmCar,0,0,bmCar.getWidth(),bmCar.getHeight(),matrix,true);
        invalidate();
    }

    public void dragCellObject(float x, float y, int curObj) throws ArrayIndexOutOfBoundsException{
        log("drag obj "+curObj);
        int row = (int)(x / cellWidth);
        int col = (int)(y / cellHeight);
        switch (curObj){
            case CellValue.EMPTY:
                break;
            case CellValue.CAR:
                break;
            case CellValue.OBSTACLE:
                cellValue[row][col] = CellValue.OBSTACLE;
                break;
        }
        invalidate();

    }
}
