package com.example.air_hockey;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

public class MenuActivity extends AppCompatActivity {

    TextView numLines;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        numLines = findViewById(R.id.numLinesextEdit);
        numLines.setText("0");
    }

    public void goPlay(View view) {
        int nLines = Integer.parseInt(numLines.getText().toString());
        if(nLines<=5){
            Toast.makeText(this,"Veuillez indiquer un nombre supérieur ou égal à 6", Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this,"combien : "+Integer.toString(nLines), Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this,Air_HockeyActivity.class);
        intent.putExtra("numLines",nLines);
        startActivity(intent);
    }

    public void goSix(View view){
        int nLines = 6;
        Intent intent = new Intent(this,Air_HockeyActivity.class);
        intent.putExtra("numLines",nLines);
        startActivity(intent);
    }

    public void goSettings(View view) {
    }
}