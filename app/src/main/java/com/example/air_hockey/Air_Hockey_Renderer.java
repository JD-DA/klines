package com.example.air_hockey;

import static android.opengl.GLES10.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES10.glClear;
import static android.opengl.GLES10.glClearColor;
import static android.opengl.GLES10.glViewport;
import static android.opengl.GLES20.glLineWidth;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.Matrix.invertM;
import static android.opengl.Matrix.multiplyMM;
import static android.opengl.Matrix.multiplyMV;
import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.setIdentityM;
import static android.opengl.Matrix.setLookAtM;
import static android.opengl.Matrix.translateM;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.example.air_hockey.data.VertexArray;
import com.example.air_hockey.objects.GameBoard;
import com.example.air_hockey.objects.Grid;
import com.example.air_hockey.objects.Mallet;
import com.example.air_hockey.objects.SimplePiece;
import com.example.air_hockey.objects.Table;
import com.example.air_hockey.programs.ColorShaderProgram;
import com.example.air_hockey.programs.TextureShaderProgram;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

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
    private Table table;
    private Mallet mallet;
    private TextureShaderProgram textureProgram;
    private ColorShaderProgram colorProgram;
    private int texture;

    private final float[] viewMatrix = new float[16];
    private final float[] viewProjectionMatrix = new float[16];
    private final float[] modelViewProjectionMatrix = new float[16];

    private boolean malletPressed = false;
    private Geometry.Point blueMalletPosition;


    private final float[] invertedViewProjectionMatrix = new float[16];

    private SimplePiece[] board;
    private GameBoard gameBoard;
    private int score = 0;




    private SimplePiece simplePiece;
    private Grid grid;

    private float x;
    private int numLines;
    private boolean numLinesChanged;
    private int indexToMove = -1;


    public Air_Hockey_Renderer(Context context,int numLines) {
        this.context = context;
        this.gameBoard = new GameBoard(numLines);
        this.numLines=numLines;
        board = new SimplePiece[numLines*numLines];

    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {


        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        table = new Table();
        mallet = new Mallet(0.08f, 0.15f, 32);
        simplePiece = new SimplePiece(0.06f, 0.02f, 32,new Geometry.Point(0f,0f,0f));
        textureProgram = new TextureShaderProgram(context);
        colorProgram = new ColorShaderProgram(context);
        texture = TextureHelper.loadTexture(context, R.drawable.air_hockey_surface);
        blueMalletPosition = new Geometry.Point(0f, mallet.height / 2f, 0.4f);
        grid = new Grid(numLines);
        fillBoardInitial();

        x=0;
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        glViewport(0,0,width,height);
        MatrixHelper.perspectiveM(projectionMatrix, 45, (float) width
                / (float) height, 1f, 10f);
        setLookAtM(viewMatrix, 0, 0f, 1.2f, 2.2f, 0f, 0f, 0f, 0f, 1f, 0f);

    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        if (numLinesChanged) {
            grid = new Grid(numLines);
            numLinesChanged = false;
        }


        // Clear the rendering surface.
        glClear(GL_COLOR_BUFFER_BIT);
        // Draw the table.
        multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        invertM(invertedViewProjectionMatrix, 0, viewProjectionMatrix, 0);

        glLineWidth(2);
        positionTableInScene();
        colorProgram.setUniforms(modelViewProjectionMatrix, 1f, 1f, 0f);
        grid.bindData(colorProgram);
        grid.draw();


        for (int i = 0; i < numLines * numLines; i++) {
            if(gameBoard.filled(i)){
                //Log.d(TAG, "onDrawFrame: try to get : "+i);
                SimplePiece lapiece = board[i];
                //Log.d(TAG, "onDrawFrame: "+lapiece);
                Geometry.Point point = lapiece.getPoint();
                positionObjectInScene(point.x, point.y+0.02f, point.z);
                colorProgram.useProgram();
                colorProgram.setUniforms(modelViewProjectionMatrix, lapiece.getR(), lapiece.getG(), lapiece.getB());
                //colorProgram.setUniforms(modelViewProjectionMatrix, 1f, 0.5f, 0.3f);

                lapiece.bindData(colorProgram);
                lapiece.draw();
            }
        }




        //setLookAtM(viewMatrix, 0, 0f, 2.2f-(x++%2500/250), 0f-(x++%2500/250), 0f, 0f, 0f, 0f, 0f, -1f);
        setLookAtM(viewMatrix, 0, 0f, 2.2f, 0f, 0f, 0f, 0f, 0f, 0f, -1f);



    }

    private void positionTableInScene() {
// The table is defined in terms of X & Y coordinates, so we rotate it // 90 degrees to lie flat on the XZ plane.
        setIdentityM(modelMatrix, 0);
        rotateM(modelMatrix, 0, -90f, 1f, 0f, 0f);
        multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix,0,modelMatrix,0);
    }
    private void positionObjectInScene(float x, float y, float z) {
        setIdentityM(modelMatrix, 0);
        translateM(modelMatrix, 0, x, y, z);
        multiplyMM(modelViewProjectionMatrix, 0, viewProjectionMatrix,
                0, modelMatrix, 0);
    }

    public void handleTouchPress(float normalizedX, float normalizedY) {
        Geometry.Ray ray = convertNormalized2DPointToRay(normalizedX, normalizedY);
// Now test if this ray intersects with the mallet by creating a // bounding sphere that wraps the mallet.
        for (int i = 0;i<numLines*numLines;i++) {
            SimplePiece piece = board[i];
            Log.d(TAG, "handleTouchPress: test sur "+i+" "+piece);
            if(piece != null) {

                Geometry.Sphere malletBoundingSphere = new Geometry.Sphere(piece.getPoint(),
                        piece.radius);
// If the ray intersects (if the user touched a part of the screen that // intersects the mallet's bounding sphere), then set malletPressed = // true.
                if (Geometry.intersects(malletBoundingSphere, ray)) {
                    Log.d(TAG, "handleTouchPress: on touche une piece "+i);
                    //piece.changeColor();
                    indexToMove=i;
                    break;
                }
            }else{
                Geometry.Point touchedPoint = gameBoard.pointFromIndex(i);
                Geometry.Sphere malletBoundingSphere = new Geometry.Sphere(touchedPoint,
                        1f/(numLines*2));
                if (Geometry.intersects(malletBoundingSphere, ray) && indexToMove!=-1) {
                    Log.d(TAG, "handleTouchPress: on touche du vide "+i);
                    SimplePiece pieceToMove = board[indexToMove];
                    board[i]=pieceToMove;
                    pieceToMove.setPoint(touchedPoint);
                    board[indexToMove]=null;
                    gameBoard.moveFromTo(indexToMove,i);
                    indexToMove = -1;
                    createNewPiece();
                    List<Integer> array = gameBoard.checkBoard(board);
                    for (Integer indexToDestroy :
                            array) {
                        board[indexToDestroy]=null;
                        score += array.size();
                    }

                }

            }
        }

    }

    private void createNewPiece() {
        int index = gameBoard.getRandomPosition();
        Geometry.Point point =  gameBoard.pointFromIndex(index);
        SimplePiece piece = new SimplePiece(1f/((numLines+1)*2), 0.04f, (int)(Math.random()*(10-3+1)+3),point);
        board[index]=piece;
    }

    public void handleTouchDrag(float normalizedX, float normalizedY) {
        if (malletPressed) {
            Geometry.Ray ray = convertNormalized2DPointToRay(normalizedX, normalizedY);
// Define a plane representing our air hockey table.
            Geometry.Plane plane = new Geometry.Plane(new Geometry.Point(0, 0, 0), new Geometry.Vector(0, 1, 0)); // Find out where the touched point intersects the plane
// representing our table. We'll move the mallet along this plane.
            Geometry.Point touchedPoint = Geometry.intersectionPoint(ray, plane);
            blueMalletPosition =
            new Geometry.Point(touchedPoint.x, mallet.height / 2f, touchedPoint.z);
        }
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

        private void divideByW(float[] vector) { vector[0] /= vector[3];
            vector[1] /= vector[3];
            vector[2] /= vector[3];
        }


    public void setNumLines(int numLines) {
        Log.d(TAG, "setNumLines: "+numLines);
        this.numLines = numLines;
        this.numLinesChanged = true;
    }

    public void fillBoardInitial(){
        float step = (float) (1.0/numLines);

        for (int i = 0; i < numLines - 2; i++) {
            int index = gameBoard.getRandomPosition();
            Geometry.Point point =  gameBoard.pointFromIndex(index);
            Log.d(TAG, "fillBoardInitial: "+point.x+" "+point.y+" "+point.z);
            SimplePiece piece = new SimplePiece(1f/((numLines+1)*2), 0.04f, (int)(Math.random()*(10-3+1)+3),point);
            board[index]=piece;
            Log.d(TAG, "fillBoardInitial: filled index : "+index);
        }
    }
}
