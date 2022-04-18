package com.example.air_hockey;

import static android.opengl.GLES10.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES10.glClear;
import static android.opengl.GLES10.glClearColor;
import static android.opengl.GLES10.glViewport;
import static android.opengl.GLES20.glLineWidth;
import static android.opengl.Matrix.invertM;
import static android.opengl.Matrix.multiplyMM;
import static android.opengl.Matrix.multiplyMV;
import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.setIdentityM;
import static android.opengl.Matrix.setLookAtM;
import static android.opengl.Matrix.translateM;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.air_hockey.objects.GameBoard;
import com.example.air_hockey.objects.Grid;
import com.example.air_hockey.objects.Mallet;
import com.example.air_hockey.objects.SimplePiece;
import com.example.air_hockey.objects.Table;
import com.example.air_hockey.programs.ColorShaderProgram;
import com.example.air_hockey.programs.TextureShaderProgram;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import util.Geometry;
import util.MatrixHelper;
import util.TextureHelper;

public class Air_Hockey_Renderer implements GLSurfaceView.Renderer {
    private static final String TAG = "AirHockeyRenderer";

    private final Context context;
    private final float[] projectionMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] viewProjectionMatrix = new float[16];
    private final float[] modelViewProjectionMatrix = new float[16];
    private final float[] invertedViewProjectionMatrix = new float[16];
    private Table table;
    private Mallet mallet;
    private TextureShaderProgram textureProgram;
    private ColorShaderProgram colorProgram;
    private int texture;
    private final boolean malletPressed = false;
    private Geometry.Point blueMalletPosition;
    private final SimplePiece[] board;
    private final GameBoard gameBoard;
    private int score = 0;
    private boolean showed = false;


    private SimplePiece simplePiece;
    private Grid grid;

    private float x;
    private int numLines;
    private boolean numLinesChanged;
    private int indexToMove = -1;

    private final Air_HockeyActivity theAirHockeyActivity;

    private boolean moving = false;
    private Queue<Integer> movement;


    public Air_Hockey_Renderer(Context context, int numLines, Air_HockeyActivity air_hockeyActivity) {
        this.context = context;
        this.gameBoard = new GameBoard(numLines);
        this.numLines = numLines;
        board = new SimplePiece[numLines * numLines];
        theAirHockeyActivity = air_hockeyActivity;

    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {


        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        table = new Table();
        mallet = new Mallet(0.08f, 0.15f, 32);
        simplePiece = new SimplePiece(0.06f, 0.02f, 32, new Geometry.Point(0f, 0f, 0f));
        textureProgram = new TextureShaderProgram(context);
        colorProgram = new ColorShaderProgram(context);
        blueMalletPosition = new Geometry.Point(0f, mallet.height / 2f, 0.4f);
        grid = new Grid(numLines);
        fillBoardInitial();

        x = 0;
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        glViewport(0, 0, width, height);
        MatrixHelper.perspectiveM(projectionMatrix, 57, (float) width
                / (float) height, 1f, 10f);
        setLookAtM(viewMatrix, 0, 0f, 1.2f, 2.2f, 0f, 0f, 0f, 0f, 1f, 0f);

    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        testAll("ondrawFrame");
        if (numLinesChanged) {
            grid = new Grid(numLines);
            numLinesChanged = false;
        }
        Log.d(TAG, "onDrawFrame: gameOver"+gameBoard.gameOver());
        if (gameBoard.gameOver() && !showed) {
            moving = true;
            showed = true;
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    theAirHockeyActivity.showGameOver();
                }
            });
        }


        // Clear the rendering surface.
        glClear(GL_COLOR_BUFFER_BIT);
        // Draw the table.
        multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        invertM(invertedViewProjectionMatrix, 0, viewProjectionMatrix, 0);

        glLineWidth(2);
        positionTableInScene();
        colorProgram.setUniforms(modelViewProjectionMatrix, grid.getColor().getR(), grid.getColor().getG(), grid.getColor().getB());
        grid.bindData(colorProgram);
        grid.draw();


        for (int i = 0; i < numLines * numLines; i++) {
            if (gameBoard.filled(i)) {
                //Log.d(TAG, "onDrawFrame: try to get : "+i);
                SimplePiece lapiece = board[i];
                //Log.d(TAG, "onDrawFrame: "+lapiece);
                if (lapiece != null) {
                    Geometry.Point point = lapiece.getPoint();
                    positionObjectInScene(point.x, point.y + 0.02f, point.z);
                    colorProgram.useProgram();
                    colorProgram.setUniforms(modelViewProjectionMatrix, lapiece.getR(), lapiece.getG(), lapiece.getB());
                    //colorProgram.setUniforms(modelViewProjectionMatrix, 1f, 0.5f, 0.3f);

                    lapiece.bindData(colorProgram);
                    lapiece.draw();
                } else {
                    Log.d(TAG, "onDrawFrame: null pointer ?");
                }
            }
        }


        //setLookAtM(viewMatrix, 0, 0f, 2.2f-(x++%2500/250), 0f-(x++%2500/250), 0f, 0f, 0f, 0f, 0f, -1f);
        setLookAtM(viewMatrix, 0, 0f, 2.2f, 0f, 0f, 0f, 0f, 0f, 0f, -1f);


    }

    private void positionTableInScene() {
// The table is defined in terms of X & Y coordinates, so we rotate it // 90 degrees to lie flat on the XZ plane.
        setIdentityM(modelMatrix, 0);
        rotateM(modelMatrix, 0, -90f, 1f, 0f, 0f);
        multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0);
    }

    private void positionObjectInScene(float x, float y, float z) {
        setIdentityM(modelMatrix, 0);
        translateM(modelMatrix, 0, x, y, z);
        multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix,
                0, modelMatrix, 0);
    }

    public void handleTouchPress(float normalizedX, float normalizedY) {
        testAll("handleTouch");
        if (moving) {
            return;
        }
        Geometry.Ray ray = convertNormalized2DPointToRay(normalizedX, normalizedY);
// Now test if this ray intersects with the mallet by creating a // bounding sphere that wraps the mallet.
        for (int i = 0; i < numLines * numLines; i++) {
            SimplePiece piece = board[i];
            //Log.d(TAG, "handleTouchPress: test sur "+i+" "+piece);
            if (piece != null) {

                Geometry.Sphere malletBoundingSphere = new Geometry.Sphere(piece.getPoint(),
                        piece.radius);
// If the ray intersects (if the user touched a part of the screen that // intersects the mallet's bounding sphere), then set malletPressed = // true.
                if (Geometry.intersects(malletBoundingSphere, ray)) {
                    //Log.d(TAG, "handleTouchPress: on touche une piece "+i);
                    //piece.changeColor();
                    indexToMove = i;
                    break;
                }
            } else {
                Geometry.Point touchedPoint = gameBoard.pointFromIndex(i);
                Geometry.Sphere malletBoundingSphere = new Geometry.Sphere(touchedPoint,
                        1f / (numLines * 2));
                if (Geometry.intersects(malletBoundingSphere, ray) && indexToMove != -1) {
                    List<Integer> path = gameBoard.movementAllowed(indexToMove, i);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Log.d(TAG, "handleTouchPress path : " + path.stream().map(String::valueOf).collect(Collectors.joining(",")));
                    }
                    if (path.size() > 1) {
                        movement = new LinkedList<>();
                        movement.addAll(path);
                        movement.poll();
                        proceedMovement();
                        moving = true;
                        break;
                    } else {
                        Log.d(TAG, "handleTouchPress: FORBIDEN MOVE");
                        grid.turnRed();

                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                theAirHockeyActivity.angryVibration();
                            }
                        });
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                grid.turnWhite();
                            }
                        }, 300);
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                grid.turnRed();
                            }
                        }, 600);
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                grid.turnWhite();
                            }
                        }, 900);
                    }


                }

            }
        }

    }

    private void proceedMovement() {
        testAll("proceedMovement");
        if (!movement.isEmpty()) {
            int i = movement.poll();
            Log.d(TAG, "proceedMovement: from " + indexToMove + " to " + i);
            SimplePiece pieceToMove = board[indexToMove];
            board[i] = pieceToMove;
            pieceToMove.setPoint(gameBoard.pointFromIndex(i));
            board[indexToMove] = null;
            gameBoard.moveFromTo(indexToMove, i);
            indexToMove = i;
            //Log.d(TAG, "proceedMovement: "+Arrays.toString(board));
            //Log.d(TAG, "proceedMovement: "+ Arrays.toString(gameBoard.getFilledPosition()));


            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    proceedMovement();
                }
            }, 125);

        } else {
            checkResults();
            printBoard();
            printGameBoard();
        }
    }

    private void checkResults() {
        testAll("checkresult");
        createNewPiece();
        List<Integer> array = gameBoard.checkBoard(board);
        for (Integer indexToDestroy :
                array) {
            board[indexToDestroy] = null;

        }
        score = array.size();
        moving = false;
        indexToMove = -1;

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                theAirHockeyActivity.addScore(score);
            }
        });

    }

    private void createNewPiece() {
        testAll("createNewPiece");
        int index = gameBoard.getRandomPosition();
        Geometry.Point point = gameBoard.pointFromIndex(index);
        SimplePiece piece = new SimplePiece(1f / ((numLines + 1) * 2), 0.04f, (int) (Math.random() * (10 - 3 + 1) + 3), point);
        board[index] = piece;
    }

    public void handleTouchDrag(float normalizedX, float normalizedY) {
        /*if (!moving && malletPressed) {
            Geometry.Ray ray = convertNormalized2DPointToRay(normalizedX, normalizedY);
// Define a plane representing our air hockey table.
            Geometry.Plane plane = new Geometry.Plane(new Geometry.Point(0, 0, 0), new Geometry.Vector(0, 1, 0)); // Find out where the touched point intersects the plane
// representing our table. We'll move the mallet along this plane.
            Geometry.Point touchedPoint = Geometry.intersectionPoint(ray, plane);
            blueMalletPosition =
            new Geometry.Point(touchedPoint.x, mallet.height / 2f, touchedPoint.z);
        }*/
    }


    private Geometry.Ray convertNormalized2DPointToRay(
            float normalizedX, float normalizedY) {
// We'll convert these normalized device coordinates into world-space
// coordinates. We'll pick a point on the near and far planes, and draw a
// line between them. To do this transform, we need to first multiply by
// the inverse matrix, and then we need to undo the perspective divide.
        final float[] nearPointNdc = {normalizedX, normalizedY, -1, 1};
        final float[] farPointNdc = {normalizedX, normalizedY, 1, 1};
        final float[] nearPointWorld = new float[4];
        final float[] farPointWorld = new float[4];
        multiplyMV(
                nearPointWorld, 0, invertedViewProjectionMatrix, 0, nearPointNdc, 0);
        multiplyMV(
                farPointWorld, 0, invertedViewProjectionMatrix, 0, farPointNdc, 0);
        divideByW(nearPointWorld);
        divideByW(farPointWorld);
        Geometry.Point nearPointRay =
                new Geometry.Point(nearPointWorld[0], nearPointWorld[1], nearPointWorld[2]);
        Geometry.Point farPointRay =
                new Geometry.Point(farPointWorld[0], farPointWorld[1], farPointWorld[2]);
        return new Geometry.Ray(nearPointRay,
                Geometry.vectorBetween(nearPointRay, farPointRay));
    }

    private void divideByW(float[] vector) {
        vector[0] /= vector[3];
        vector[1] /= vector[3];
        vector[2] /= vector[3];
    }


    public void setNumLines(int numLines) {
        //Log.d(TAG, "setNumLines: "+numLines);
        this.numLines = numLines;
        this.numLinesChanged = true;
    }

    public void fillBoardInitial() {
        testAll("fillBoardInitial");
        float step = (float) (1.0 / numLines);

        for (int i = 0; i < numLines - 2; i++) {
            int index = gameBoard.getRandomPosition();
            Geometry.Point point = gameBoard.pointFromIndex(index);
            //Log.d(TAG, "fillBoardInitial: "+point.x+" "+point.y+" "+point.z);
            SimplePiece piece = new SimplePiece(1f / ((numLines + 1) * 2), 0.04f, (int) (Math.random() * (10 - 3 + 1) + 3), point);
            board[index] = piece;
            //Log.d(TAG, "fillBoardInitial: filled index : "+index);
        }
    }

    private void printBoard() {
        Log.d(TAG, "printBoard: ///////////////////////////////");
        for (int i = 0; i < numLines; i++) {
            StringBuilder line = new StringBuilder();
            for (int j = 0; j < numLines; j++) {
                if (board[i * numLines + j] != null)
                    line.append(board[i * numLines + j].toString() + "\t");
                else
                    line.append(" null\t");

            }
            Log.d(TAG, "printBoard: " + line);
        }
    }

    private void printGameBoard() {
        Log.d(TAG, "printBoard: ///////////////////////////////");
        for (int i = 0; i < numLines; i++) {
            StringBuilder line = new StringBuilder();
            for (int j = 0; j < numLines; j++) {
                line.append(gameBoard.getFilledPosition()[i * numLines + j] + "\t");
            }
            Log.d(TAG, "printBoard: " + line);
        }
        Log.d(TAG, "printBoard: ///////////////////////////////");
        for (int i = 0; i < numLines; i++) {
            StringBuilder line = new StringBuilder();
            for (int j = 0; j < numLines; j++) {
                line.append((i * numLines + j) + "\t");
            }
            Log.d(TAG, "printBoard: " + line);
        }
    }

    public void testAll(String where) {
        boolean ok = true;
        Log.d(TAG, "$$$$$$$$$$$$ " + where);
        for (int i = 0; i < this.numLines; i++) {
            //Log.d(TAG, "testAll: i:"+i);
            for (int j = 0; j < this.numLines; j++) {
                //Log.d(TAG, "testAll: j:"+j);
                if (gameBoard.getFilledPosition()[i * numLines + j]) {
                    if (board[i * numLines + j] == null) {
                        Log.d(TAG, "testAll: pas ok pour " + (i * numLines + j));
                        //throw new RuntimeException("pas ok "+(i * numLines + j));
                    }
                } else {
                    if (board[i * numLines + j] != null) {
                        Log.d(TAG, "testAll: pas ko pour " + (i * numLines + j));
                        //throw new RuntimeException("pas ko "+(i * numLines + j));
                    }
                }
            }
        }
    }
}
