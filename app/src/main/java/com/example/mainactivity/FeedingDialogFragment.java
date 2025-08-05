package com.example.mainactivity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.mainactivity.Database.AppDatabase;
import com.example.mainactivity.Database.entity.InfoEntity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.atomic.AtomicReference;

public class FeedingDialogFragment extends DialogFragment {

    private MaterialButton buttonStarterFed;
    private MaterialButton buttonExitDialog;
    private TextView textViewRequirementsSubtitle;
    private TextView textViewRequirements;
    private TextView textViewInstructionsSubtitle;
    private TextView textViewProgress;
    private TextView textViewInstructions;
    private TextView textViewTips;

    private String deviceMac;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.feeding_dialog_fragment, container, false);

        //initialize views here
        buttonStarterFed = view.findViewById(R.id.buttonStarterFed);
        buttonExitDialog = view.findViewById(R.id.buttonExitDialog);
        textViewRequirementsSubtitle = view.findViewById(R.id.textViewRequirementsSubtitle);
        textViewRequirements = view.findViewById(R.id.textViewRequirements);
        textViewInstructionsSubtitle = view.findViewById(R.id.textViewInstructionsSubtitle);
        textViewProgress = view.findViewById(R.id.textViewProgress);
        textViewInstructions = view.findViewById(R.id.textViewInstructions);
        textViewTips = view.findViewById(R.id.textViewTips);

        //get day progress from firebase
        FirebaseDatabase firebase = FirebaseDatabase.getInstance();
        String dayProgressRef = "sensors/" + deviceMac + "/general/current_day";

        //get local database
        AppDatabase db = AppDatabase.getInstance(getContext());


        //set text for views here from local sql database
        //REQUIREMENTS
        AtomicReference<InfoEntity> infoEntity = new AtomicReference<>(db.infoDao().getInfoByDay(0));
        textViewRequirements.setText(infoEntity.get().getInfo());

        //INSTRUCTIONS
        firebase.getReference(dayProgressRef).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot dataSnapshot = task.getResult();
                if (dataSnapshot.exists()) {
                    String dayProgress = dataSnapshot.getValue(String.class);

                    //check if day progress is null + fix it
                    if (dayProgress == null) dayProgress = "0";
                    int day = Integer.parseInt(dayProgress);
                    if (day < 0) day = 0;
                    if (day > 7) day = 8;

                    //set daily instructions here
                    infoEntity.set(db.infoDao().getInfoByDay(day)); //set to current day
                    textViewProgress.setText(infoEntity.get().getDayName() + "/7");
                    textViewInstructions.setText(infoEntity.get().getInfo());
                } else {
                    Toast.makeText(getContext(), "Firebase Retrieval Error", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //set click listeners for buttons here
        buttonStarterFed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //update day progress in firebase
                FirebaseDatabase firebase = FirebaseDatabase.getInstance();
                String dayProgressRef = "sensors/" + deviceMac + "/general/current_day";

                //get current day
                firebase.getReference(dayProgressRef).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DataSnapshot dataSnapshot = task.getResult();
                        if (dataSnapshot.exists()) {
                            String dayProgress = dataSnapshot.getValue(String.class);

                            //check if day progress is null + fix it
                            if (dayProgress == null) dayProgress = "0";
                            int day = Integer.parseInt(dayProgress);
                            if (day < 0) day = 0;
                            if (day > 7) day = 8;

                            //update day progress in firebase
                            firebase.getReference(dayProgressRef).setValue(String.valueOf(day + 1));
                        } else {
                           //do nothing
                        }
                    }
                });


                //dismiss the dialog
                dismiss();
            }
        });

        //exit button click
        view.findViewById(R.id.buttonExitDialog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        return view;
    }
}
