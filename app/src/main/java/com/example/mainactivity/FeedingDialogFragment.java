package com.example.mainactivity;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static android.widget.Toast.LENGTH_LONG;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.example.mainactivity.Database.AppDatabase;
import com.example.mainactivity.Database.entity.InfoEntity;
import com.example.mainactivity.Database.entity.TipsEntity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
    private TextView textviewStatus;
    private DatabaseReference databaseReference;
    private int selectedDay = 0; // Default to day 1
    private Spinner day_spinner;
    private String deviceMac;
    private AtomicReference<InfoEntity> infoEntity;
    private AtomicReference<TipsEntity> tipsEntity;
    private AppDatabase db;
    private Integer currentDay;
    private Boolean first_open = true;
    private boolean ready_state = false;
    private int attempt = 1;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        //get the device MAC
        Bundle mArgs = getArguments();
        deviceMac = mArgs.getString("MAC");
        ready_state = mArgs.getBoolean("READY");
        attempt = mArgs.getInt("ATTEMPT");
        System.out.println("MAC IS:" + deviceMac);

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

        textviewStatus = view.findViewById(R.id.feeding_instructions_next_day_status);

        //get day progress from firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("sensors/" + deviceMac);

        //get local database
        db = AppDatabase.getInstance(getContext());

        //set text for views here from local sql database
        //REQUIREMENTS
        infoEntity = new AtomicReference<>(db.infoDao().getInfoByDay(10)); //day = 10 to set requirements
        tipsEntity = new AtomicReference<>();
        textViewRequirements.setText(infoEntity.get().getInfo());

        //INSTRUCTIONS
        databaseReference.child("general").child("current_day").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                DataSnapshot dataSnapshot = task.getResult();
                if (dataSnapshot.exists()){
                    currentDay = dataSnapshot.getValue(Integer.class);
                    selectedDay = currentDay;
                    if (currentDay == 0){
                        textviewStatus.setVisibility(GONE);
                    }else {
                        textviewStatus.setVisibility(VISIBLE);
                    }
                    //check if day progress is null + fix it
                    if (currentDay == null) currentDay = 0; //error state
                    int day = currentDay;
                    if (day < 0) day = 0; //error state
                    if (day > 7) day = 8;
                    //Toast.makeText(getContext(),"Currently Day: " + currentDay, Toast.LENGTH_SHORT).show();

                    //set daily instructions here
                    infoEntity.set(db.infoDao().getInfoByDay(day)); //set to current day

                    List<TipsEntity> tipList = new ArrayList<>();
                    //get a random tip from the list of tips
                    if (currentDay == 8){
                        tipList = db.tipsDao().getTipsByCondition("day8");
                    }else if (currentDay == 7){
                        tipList = db.tipsDao().getTipsByCondition("day7");
                    } else{
                         tipList = db.tipsDao().getTipsByCondition("default");
                    }
                    //TODO: add the other temp flag after algorithm implemented.

                    Random r = new Random();
                    tipsEntity.set(tipList.get(r.nextInt(tipList.size())));
                    textViewTips.setText("Tip: \n" + tipsEntity.get().getTip());

                    textViewProgress.setText(infoEntity.get().getDayName() + "/7 Instructions");
                    textViewInstructions.setText(infoEntity.get().getInfo());
                    //spinner to select days
                    day_spinner = view.findViewById(R.id.feeding_instruction_day_spinner);
                    setup_spinner();

                }else{
                    Toast.makeText(getContext(), "SNAPSHOT ERROR", Toast.LENGTH_SHORT).show();
                }
            }else{
                System.out.println("IT DOES NTO EXIST");
                Toast.makeText(getContext(), "Firebase Retrieval Error", Toast.LENGTH_SHORT).show();
            }
        });

        //set click listeners for buttons here
        buttonStarterFed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = getContext();
                if (context == null) return;
                if (ready_state) {
                    // Create alert dialog for confirmation
                    new AlertDialog.Builder(getContext())
                            .setTitle("Confirmation")
                            .setMessage("Are you sure you want to proceed to the next day?")
                            .setPositiveButton("I have fed my starter!", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // User clicked OK, perform database changes
                                    databaseReference.child("general").child("current_day").setValue(currentDay+1)
                                            .addOnSuccessListener(aVoid -> {
                                                databaseReference.child("general").child("enable").setValue(true)
                                                        .addOnFailureListener(e -> {
                                                            Toast.makeText(getContext(), "Failed to enable device: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        });
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(getContext(), "Failed to set the device date!" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                    dismiss();
                                }
                            })
                            .setNegativeButton("Not Yet!", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // User cancelled the dialog
                                    dialog.dismiss();
                                }
                            })
                            .create()
                            .show();
                } else {
                    Toast.makeText(getContext(), "The dough is not ready for the next day!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        //update day progress in firebase
            //update day progress in firebase
            //TODO: ALGORITHM IMPORTANT NOTE: IF YOU ARE AT DAY 7 AND GO OVER 24 HOURS, JUST JUMP TO DAY 8 AUTOMATICALLY.
            //TODO: ONLY SHOW THE STARTER FED OPTION WHEN THE USER IS WAITING ON THE NEXT DAY, OR IF THE USER IS READY TO MOVE ON!
            // SO DAY + 1, THEN IF THE ALGORITHM DECIDES THAT THEY ARE READY, THEN YOU CAN SEE THE BUTTON.
            // THEN THE PRESSING OF IT WILL CHANGE THE DAY
            // ENABLE THE LID AGAIN (IF IT WAS DISABLED).
            // REMOVE THE NEXT DAY BUTTON ONCE COMPLETED.
            // SPECIAL ALGORITHM FOR DAY 1 AND DAY 2.
            // Add this spinner selection listener
        //dismiss the dialog
        //exit button click
        view.findViewById(R.id.buttonExitDialog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        return view;
    }

    private void setup_spinner() {
        List<String> days = new ArrayList<>();

        // Add days from 1 to 8 to the list
        // day 0 is used in the database to store the starter requirements, so don't include it here
        for (int day = 1; day <= 8; day++) {
            days.add("Day " + day);
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, days);

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        day_spinner.setAdapter(spinnerAdapter);



        day_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = (String) parent.getItemAtPosition(position);
                //feeding instructions open to current day
                if (first_open){
                    selectedDay = currentDay;
                    int spinnerPosition = spinnerAdapter.getPosition("Day " + currentDay);
                    day_spinner.setSelection(spinnerPosition);
                    first_open = false;
                    //after day 7+, disable the feeding button
                    if (currentDay == 8){
                        buttonStarterFed.setVisibility(GONE);
                        textviewStatus.setVisibility(GONE);
                    }
                }else{
                    selectedDay = Integer.parseInt(selected.replace("Day ", ""));
                    day_spinner.getSelectedItemPosition();
                    System.out.println("WE ARE CURRENTLY SELECTING DAY: " + selectedDay + " WITH ID: " + day_spinner.getSelectedItemPosition());
                }

                //check if feeding conditions have been met, if yes: enable button, otherwise: disable button
                if (ready_state){
                    textviewStatus.setText("Ready for Day " + (currentDay + 1) + " ! Check the instructions!");
                    if (currentDay == day_spinner.getSelectedItemPosition()){
                        buttonStarterFed.setVisibility(VISIBLE);
                    }else{
                        buttonStarterFed.setVisibility(GONE);
                    }
                }else{
                    textviewStatus.setText("Not ready for Day " + (currentDay + 1));
                    buttonStarterFed.setVisibility(GONE);
                }
                //set daily instructions here
                infoEntity.set(db.infoDao().getInfoByDay(selectedDay)); //set to current day
                textViewProgress.setText(infoEntity.get().getDayName() + "/7 Instructions");
                textViewInstructions.setText(infoEntity.get().getInfo());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        ViewGroup.LayoutParams params = getDialog().getWindow().getAttributes();
        params.width = ConstraintLayout.LayoutParams.MATCH_PARENT;
//        params.height = ConstraintLayout.LayoutParams.MATCH_PARENT;
        getDialog().getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
    }
}
