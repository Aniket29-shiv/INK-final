package com.google.mlkit.samples.vision.digitalink;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SignIn extends AppCompatActivity {

    ImageView backButton;
    TextView RegisterButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signin);

        backButton = findViewById(R.id.back_btn);
        backButton.setOnClickListener(view -> {
            Intent intent = new Intent(SignIn.this, HomeScreen.class);
            startActivity(intent);
        });

        RegisterButton = findViewById(R.id.register);
        RegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignIn.this, SignUp.class);
                startActivity(intent);
            }
        });
    }
}