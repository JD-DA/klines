package com.example.klines.objects;


import static android.opengl.GLES10.glDrawArrays;
import static javax.microedition.khronos.opengles.GL10.GL_LINES;
import static javax.microedition.khronos.opengles.GL10.GL_TRIANGLE_FAN;
import static javax.microedition.khronos.opengles.GL10.GL_TRIANGLE_STRIP;


import java.util.ArrayList;
import java.util.List;

import util.Geometry.Circle;
import util.Geometry.*;


class ObjectBuilder {
    private static final int FLOATS_PER_VERTEX = 3;



    interface DrawCommand {
        void draw();
    }

    static class GeneratedData {
        //coordinates
        final float[] vertexData;
        //list of draw actions (draw triangles, then draw lines...) for the same object related to the coordinates
        final List<DrawCommand> drawList;

        GeneratedData(float[] vertexData, List<DrawCommand> drawList) {
            this.vertexData = vertexData;
            this.drawList = drawList;
        }
    }

    /////// Object creations

    static GeneratedData createPiece(Cylinder piece, int numPoints) {
        int size = sizeOfCircleInVertices(numPoints)
                 + sizeOfOpenCylinderInVertices(numPoints);
        
        ObjectBuilder builder = new ObjectBuilder(size);

        Circle pieceTop = new Circle(
            piece.center.translateY(piece.height / 2f),
            piece.radius);
        
        builder.appendCircle(pieceTop, numPoints);
        builder.appendOpenCylinder(piece, numPoints);

        return builder.build();
    }

    static GeneratedData createGrid(int numLines){
        ObjectBuilder builder = new ObjectBuilder((numLines+1)*4);
        builder.appendGrid(numLines);
        return builder.build();

    }
    public static GeneratedData createGridNextPieces() {
        ObjectBuilder builder = new ObjectBuilder(12);
        builder.appendGridNextPieces();
        return builder.build();
    }



    private static int sizeOfCircleInVertices(int numPoints) {
        return 1 + (numPoints + 1);
    }

    private static int sizeOfOpenCylinderInVertices(int numPoints) {
        return (numPoints + 1) * 2;
    }

    private final float[] vertexData;
    private final List<DrawCommand> drawList = new ArrayList<DrawCommand>();
    private int offset = 0;

    private ObjectBuilder(int sizeInVertices) {
        vertexData = new float[sizeInVertices * FLOATS_PER_VERTEX];
    }

    //// Geometric objects creations

    private void appendCircle(Circle circle, int numPoints) {
        final int startVertex = offset / FLOATS_PER_VERTEX;
        final int numVertices = sizeOfCircleInVertices(numPoints);

        vertexData[offset++] = circle.center.x;
        vertexData[offset++] = circle.center.y;
        vertexData[offset++] = circle.center.z;


        for (int i = 0; i <= numPoints; i++) {
            float angleInRadians = 
                  ((float) i / (float) numPoints)
                * ((float) Math.PI * 2f);
            
            vertexData[offset++] =
                    (float) (circle.center.x
                                    + circle.radius * Math.cos(angleInRadians));
            vertexData[offset++] = circle.center.y;
            vertexData[offset++] =
                    (float) (circle.center.z
                                    + circle.radius * Math.sin(angleInRadians));
        }
        drawList.add(() -> glDrawArrays(GL_TRIANGLE_FAN, startVertex, numVertices));
    }

    private void appendOpenCylinder(Cylinder cylinder, int numPoints) {
        final int startVertex = offset / FLOATS_PER_VERTEX;
        final int numVertices = sizeOfOpenCylinderInVertices(numPoints);
        final float yStart = cylinder.center.y - (cylinder.height / 2f);
        final float yEnd = cylinder.center.y + (cylinder.height / 2f);

        for (int i = 0; i <= numPoints; i++) {
            float angleInRadians = 
                  ((float) i / (float) numPoints)
                * ((float) Math.PI * 2f);
            
            float xPosition =
                    (float) (cylinder.center.x
                                    + cylinder.radius * Math.cos(angleInRadians));
            
            float zPosition =
                    (float) (cylinder.center.z
                                    + cylinder.radius * Math.sin(angleInRadians));

            vertexData[offset++] = xPosition;
            vertexData[offset++] = yStart;
            vertexData[offset++] = zPosition;

            vertexData[offset++] = xPosition;
            vertexData[offset++] = yEnd;
            vertexData[offset++] = zPosition;
        }
        drawList.add(() -> glDrawArrays(GL_TRIANGLE_STRIP, startVertex, numVertices));
    }

    private void appendGrid(int numLines){
        final int startVertex = offset / FLOATS_PER_VERTEX;
        float startX = -0.5f;
        float startY = 0.5f;
        float step = 1.0f/numLines;
        //lines
        for(int i=0;i<=numLines;i++){
            //Log.d("ShaderHelper", "appendGrid: "+startX+" "+(startY-i*step)+" "+(startX+1.0f)+" "+(startY-i*step));
            vertexData[offset++] =startX;
            vertexData[offset++] =startY-i*step;
            vertexData[offset++] =startX+1.0f;
            vertexData[offset++] =startY-i*step;
        }
        //colones
        for(int i=0;i<=numLines;i++){
            //Log.d("ShaderHelper", "appendGrid: "+startX+i*step+" "+startY+" "+startX+i*step+" "+(startY-1.0f));
            vertexData[offset++] =startX+i*step;
            vertexData[offset++] =startY;
            vertexData[offset++] =startX+i*step;
            vertexData[offset++] =startY-1.0f;
        }
        drawList.add(() -> glDrawArrays(GL_LINES, startVertex, (numLines+1)*4));

    }
    private void appendGridNextPieces() {
        final int startVertex = offset / FLOATS_PER_VERTEX;
        float step = 1.0f/7;
        float startX = -(step*1.5f);
        float startY = 0.5f+step*1.5f;

        //lignes
        for(int i=0;i<=1;i++){
            //Log.d("ShaderHelper", "appendGrid: "+startX+" "+(startY-i*step)+" "+(startX+1.0f)+" "+(startY-i*step));
            vertexData[offset++] =startX;
            vertexData[offset++] =startY-i*step;
            vertexData[offset++] =startX+step*3;
            vertexData[offset++] =startY-i*step;
        }
        //colonnes
        for(int i=0;i<=3;i++){
            //Log.d("ShaderHelper", "appendGrid: "+startX+i*step+" "+startY+" "+startX+i*step+" "+(startY-1.0f));
            vertexData[offset++] =startX+i*step;
            vertexData[offset++] =startY;
            vertexData[offset++] =startX+i*step;
            vertexData[offset++] =startY-step;
        }
        drawList.add(() -> glDrawArrays(GL_LINES, startVertex, 12));
    }



    private GeneratedData build() {
        return new GeneratedData(vertexData, drawList);
    }
}
