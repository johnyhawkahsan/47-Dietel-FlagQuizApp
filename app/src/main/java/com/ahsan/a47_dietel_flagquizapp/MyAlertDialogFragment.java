package com.ahsan.a47_dietel_flagquizapp;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

/**
 * Created by ahsan on 10/14/2017.
 */

public class MyAlertDialogFragment extends DialogFragment {

    //Constructor for this class
    public static MyAlertDialogFragment newInstance(String title, int totalGuesses){
        MyAlertDialogFragment fragment = new MyAlertDialogFragment();
        Bundle args = new Bundle();
        args.putString("quiz results", title);//Save "title" argument in "quiz results" so we can retrieve it below and then use it.
        args.putInt("totalGuesses", totalGuesses);//Save "totalGuesses" argument in "totalGuesses" so we can retrieve it below and then use it.
        fragment.setArguments(args);
        return fragment;
    }





    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String title = getArguments().getString("quiz results");
        int totalGuesses = getArguments().getInt("totalGuesses");//get the variable from Arguments and store in this variable so we can use it here.

        return new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(getString(R.string.results, totalGuesses, (1000 / (double) totalGuesses)))
                .setPositiveButton(R.string.reset_quiz, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //((MainActivityFragment)getParentFragment()).resetQuiz();//Not working

                                MainActivityFragment myParentFragment = (MainActivityFragment) getFragmentManager().findFragmentById(R.id.quizFragment);//Save this fragment in a variable according to it's id, quizFragment
                                myParentFragment.resetQuiz();//Now we can call this method

                            }
                        }
                )
                .create();
    }



}
