package com.example.klines;

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
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.klines.objects.GameBoard;
import com.example.klines.objects.Grid;
import com.example.klines.objects.GridNextPieces;
import com.example.klines.objects.SimplePiece;
import com.example.klines.programs.ColorShaderProgram;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import util.Geometry;
import util.MatrixHelper;

public class Klines_Renderer implements GLSurfaceView.Renderer {
    private static final String TAG = "AirHockeyRenderer";

    private final Context context;
    private final float[] projectionMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] viewProjectionMatrix = new float[16];
    private final float[] modelViewProjectionMatrix = new float[16];
    private final float[] invertedViewProjectionMatrix = new float[16];
    private GridNextPieces gridNextPieces;
    private ColorShaderProgram colorProgram;
    private final SimplePiece[] board;
    private final GameBoard gameBoard;
    private int score = 0;
    //used to do nothing at the end of the game and not display multiple times the dialogue box
    private boolean showed = false;


    private Grid grid;

    //used for moving the camera
    private float x;

    //number of lines for the grid
    private int numLines;

    //index of the piece that we touched and that we want to move
    private int indexToMove = -1;

    private final KlinesActivity theAirHockeyActivity;

    //when a piece is moving this help us to forbid any other touch action, just wait and watch it move
    private boolean moving = false;
    //the indexes that the moving piece is gonna have to pass by during its journey
    private Queue<Integer> movement;

    //boolean tested for when we change the screen orientation, not successful :(
    private boolean alreadyFiled = false;
    //the array holding the pieces that we will places during the next round
    SimplePiece[] nextPieces;


    public Klines_Renderer(Context context, int numLines, KlinesActivity klinesActivity) {
        this.context = context;
        this.gameBoard = new GameBoard(numLines);
        this.numLines = numLines;
        board = new SimplePiece[numLines * numLines];
        theAirHockeyActivity = klinesActivity;
        nextPieces = new SimplePiece[3];
        x=0;

    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        Log.d(TAG, "onSurfaceCreated: "+x);

        //attempt to not redo everything when screen is rotated
        if(gridNextPieces==null) {
            Log.d(TAG, "onSurfaceCreated: ON REFAIT TOUT");
            glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            colorProgram = new ColorShaderProgram(context);
            grid = new Grid(numLines);
            gridNextPieces = new GridNextPieces();
            fillBoardInitial();
        }
        for (int i = 0; i < 3; i++) {
            nextPieces[i] = new SimplePiece(1f / ((numLines + 1) * 2), 0.04f, (int) (Math.random() * (10 - 3 + 1) + 3), new Geometry.Point(0f,0f,0f));
        }

        x = 0;
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        glViewport(0, 0, width, height);
        MatrixHelper.perspectiveM(projectionMatrix, 50, (float) width
                / (float) height, 1f, 10f);
        setLookAtM(viewMatrix, 0, 0f, 1.2f, 2.2f, 0f, 0f, 0f, 0f, 1f, 0f);

    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        //testAll("ondrawFrame");

        //Log.d(TAG, "onDrawFrame: gameOver"+gameBoard.gameOver());
        if (gameBoard.gameOver() && !showed) {
            moving = true;
            showed = true;
            new Handler(Looper.getMainLooper()).post(theAirHockeyActivity::showGameOver);
        }



        glClear(GL_COLOR_BUFFER_BIT);
        // Draw the table.
        multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        invertM(invertedViewProjectionMatrix, 0, viewProjectionMatrix, 0);

        glLineWidth(2);
        positionGridInScene();
        colorProgram.setUniforms(modelViewProjectionMatrix, grid.getColor().getR(), grid.getColor().getG(), grid.getColor().getB());
        grid.bindData(colorProgram);
        grid.draw();

        gridNextPieces.bindData(colorProgram);
        gridNextPieces.draw();

        //draw all the pieces
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
                    lapiece.bindData(colorProgram);
                    lapiece.draw();
                } else {
                    Log.d(TAG, "onDrawFrame: null pointer !?");
                    //this is where the main issue was with the null pointer, even though it wasn't supposed to happen
                    //probably due to the interruption of the thread execution by the onDraw.
                }
            }
        }
        //draw the 3 next pieces
        for (int i = 0; i < 3; i++) {
            SimplePiece lapiece = nextPieces[i];
            //Log.d(TAG, "onDrawFrame: "+lapiece);
            if (lapiece != null) {
                //Log.d(TAG, "onDrawFrame: "+gridNextPieces.getX()+" "+gridNextPieces.getY());
                positionObjectInScene(gridNextPieces.getX()+(i+0.5f)*gridNextPieces.getStep(), 0.02f,-gridNextPieces.getY()+0.5f*gridNextPieces.getStep());
                colorProgram.useProgram();
                colorProgram.setUniforms(modelViewProjectionMatrix, lapiece.getR(), lapiece.getG(), lapiece.getB());
                lapiece.bindData(colorProgram);
                lapiece.draw();
            }
        }

        x++;
        //Uncomment for weird camera movement (but funny), used to see where the pieces were.
        //setLookAtM(viewMatrix, 0, 0f, 2.2f-(x++%1500/250), 0f-(x++%2500/250), 0f, 0f, 0f, 0f, 0f, -1f);
        setLookAtM(viewMatrix, 0, 0f, 2.2f, 0f, 0f, 0f, 0f, 0f, 0f, -1f);


    }

    private void positionGridInScene() {
// The grid is defined in terms of X & Y coordinates, so we rotate it // 90 degrees to lie flat on the XZ plane.
        setIdentityM(modelMatrix, 0);
        rotateM(modelMatrix, 0, -90f, 1f, 0f, 0f);
        multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0);
    }

    private void positionObjectInScene(float x, float y, float z) {
        //Used to position objects in the scene
        setIdentityM(modelMatrix, 0);
        translateM(modelMatrix, 0, x, y, z);
        multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix,
                0, modelMatrix, 0);
    }

    public void handleTouchPress(float normalizedX, float normalizedY) {
        //testAll("handleTouch");
        if (moving) {
            return;
        }
        Geometry.Ray ray = convertNormalized2DPointToRay(normalizedX, normalizedY);
// Now test if this ray intersects with the mallet by creating a
// bounding sphere that wraps the piece.
        for (int i = 0; i < numLines * numLines; i++) {
            SimplePiece piece = board[i];
            //Log.d(TAG, "handleTouchPress: test sur "+i+" "+piece);
            if (piece != null) {
                //we touch a piece

                Geometry.Sphere malletBoundingSphere = new Geometry.Sphere(piece.getPoint(),
                        piece.radius);
                if (Geometry.intersects(malletBoundingSphere, ray)) {
                    //Log.d(TAG, "handleTouchPress: on touche une piece "+i);
                    indexToMove = i;
                    break;
                }
            } else {
                // maybe we touch an empty case of the canvas ?
                Geometry.Point touchedPoint = gameBoard.pointFromIndex(i);
                Geometry.Sphere malletBoundingSphere = new Geometry.Sphere(touchedPoint,
                        1f / (numLines * 2));
                if (Geometry.intersects(malletBoundingSphere, ray) && indexToMove != -1) {
                    //test if the movement is allowed, BFS
                    List<Integer> path = gameBoard.movementAllowed(indexToMove, i);
                    /*
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Log.d(TAG, "handleTouchPress path : " + path.stream().map(String::valueOf).collect(Collectors.joining(",")));
                    }
                    */
                    if (path.size() > 1) {
                        //It's allowed, we have path !
                        movement = new LinkedList<>();
                        movement.addAll(path);
                        movement.poll();
                        proceedMovement();
                        moving = true;
                        break;
                    } else {
                        Log.d(TAG, "handleTouchPress: FORBIDEN MOVE");
                        grid.turnRed();
                        //to communicate with the activity we use a runnable to be launched in the main thread
                        new Handler(Looper.getMainLooper()).post(theAirHockeyActivity::angryVibration);
                        //blinking effect
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
    //sort of recursive method that will proceed to do one movement then schedule the new call for
    // movement a few milliseconds later if there is still a movement to do
    private void proceedMovement() {
        testAll("proceedMovement");
        if (!movement.isEmpty()) {
            int i = movement.poll();
            Log.d(TAG, "proceedMovement: from " + indexToMove + " to " + i);
            //we move the piece
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
            //then at the end we check the result
            checkResults();
            printBoard();
            printGameBoard();
        }
    }

    /*
    Checks the result and creates the 3 new pieces
     */
    private void checkResults() {

        testAll("checkresult");
        placeNewPiece();
        //return the indexes of the piece that we need to destroy
        //it is possible that the new pieces randomly complete a line
        List<Integer> array = gameBoard.checkBoard(board);
        for (Integer indexToDestroy :
                array) {
            board[indexToDestroy] = null;

        }
        score = array.size();
        moving = false;
        indexToMove = -1;

        new Handler(Looper.getMainLooper()).post(() -> theAirHockeyActivity.addScore(score));

    }

    private void placeNewPiece() {
        testAll("createNewPiece");
        if(!gameBoard.gameOver()){
            for (int i = 0; i < 3; i++) {
                if(!gameBoard.gameOver()) {
                    SimplePiece lapiece = nextPieces[i];
                    //Log.d(TAG, "onDrawFrame: "+lapiece);
                    if (lapiece != null) {
                        //we look for a random position
                        int index = gameBoard.getRandomPosition();
                        Geometry.Point point = gameBoard.pointFromIndex(index);
                        //we place it
                        lapiece.setPoint(point);
                        board[index] = lapiece;
                    } else {
                        Log.d(TAG, "onDrawFrame: null pointer ?");
                    }
                    //we create a new random piece for the nextGrid
                    nextPieces[i] = new SimplePiece(1f / ((numLines + 1) * 2), 0.04f, (int) (Math.random() * (10 - 3 + 1) + 3), new Geometry.Point(0f, 0f, 0f));
                }
            }

        }
    }


    private Geometry.Ray convertNormalized2DPointToRay(
            float normalizedX, float normalizedY) {
        //Method inspired from the book "OpenGL ES 2 for android" by Kevin Brothaler
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
        //Class inspired from the book "OpenGL ES 2 for android" by Kevin Brothaler
        vector[0] /= vector[3];
        vector[1] /= vector[3];
        vector[2] /= vector[3];
    }

/*
Initial method used to create the first pieces of the board
 */
    public void fillBoardInitial() {
        if(!alreadyFiled) {
            alreadyFiled=true;
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
    }
//used for debug
    private void printBoard() {
        Log.d(TAG, "printBoard: ///////////////////////////////");
        for (int i = 0; i < numLines; i++) {
            StringBuilder line = new StringBuilder();
            for (int j = 0; j < numLines; j++) {
                if (board[i * numLines + j] != null)
                    line.append(board[i * numLines + j].toString()).append("\t");
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
                line.append(gameBoard.getFilledPosition()[i * numLines + j]).append("\t");
            }
            Log.d(TAG, "printBoard: " + line);
        }
        Log.d(TAG, "printBoard: ///////////////////////////////");
        for (int i = 0; i < numLines; i++) {
            StringBuilder line = new StringBuilder();
            for (int j = 0; j < numLines; j++) {
                line.append(i * numLines + j).append("\t");
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
