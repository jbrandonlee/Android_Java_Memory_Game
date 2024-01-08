package sg.edu.nus.iss.memory;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class GameActivity extends AppCompatActivity {

    private List<Integer> imgIds;
    private List<Integer> gridAnswers = new ArrayList<>();
    private List<File> imgFiles = new ArrayList<>();

    private GridFlipAdapter mGridAdapter;
    private GridView mGridView;
    private TextView mTimerText;
    private TextView mScoreText;
    private Button mEndButton;

    private AnimatorSet inLeftSet;

    private final int MAX_SCORE = 6;
    private int score = 0;
    private int seconds = 0;
    private int selectedId = -1;
    private boolean gameRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        initViews();
        initGame();
    }

    protected void initViews() {
        initGridAdapter();
        mTimerText = findViewById(R.id.timerText);
        mScoreText = findViewById(R.id.scoreText);
        mEndButton = findViewById(R.id.endButton);
        mEndButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Prompt player before ending game with AlertDialog.Builder
                endGame();
            }
        });
    }

    protected void initGridAdapter() {
        inLeftSet = (AnimatorSet) AnimatorInflater.loadAnimator(
                this, R.animator.card_flip_left_in);
        mGridAdapter = new GridFlipAdapter(this, imgFiles);
        mGridView = findViewById(R.id.gridView);
        mGridView.setAdapter(mGridAdapter);
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                System.out.println("Item clicked: " + i);
                flipCard(view);

                // Gameplay
                if (selectedId == -1) {
                    // Player is selecting first tile
                    selectedId = i;
                } else if (selectedId == i) {
                    // Player is unselecting first tile
                    selectedId = -1;
                } else {
                    // Player is selecting second tile
                    checkCardMatch(selectedId, i);
                }
            }
        });
    }

    protected void flipCard(View gridItem) {
        ViewFlipper flipper = gridItem.findViewById(R.id.view_flipper);
        inLeftSet.end();
        flipper.showNext();
        inLeftSet.setTarget(flipper.getCurrentView());
        inLeftSet.start();
    }

    protected void initGame() {
        getDataFromIntent();
        generateGridAnswers();
        generateGridFiles();
        updateGridView();
        updateScore(0);
        updateTimer(0);
        gameRunning = true;
        runTimer();
    }

    protected void getDataFromIntent() {
        Intent intent = getIntent();
        imgIds = intent.getIntegerArrayListExtra("imgIds");
    }

    protected void generateGridAnswers() {
        for (Integer i : imgIds) {
            gridAnswers.add(i);
            gridAnswers.add(i);
        }
        Collections.shuffle(gridAnswers);
    }

    protected void generateGridFiles() {
        for (Integer i: gridAnswers) {
            File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File image = new File(dir, i + ".jpg");
            imgFiles.add(image);
        }
    }

    // -- Game Logic --
    protected void endGame() {
        if (score == MAX_SCORE) {
            // TODO: Play SFX_SUCCESS_LONG
        } else {
            // TODO: Play SFX_FAILURE_LONG
        }
        finish();
    }

    protected void checkCardMatch(int firstId, int secondId) {
        System.out.println("Checking: " + firstId + " and " + secondId);
        selectedId = -1;

        if (gridAnswers.get(firstId) == gridAnswers.get(secondId)) {
            // TODO: ANIMATE GREEN TINT
            // TODO: Play SFX_SUCCESS
            // Disable ImageViews
            mGridView.getChildAt(firstId).setOnClickListener(null);
            mGridView.getChildAt(secondId).setOnClickListener(null);

            score++;
            updateScore(score);

            if (score == MAX_SCORE) {
                endGame();
            }
        } else {
            // TODO: ANIMATE RED TINT
            // TODO: Play SFX_FAILURE
            // Add delay before flipping back
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    flipCard(mGridView.getChildAt(firstId));
                    flipCard(mGridView.getChildAt(secondId));
                }
            }, 1000);
        }
    }

    // -- View Management --
    protected void runTimer() {
        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (gameRunning) {
                    seconds++;
                }
                updateTimer(seconds);
                handler.postDelayed(this, 1000);
            }
        });
    }

    protected void updateTimer(int seconds) {
        this.seconds = seconds;
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        String time = String.format(Locale.getDefault(),"%02d:%02d:%02d", hours, minutes, secs);
        mTimerText.setText(time);
    }

    protected void updateGridView() {
        mGridAdapter.updateImages(imgFiles);
    }

    protected void updateScore(int score) {
        this.score = score;
        mScoreText.setText(score + " out of 6 matches");
    }
}