package sg.edu.nus.iss.memory;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
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

public class GameActivity extends AppCompatActivity {

    private List<Integer> imgIds;
    private List<Integer> gridAnswers = new ArrayList<>();
    private List<File> imgFiles = new ArrayList<>();

    private TextView scoreText;
    private Button endButton;

    private int selectedId = -1;
    private int score = 0;
    private final int MAX_SCORE = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        scoreText = findViewById(R.id.scoreText);
        endButton = findViewById(R.id.endButton);
        endButton.setOnClickListener(new View.OnClickListener() {
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
    }

    protected void endGame() {
        if (score == MAX_SCORE) {
            // TODO: Play SFX_SUCCESS_LONG
        } else {
            // TODO: Play SFX_FAILURE_LONG
        }
        finish();
    }

    protected void updateScore(int score) {
        scoreText.setText(score + " out of 6 matches");
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