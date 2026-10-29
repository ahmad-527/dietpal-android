package com.example.dietpal;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;

public class Gym extends AppCompatActivity {


    int[] newArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gym);

        Toolbar toolbar=findViewById(R.id.toolbarGym);
        setSupportActionBar(toolbar);

        newArray= new int[]
                {
                        R.id.mountain,R.id.crunches,R.id.bench,
                        R.id.bicycle,R.id.legRaises,R.id.heels,
                        R.id.legCrunches,R.id.sitUp,R.id.vUp,
                        R.id.plank,R.id.legPlank,R.id.Russian,
                        R.id.bridge,R.id.verticalLeg,R.id.toeTouch
                };
    }


    public void Imagebuttonclicked(View view)
    {
        for (int i=0;i<newArray.length;i++)
        {
            if (view.getId()==newArray[i])
            {
                int value=i+1;
                Log.i("First",String.valueOf(value));
                Intent intent=new Intent(Gym.this,workout.class);
                intent.putExtra("value",String.valueOf(value));
                startActivity(intent);

            }
        }



    }


}