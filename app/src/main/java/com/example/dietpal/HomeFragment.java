package com.example.dietpal;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.dietpal.Models.RecipeActivity;

public class HomeFragment extends Fragment {

    CardView cardGym, cardBMI, cardMeals;
    Button LogOut;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        cardGym = view.findViewById(R.id.cardGym);

        cardGym.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), Gym.class);
                startActivity(intent);
            }
        });

        cardBMI = view.findViewById(R.id.cardBMI);

        cardBMI.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openBmiFragment();
            }
        });

        cardMeals=view.findViewById(R.id.cardMeals);

        cardMeals.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getActivity(), RecipeActivity.class);
                startActivity(intent);
            }
        });

        LogOut=view.findViewById(R.id.LogOut);

        LogOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getActivity(), Welcome.class);
                startActivity(intent);

            }
        });


        return view;
    }

    private void openBmiFragment() {
        BmiFragment bmiFragment = new BmiFragment(); // Create an instance of BmiFragment
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.frameLayout, bmiFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
