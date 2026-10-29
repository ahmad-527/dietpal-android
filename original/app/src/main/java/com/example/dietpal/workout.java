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

    Intent intent = getIntent();
    buttonValue = intent.getStringExtra("value");

    int intValue;
    try {
      intValue = Integer.parseInt(buttonValue);
    } catch (NumberFormatException invalidExercise) {
      finish();
      return;
    }
    if (intValue < 1 || intValue > 15) {
      finish();
      return;
    }

    int[] layouts = {
      R.layout.mountain,
      R.layout.crunches,
      R.layout.bench,
      R.layout.bicycle,
      R.layout.legraises,
      R.layout.heels,
      R.layout.legcrunches,
      R.layout.situp,
      R.layout.vups,
      R.layout.plank,
      R.layout.legplank,
      R.layout.russian,
      R.layout.bridge,
      R.layout.verticalleg,
      R.layout.toetouch
    };
    setContentView(layouts[intValue - 1]);

    startBtn = findViewById(R.id.startButton);
    mtextview = findViewById(R.id.time);

    startBtn.setOnClickListener(
        new View.OnClickListener() {
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

    countDownTimer =
        new CountDownTimer(MTimeleftinMills, 1000) {
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
              intent.putExtra("value", String.valueOf(newValue));
              startActivity(intent);
              finish();
            } else {
              newValue = 1;
              Intent intent = new Intent(workout.this, workout.class);
              intent.putExtra("value", String.valueOf(newValue));
              startActivity(intent);
              finish();
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

  @Override
  protected void onDestroy() {
    if (countDownTimer != null) countDownTimer.cancel();
    super.onDestroy();
  }

  @Override
  protected void onStop() {
    if (countDownTimer != null) countDownTimer.cancel();
    MTimeRunning = false;
    if (startBtn != null) startBtn.setText("START");
    super.onStop();
  }
}
