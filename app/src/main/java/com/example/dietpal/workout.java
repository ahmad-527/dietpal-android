package com.example.dietpal;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class workout extends AppCompatActivity {

    String buttonValue;
    Button startBtn;
    TextView mtextview;
    private CountDownTimer countDownTimer;
    private boolean MTimeRunning;
    private long MTimeleftinMills;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);

        Intent intent = getIntent();
        buttonValue = intent.getStringExtra("value");

        int intValue = Integer.valueOf(buttonValue);

        switch (intValue) {
            case 1:
                setContentView(R.layout.mountain);
                break;
            case 2:
                setContentView(R.layout.crunches);
                break;
            case 3:
                setContentView(R.layout.bench);
                break;
            case 4:
                setContentView(R.layout.bicycle);
                break;
            case 5:
                setContentView(R.layout.legraises);
                break;
            case 6:
                setContentView(R.layout.heels);
                break;
            case 7:
                setContentView(R.layout.legcrunches);
                break;
            case 8:
                setContentView(R.layout.situp);
                break;
            case 9:
                setContentView(R.layout.vups);
                break;
            case 10:
                setContentView(R.layout.plank);
                break;
            case 11:
                setContentView(R.layout.legplank);
                break;
            case 12:
                setContentView(R.layout.russian);
                break;
            case 13:
                setContentView(R.layout.bridge);
                break;
            case 14:
                setContentView(R.layout.verticalleg);
                break;
            case 15:
                setContentView(R.layout.toetouch);
                break;
        }

        startBtn = findViewById(R.id.startButton);
        mtextview = findViewById(R.id.time);

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MTimeRunning) {
                    stopTimer();
                } else {
                    startTimer();
                }
            }
        });
    }

    private void stopTimer() {
        countDownTimer.cancel();
        MTimeRunning = false;
        startBtn.setText("START");
    }

    private void startTimer() {
        final CharSequence value1 = mtextview.getText();
        String num1 = value1.toString();
        String num2 = num1.substring(0, 2);
        String num3 = num1.substring(3, 5);

        final int num = Integer.valueOf(num2) * 60 + Integer.valueOf(num3);
        MTimeleftinMills = num * 1000;

        countDownTimer = new CountDownTimer(MTimeleftinMills, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                MTimeleftinMills = millisUntilFinished;
                updateTimer();
            }

            @Override
            public void onFinish() {
                int newValue = Integer.valueOf(buttonValue) + 1;
                if (newValue <= 15) { // Update the limit based on your case
                    Intent intent = new Intent(workout.this, workout.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra("value", String.valueOf(newValue));
                    startActivity(intent);
                } else {
                    newValue = 1;
                    Intent intent = new Intent(workout.this, workout.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra("value", String.valueOf(newValue));
                    startActivity(intent);
                }
            }
        }.start();
        startBtn.setText("PAUSE");
        MTimeRunning = true;
    }

    private void updateTimer() {
        int min = (int) MTimeleftinMills / 60000;
        int sec = (int) MTimeleftinMills % 60000 / 1000;

        String timeLeftText = "";

        if (min < 10) {
            timeLeftText = "0";
        }
        timeLeftText += min + ":";
        if (sec < 10) {
            timeLeftText += "0";
        }
        timeLeftText += sec;
        mtextview.setText(timeLeftText);
    }
}
