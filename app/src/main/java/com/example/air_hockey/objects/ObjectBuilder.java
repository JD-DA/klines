package com.example.air_hockey.objects;


import static android.opengl.GLES10.glDrawArrays;
import static javax.microedition.khronos.opengles.GL10.GL_LINES;
import static javax.microedition.khronos.opengles.GL10.GL_TRIANGLE_FAN;
import static javax.microedition.khronos.opengles.GL10.GL_TRIANGLE_STRIP;


import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import util.Geometry.Circle;
import util.Geometry.*;


class ObjectBuilder {
    private static final int FLOATS_PER_VERTEX = 3;



    static interface DrawCommand {
        void draw();
    }

    static class GeneratedData {
        final float[] vertexData;
        final List<DrawCommand> drawList;

        GeneratedData(float[] vertexData, List<DrawCommand> drawList) {
            this.vertexData = vertexData;
            this.drawList = drawList;
        }
    }

    static GeneratedData createPiece(Cylinder piece, int numPoints) {
        int size = sizeOfCircleInVertices(numPoints)
                 + sizeOfOpenCylinderInVertices(numPoints);
        
        ObjectBuilder builder = new ObjectBuilder(size);

        Circle puckTop = new Circle(
            piece.center.translateY(piece.height / 2f),
            piece.radius);
        
        builder.appendCircle(puckTop, numPoints);
        builder.appendOpenCylinder(piece, numPoints);

        return builder.build();
    }
    
    static GeneratedData createMallet(
        Point center, float radius, float height, int numPoints) {
        int size = sizeOfCircleInVertices(numPoints) * 2
                 + sizeOfOpenCylinderInVertices(numPoints) * 2;
        
        ObjectBuilder builder = new ObjectBuilder(size);                                      
        
        // First, generate the mallet base.
        float baseHeight = height * 0.25f;
        
        Circle baseCircle = new Circle(
            center.translateY(-baseHeight), 
            radius);
        Cylinder baseCylinder = new Cylinder(
            baseCircle.center.translateY(-baseHeight / 2f), 
            radius, baseHeight);

        builder.appendCircle(baseCircle, numPoints);
        builder.appendOpenCylinder(baseCylinder, numPoints);
                
        // Now generate the mallet handle.
        float handleHeight = height * 0.75f;
        float handleRadius = radius / 3f;
        
        Circle handleCircle = new Circle(
            center.translateY(height * 0.5f), 
            handleRadius);        
        Cylinder handleCylinder = new Cylinder(
            handleCircle.center.translateY(-handleHeight / 2f),
            handleRadius, handleHeight);                

        builder.appendCircle(handleCircle, numPoints);
        builder.appendOpenCylinder(handleCylinder, numPoints);

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

    private void appendCircle(Circle circle, int numPoints) {
        final int startVertex = offset / FLOATS_PER_VERTEX;
        final int numVertices = sizeOfCircleInVertices(numPoints);

        // Center point of fan
        vertexData[offset++] = circle.center.x;
        vertexData[offset++] = circle.center.y;
        vertexData[offset++] = circle.center.z;

        // Fan around center point. <= is used because we want to generate
        // the point at the starting angle twice to complete the fan.
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
        drawList.add(new DrawCommand() {
            @Override
            public void draw() {
                glDrawArrays(GL_TRIANGLE_FAN, startVertex, numVertices);
            }
        });
    }

    private void appendOpenCylinder(Cylinder cylinder, int numPoints) {
        final int startVertex = offset / FLOATS_PER_VERTEX;
        final int numVertices = sizeOfOpenCylinderInVertices(numPoints);
        final float yStart = cylinder.center.y - (cylinder.height / 2f);
        final float yEnd = cylinder.center.y + (cylinder.height / 2f);

        // Generate strip around center point. <= is used because we want to
        // generate the points at the starting angle twice, to complete the
        // strip.
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
        drawList.add(new DrawCommand() {
            @Override
            public void draw() {
                glDrawArrays(GL_TRIANGLE_STRIP, startVertex, numVertices);
            }
        });        
    }

    private void appendGrid(int numLines){
        final int startVertex = offset / FLOATS_PER_VERTEX;
        float startX = -0.5f;
        float startY = 0.5f;
        float step = 1.0f/numLines;
        //lignes
        for(int i=0;i<=numLines;i++){
            //Log.d("ShaderHelper", "appendGrid: "+startX+" "+(startY-i*step)+" "+(startX+1.0f)+" "+(startY-i*step));
            vertexData[offset++] =startX;
            vertexData[offset++] =startY-i*step;
            vertexData[offset++] =startX+1.0f;
            vertexData[offset++] =startY-i*step;
        }
        //colonnes
        for(int i=0;i<=numLines;i++){
            //Log.d("ShaderHelper", "appendGrid: "+startX+i*step+" "+startY+" "+startX+i*step+" "+(startY-1.0f));
            vertexData[offset++] =startX+i*step;
            vertexData[offset++] =startY;
            vertexData[offset++] =startX+i*step;
            vertexData[offset++] =startY-1.0f;
        }
        drawList.add(new DrawCommand() {
            @Override
            public void draw() {
                glDrawArrays(GL_LINES, startVertex, (numLines+1)*4);
            }
        });

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
        drawList.add(new DrawCommand() {
            @Override
            public void draw() {
                glDrawArrays(GL_LINES, startVertex, 12);
            }
        });
    }



    private GeneratedData build() {
        return new GeneratedData(vertexData, drawList);
    }
}
