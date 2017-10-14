// MainActivityFragment.java
// Contains the Flag Quiz logic
package com.ahsan.a47_dietel_flagquizapp;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivityFragment extends Fragment {

    // String used when logging error messages
    private static final String TAG = "FlagQuiz Activity";

    private static final int FLAGS_IN_QUIZ = 10;//represents the number of flags in the quiz

    private List<String> fileNameList; // flag-image file names for the currently enabled geographic regions
    private List<String> quizCountriesList; // holds the flag file names for the countries used in the current quiz
    private Set<String> regionsSet; //stores the geographic regions that are enabled
    private String correctAnswer; // correct country for the current flag
    private int totalGuesses; // number of guesses made
    private int correctAnswers; // number of correct guesses
    private int guessRows; // is the number of two-Button LinearLayouts displaying the flag answer choices—this is controlled by the app’s settings
    private SecureRandom random; // the random-number generator used to randomly pick the flags to include in the quiz and which Button in the two-Button LinearLayouts represents the correct answer
    private Handler handler; // When the user selects a correct answer and the quiz is not over, we use the Handler object handler to load the next flag after a short delay
    private Animation shakeAnimation; // holds the dynamically inflated shake animation that’s applied to the flag image when an incorrect guess is made.

    private LinearLayout quizLinearLayout; // layout that contains the quiz
    private TextView questionNumberTextView; // shows current question #
    private ImageView flagImageView; // displays a flag
    private LinearLayout[] guessLinearLayouts; // rows of answer Buttons
    private TextView answerTextView; // displays correct answer



    // configures the MainActivityFragment when its View is created
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_main, container, false);//false is A boolean indicating whether or not the inflated GUI needs to be attached to the ViewGroup in the second argument. In a fragment’s onCreateView method, thisshould always be false

        fileNameList = new ArrayList<>();//stores the flag-image file names for the currently enabled geographical regions
        quizCountriesList = new ArrayList<>();//holds the names of the countries in the current quiz
        random = new SecureRandom();
        handler = new Handler();

        // load the shake animation that's used for incorrect answers
        shakeAnimation = AnimationUtils.loadAnimation(getActivity(),R.anim.incorrect_shake);//inherited Fragment method getActivity returns the Activity that hosts this Fragment-Activity is an indirect subclass of Context
        shakeAnimation.setRepeatCount(3);// animation repeats 3 times

        // get references to GUI components
        quizLinearLayout = (LinearLayout) view.findViewById(R.id.quizLinearLayout);
        questionNumberTextView = (TextView) view.findViewById(R.id.questionNumberTextView);
        flagImageView = (ImageView) view.findViewById(R.id.flagImageView);
        guessLinearLayouts = new LinearLayout[4];
        guessLinearLayouts[0] = (LinearLayout) view.findViewById(R.id.row1LinearLayout);
        guessLinearLayouts[1] = (LinearLayout) view.findViewById(R.id.row2LinearLayout);
        guessLinearLayouts[2] = (LinearLayout) view.findViewById(R.id.row3LinearLayout);
        guessLinearLayouts[3] = (LinearLayout) view.findViewById(R.id.row4LinearLayout);
        answerTextView = (TextView) view.findViewById(R.id.answerTextView);




        // configure listeners for the guess Buttons
        for (LinearLayout row : guessLinearLayouts) {
            for (int column = 0; column < row.getChildCount(); column++) {
                Button button = (Button) row.getChildAt(column);
                button.setOnClickListener(guessButtonListener);
            }
        }

        // set questionNumberTextView's text
        questionNumberTextView.setText(getString(R.string.question, 1, FLAGS_IN_QUIZ));//Question %1$d of %2$d String contains placeholders for two integer values

        return view;//returns the MainActivityFragment’s GUI.
    }





    // update guessRows based on values in SharedPreferences- is called from the app’s MainActivity when the app is launched and each time the user changes the number of guess Buttons to display with each flag.
    public void updateGuessRows(SharedPreferences sharedPreferences) {
        // get the number of guess buttons that should be displayed
        String choices = sharedPreferences.getString(MainActivity.CHOICES, null);//MainActivity.CHOICES—a constant containing the name of the preference in which the SettingsActivityFragment stores the number of guess Buttons to display.
        guessRows = Integer.parseInt(choices) / 2;//converts the preference’s value to an int and divides it by 2 to determine the value for guessRows, which indicates how many of the guessLinearLayouts should be displayed

        // hide all guess button LinearLayouts
        for (LinearLayout layout : guessLinearLayouts)//Iterate through all 4 guessLinearLayouts[4] array
            layout.setVisibility(View.GONE);

        // display appropriate guess button LinearLayouts
        for (int row = 0; row < guessRows; row++)
            guessLinearLayouts[row].setVisibility(View.VISIBLE);
    }






    // update world regions for quiz based on values in SharedPreferences
    public void updateRegions(SharedPreferences sharedPreferences){
        regionsSet = sharedPreferences.getStringSet(MainActivity.REGIONS, null);//MainActivity.REGIONS is a constant containing the name of the preference in which the SettingsActivityFragment stores the enabled world regions.
    }







    // set up and start the next quiz
    public void resetQuiz(){
        // use AssetManager to get image file names for enabled regions
        AssetManager assets = getActivity().getAssets();
        fileNameList.clear();// empty list of image file names

        try{
            // iterate through all ENABLED world regions.
            for (String region: regionsSet){
                //We use AssetManager’s list method to get an array of the flag-image file names, which we store in the String array paths
                String[] paths = assets.list(region);//assets.list = Return a String array of all the assets at the given path.

                for (String path : paths){
                    fileNameList.add(path.replace(".png", ""));//remove the .png extension from each file name and place the names in the fileNameList.
                }
            }

        } catch (IOException exception) {
            Log.e(TAG, "Error loading image file names", exception);
        }

        correctAnswers = 0; // reset the number of correct answers made
        totalGuesses = 0; // reset the total number of guesses the user made
        quizCountriesList.clear(); // clear prior list of quiz countries

        int flagCounter = 1;
        int numberOfFlags = fileNameList.size();

        // add 10 (FLAGS_IN_QUIZ) randomly selected file names to the quizCountriesList
        while (flagCounter <= FLAGS_IN_QUIZ){
            int randomIndex = random.nextInt(numberOfFlags);

            // get the random file name
            String filename = fileNameList.get(randomIndex);

            // if the region is enabled and it hasn't already been chosen
            if (!quizCountriesList.contains(filename)){
                quizCountriesList.add(filename);// add the file to the list
                ++flagCounter;
            }
        }

        loadNextFlag(); // start the quiz by loading the first flag

    }






    //Method loadNextFlag loads and displays the next flag and the corresponding set of answer Buttons.
    //The image file names in quizCountriesList have the format(regionName-countryName) without the .png extension. If a regionName or countryName contains multiple words, they’re separated by underscores (_).
    private void loadNextFlag(){
        //We remove the first name from quizCountriesList and stores it in nextImage.We also save this in correctAnswer so it can be used later to determine whether the user made a correct guess
        String nextImage = quizCountriesList.remove(0);
        correctAnswer = nextImage;// update the correct answer

        Log.i(TAG, "Correct answer for this question is: " + correctAnswer);
        answerTextView.setText(""); // clear answerTextView

        // display the current question number in the questionNumberTextView using the formatted String resource R.string.question.
        questionNumberTextView.setText(getString(R.string.question, (correctAnswers + 1), FLAGS_IN_QUIZ));

        // extracts the region from nextImage the region to be used as the assets subfolder name from which we’ll load the image
        String region = nextImage.substring(0, nextImage.indexOf('-'));//quizCountriesList have the format(regionName-countryName)

        // use AssetManager to load next image from assets folder
        AssetManager assets = getActivity().getAssets();

        // get an InputStream to the asset representing the next flag and try to use the InputStream
        try(InputStream stream = assets.open(region + "/" + nextImage + ".png")){
            // load the asset as a Drawable and display on the flagImageView
            Drawable flag = Drawable.createFromStream(stream, nextImage);
            flagImageView.setImageDrawable(flag);

            animate(false); // animate the flag onto the screen


        } catch (IOException exception) {
            Log.e(TAG, "Error loading " + nextImage, exception);
        }

        Collections.shuffle(fileNameList); // shuffle file names

        //locate the correctAnswer and move it to the end of the fileNameList—later we’ll insert this answer randomly into the one of the guess Buttons.
        int correct = fileNameList.indexOf(correctAnswer);
        fileNameList.add(fileNameList.remove(correct));

        // add 2, 4, 6 or 8 guess Buttons based on the value of guessRows
        for (int row = 0; row < guessRows; row++){
            // place Buttons in currentTableRow
            for (int column = 0; column < guessLinearLayouts[row].getChildCount(); column++){
                // get reference to Button to configure
                Button newGuessButton = (Button) guessLinearLayouts[row].getChildAt(column);
                newGuessButton.setEnabled(true);

                // get country name and set it as newGuessButton's text
                String filename = fileNameList.get((row * 2) + column);
                newGuessButton.setText(getCountryName(filename));
            }
         }

        // randomly replace one Button with the correct answer
        int row = random.nextInt(guessRows); // pick random row
        int column = random.nextInt(2); // pick random column
        LinearLayout randomRow = guessLinearLayouts[row]; // get the row
        String countryName = getCountryName(correctAnswer);
        ((Button) randomRow.getChildAt(column)).setText(countryName);

        }

    // parses the country flag file name and returns the country name
    private String getCountryName(String name) {

        String modifiedName = name.substring(name.indexOf('-') + 1).replace('_', ' ');//NOTE: Edited by me
        Log.i(TAG, "Old name: " + name + ", and now new name is: " + modifiedName);
        return modifiedName;
    }


    // animates the entire quizLinearLayout on or off screen
    private void animate(boolean animateOut) {
        // prevent animation into the the UI for the first flag
        if (correctAnswers == 0)
            return;

        // calculate center x and center y
        int centerX = (quizLinearLayout.getLeft() + quizLinearLayout.getRight()) / 2; // calculate center x
        int centerY = (quizLinearLayout.getTop() + quizLinearLayout.getBottom()) / 2; // calculate center y

        // calculate animation radius
        int radius = Math.max(quizLinearLayout.getWidth(), quizLinearLayout.getHeight());

        Animator animator;

        // if the quizLinearLayout should animate out rather than in
        if (animateOut) {
            // create circular reveal animation
            animator = ViewAnimationUtils.createCircularReveal(quizLinearLayout, centerX, centerY, radius, 0);
            animator.addListener(new AnimatorListenerAdapter() {
                        // called when the animation finishes
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            loadNextFlag();
                        }
                    }
            );
        }
        else { // if the quizLinearLayout should animate in
            animator = ViewAnimationUtils.createCircularReveal(quizLinearLayout, centerX, centerY, 0, radius);
        }

        animator.setDuration(500); // set animation duration to 500 ms
        animator.start(); // start the animation
    }



    // called when a guess Button is touched
    private OnClickListener guessButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Button guessButton = ((Button) v);//The method receives the clicked Button as parameter v
            String guess = guessButton.getText().toString();
            String answer = getCountryName(correctAnswer);
            ++totalGuesses;// increment number of guesses the user has made

            if (guess.equals(answer)){ // if the guess is correct
                ++correctAnswers;

                // display correct answer in green text
                answerTextView.setText(answer + "!");
                answerTextView.setTextColor(getResources().getColor(R.color.correct_answer, getContext().getTheme()));

                disableButtons(); // disable all guess Buttons

                // if the user has correctly identified FLAGS_IN_QUIZ flags
                if (correctAnswers == FLAGS_IN_QUIZ) {


/*
The error is not especially weird. If you were not getting this error before, that was weird.
Android destroys and recreates fragments as part of a configuration change (e.g., screen rotation) and as part of rebuilding a task if needed
(e.g., user switches to another app, your app's process is terminated while it is in the background, then the user tries to return to your app, all within 30 minutes or so). Android has no means of recreating an anonymous subclass of DialogNew.
So, make a regular public Java class (or a public static nested class) that extends DialogNew and has your business logic, replacing the anonymous subclass of DialogNew that you are using presently.
*/

                    // DialogFragment to display quiz stats and start new quiz
                    DialogFragment quizResults = new DialogFragment() {
                                // create an AlertDialog and return it
                                @Override
                                public Dialog onCreateDialog(Bundle bundle) {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());//AlertDialog.Builder to configure and create an AlertDialog for showing the quiz results, then returns it.
                                    builder.setMessage(getString(R.string.results, totalGuesses, (1000 / (double) totalGuesses)));

                                    // "Reset Quiz" Button
                                    builder.setPositiveButton(R.string.reset_quiz, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    resetQuiz();
                                                }
                                            }
                                    );

                                    return builder.create(); // return the AlertDialog
                                }
                            };

                    // use FragmentManager to display the DialogFragment
                    quizResults.setCancelable(false);
                    quizResults.show(getFragmentManager(), "quiz results");


                } else { // answer is correct but quiz is not over - load the next flag after a 2-second delay
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            animate(true);// animate the flag off the screen
                        }
                    }, 2000);// 2000 milliseconds for 2-second delay

                }
            } else {// answer was incorrect
                flagImageView.startAnimation(shakeAnimation);// play shake

                // display "Incorrect!" in red
                answerTextView.setText(R.string.incorrect_answer);
                answerTextView.setTextColor(getResources().getColor(R.color.incorrect_answer, getContext().getTheme()));
                guessButton.setEnabled(false);// disable incorrect answer

            }
        }
    };




    // utility method that disables all answer Buttons
    private void disableButtons(){
        for (int row = 0; row < guessRows; row++){
            LinearLayout guessRow = guessLinearLayouts[row];
            for (int i = 0; i < guessRow.getChildCount(); i++)
                guessRow.getChildAt(i).setEnabled(false);
        }
    }



}





































