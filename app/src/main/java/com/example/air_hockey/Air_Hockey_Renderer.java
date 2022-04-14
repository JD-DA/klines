package com.example.air_hockey;

import static android.opengl.GLES10.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES10.GL_FLAT;
import static android.opengl.GLES10.GL_FLOAT;
import static android.opengl.GLES10.GL_TRIANGLE_FAN;
import static android.opengl.GLES10.glClear;
import static android.opengl.GLES10.glClearColor;
import static android.opengl.GLES10.glDrawArrays;
import static android.opengl.GLES10.glVertexPointer;
import static android.opengl.GLES10.glViewport;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform4f;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;

import static javax.microedition.khronos.opengles.GL10.GL_LINES;
import static javax.microedition.khronos.opengles.GL10.GL_POINTS;
import static javax.microedition.khronos.opengles.GL10.GL_TRIANGLES;

import android.content.Context;
import android.graphics.Shader;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import util.LoggerConfig;
import util.ShaderHelper;
import util.TextResourceReader;

public class Air_Hockey_Renderer implements GLSurfaceView.Renderer {
    private static final int POSITION_COMPONENT_COUNT =2;
    private static final int BYTES_PER_FLOAT = 4;
    private final FloatBuffer vertexData;
    private final Context context;
    private int program;


    private static final String A_POSITION = "a_Position";
    private int aPositionLocation;

    private static final String A_COLOR = "a_Color";
    private static final int COLOR_COMPONENT_COUNT = 3;
    private static final int STRIDE =
            (POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT) * BYTES_PER_FLOAT;
    private int aColorLocation;


    public Air_Hockey_Renderer(Context context) {
        float[] tableVertices = {
//triangle 1
                -0.5f, -0.5f,0.4f, 0.4f, 0.4f,
                0.5f,  0.5f,0.4f, 0.4f, 0.4f,
                -0.5f,  0.5f,0.4f, 0.4f, 0.4f,

                -0.5f, -0.5f,0.4f, 0.4f, 0.4f,
                0.5f, -0.5f,0.4f, 0.4f, 0.4f,
                0.5f,  0.5f,0.4f, 0.4f, 0.4f,
//triangle fan
                0,0,1f,   1f,   1f,
                -0.45f, -0.45f, 0.7f, 0.7f, 0.7f,
                0.45f,  -0.45f,0.7f, 0.7f, 0.7f,
                0.45f,  0.45f,0.7f, 0.7f, 0.7f,
                -0.45f, 0.45f,0.7f, 0.7f, 0.7f,
                -0.45f, -0.45f,0.7f, 0.7f, 0.7f,
// Line 1
                -0.5f, 0f, 1f, 0f, 0f,
                0.5f, 0f, 1f, 0f, 0f,
// Mallets
                0f, -0.25f, 0f, 0f, 1f,
                0f,  0.25f, 1f, 0f, 0f,

        };

        vertexData = ByteBuffer.allocateDirect(tableVertices.length*BYTES_PER_FLOAT)
        .order(ByteOrder.nativeOrder()). asFloatBuffer();
        vertexData.put(tableVertices);
        this.context = context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        glClearColor(0.0f,0.0f,0.0f,0.0f);
        String vertexShaderSource = TextResourceReader.readTextFileFromResource(context,R.raw.simple_vertex_shader);
        String fragmentShaderSource = TextResourceReader.readTextFileFromResource(context,R.raw.simple_fragment_shader);
        Log.w("ShaderHelper",vertexShaderSource);
        Log.w("ShaderHelper",fragmentShaderSource);
        int vertexShader = ShaderHelper.compileVertexShader(vertexShaderSource);
        int fragmentShader = ShaderHelper.compileFragmentShader(fragmentShaderSource);
        program = ShaderHelper.linkProgram(vertexShader,fragmentShader);
        if(LoggerConfig.ON){
            ShaderHelper.validateProgram(program);
        }
        glUseProgram(program);

        aColorLocation = glGetAttribLocation(program, A_COLOR);
        aPositionLocation = glGetAttribLocation(program,A_POSITION);

        vertexData.position(0);
        glVertexAttribPointer(aPositionLocation,POSITION_COMPONENT_COUNT,GL_FLOAT,false,STRIDE,vertexData);
        glEnableVertexAttribArray(aPositionLocation);

        vertexData.position(POSITION_COMPONENT_COUNT);
        glVertexAttribPointer(aColorLocation,COLOR_COMPONENT_COUNT,GL_FLOAT,false,STRIDE,vertexData);
        glEnableVertexAttribArray(aColorLocation);
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int i, int i1) {
        glViewport(0,0,i,i1);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        glClear(GL_COLOR_BUFFER_BIT);
        glDrawArrays(GL_TRIANGLES,0,6);
        glDrawArrays(GL_TRIANGLE_FAN,6,6);
        glDrawArrays(GL_LINES,12,2);
        glDrawArrays(GL_POINTS,14,1);
        glDrawArrays(GL_POINTS,15,1);
    }
}