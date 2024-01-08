package sg.edu.nus.iss.memory;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

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

    private TextView mTimerText;
    private TextView mScoreText;
    private Button mEndButton;

    private final int MAX_SCORE = 6;
    private int score = 0;
    private int seconds = 0;
    private int selectedId = -1;
    private boolean gameRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
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

        initGame();
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

    protected void endGame() {
        if (score == MAX_SCORE) {
            // TODO: Play SFX_SUCCESS_LONG
        } else {
            // TODO: Play SFX_FAILURE_LONG
        }
        finish();
    }

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

    protected void updateScore(int score) {
        this.score = score;
        mScoreText.setText(score + " out of 6 matches");
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

    protected void setSelected(View view, boolean selected) {
        if (selected) {
            view.setBackgroundColor(Color.parseColor("#BF4F51"));
        } else {
            view.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    protected void checkCardMatch(int firstId, int secondId) {
        GridView gridView = findViewById(R.id.gridView);
        gridView.getChildAt(firstId).setBackgroundColor(Color.TRANSPARENT);
        gridView.getChildAt(secondId).setBackgroundColor(Color.TRANSPARENT);
        selectedId = -1;

        if (gridAnswers.get(firstId) == gridAnswers.get(secondId)) {
            // TODO: ANIMATE GREEN TINT
            // TODO: Play SFX_SUCCESS
            // TODO: Disable ImageViews
            Toast.makeText(this, "CORRECT", Toast.LENGTH_LONG).show();
            score++;
            updateScore(score);

            if (score == MAX_SCORE) {
                endGame();
            }
        } else {
            // TODO: ANIMATE RED TINT
            // TODO: Play SFX_FAILURE
            Toast.makeText(this, "WRONG", Toast.LENGTH_LONG).show();
        }
    }

    // TODO: REFACTOR THIS
    protected void updateGridView() {
        GridAdapter adapter = new GridAdapter(this, imgFiles);
        GridView gridView = findViewById(R.id.gridView);
        if (gridView != null) {
            gridView.setAdapter(adapter);
            gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    // Gameplay
                    if (selectedId == -1) {
                        // Player is selecting first tile
                        selectedId = i;
                        setSelected(view, true);
                    } else if (selectedId == i) {
                        // Player is unselecting first tile
                        selectedId = -1;
                        setSelected(view, false);
                    } else {
                        // Player is selecting second tile
                        setSelected(view, true);
                        checkCardMatch(selectedId, i);
                    }
                }
            });
        }
    }
}