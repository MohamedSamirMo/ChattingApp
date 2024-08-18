package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {


    private LottieAnimationView lottieAnimationView;
    FirebaseAuth auth = FirebaseAuth.getInstance();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                lottieAnimationView = findViewById(R.id.lottieAnimationView);

                // Play the animation
                lottieAnimationView.playAnimation();

                // Pause the animation
                lottieAnimationView.pauseAnimation();

                // Stop the animation
                lottieAnimationView.cancelAnimation();

                // Set the animation speed
                lottieAnimationView.setSpeed(1.5f); // 1.5 times the normal speed

                // Set the animation progress
                lottieAnimationView.setProgress(0.5f);
                if (auth.getCurrentUser() != null) {

                    startActivity(new Intent(SplashActivity.this, UsersActivity.class));


                }else {
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                }
                finish();
            }
        },6000);

    }
}