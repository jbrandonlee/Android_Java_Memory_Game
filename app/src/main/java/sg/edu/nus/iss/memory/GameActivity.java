package sg.edu.nus.iss.memory;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
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
    private MediaPlayer bgmPlayer;
    private SoundPool soundPool;
    private int sfx_flip, sfx_unflip, sfx_match, sfx_win, sfx_lose;

    private final int MAX_SCORE = 6;
    private int score = 0;
    private int seconds = 0;
    private int selectedId = -1;
    private boolean gameRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        initSoundPool();
        initViews();
        initGame();
    }

    @Override
    protected void onStart() {
        super.onStart();
        bgmPlayer = MediaPlayer.create(this, R.raw.bgm_game);
        bgmPlayer.setLooping(true);
        bgmPlayer.setVolume(0.5f,0.5f);
        bgmPlayer.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        bgmPlayer.stop();
        bgmPlayer.release();
    }

    protected void initSoundPool() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes =
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build();
            soundPool = new SoundPool.Builder()
                                .setMaxStreams(5)
                                .setAudioAttributes(audioAttributes)
                                .build();
        } else {
            soundPool = new SoundPool(3, AudioManager.STREAM_MUSIC,0);
        }
        sfx_flip = soundPool.load(this, R.raw.sfx_flip, 1);
        sfx_unflip = soundPool.load(this, R.raw.sfx_unflip, 1);
        sfx_match = soundPool.load(this, R.raw.sfx_match, 1);
        sfx_win = soundPool.load(this, R.raw.sfx_win, 1);
        sfx_lose = soundPool.load(this, R.raw.sfx_lose, 1);
    }

    protected void initViews() {
        initGridAdapter();
        mTimerText = findViewById(R.id.timerText);
        mScoreText = findViewById(R.id.scoreText);
        mEndButton = findViewById(R.id.endButton);
        mEndButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                // Do nothing if player clicks on selected tile
                if (i == selectedId) { return; }

                // Play SFX and Animate
                soundPool.play(sfx_flip, 1,1,0,0,1);
                flipCard(view);

                // Gameplay
                if (selectedId == -1) {
                    // Player is selecting first tile
                    selectedId = i;
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
        int delay;
        gameRunning = false;
        mEndButton.setEnabled(false);
        bgmPlayer.pause();
        if (score == MAX_SCORE) {
            delay = 2000;
            soundPool.play(sfx_win, 1,1,0,0,1);
        } else {
            delay = 1000;
            soundPool.play(sfx_lose, 1,1,0,0,1);
        }
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, delay);
    }

    protected void checkCardMatch(int firstId, int secondId) {
        System.out.println("Checking: " + firstId + " and " + secondId);
        View first = mGridView.getChildAt(firstId);
        View second = mGridView.getChildAt(secondId);
        ImageView firstImage = first.findViewById(R.id.front);
        ImageView secondImage = second.findViewById(R.id.front);
        selectedId = -1;

        if (gridAnswers.get(firstId) == gridAnswers.get(secondId)) {
            // Successful Match - Play SFX_MATCH, Tint Inactive, Disable ImageViews OnClick
            soundPool.play(sfx_match, 1,1,0,0,1);
            firstImage.setColorFilter(Color.parseColor("#AAFFFFFF"), PorterDuff.Mode.MULTIPLY);
            secondImage.setColorFilter(Color.parseColor("#AAFFFFFF"), PorterDuff.Mode.MULTIPLY);
            first.setOnClickListener(null);
            second.setOnClickListener(null);
            score++;
            updateScore(score);
            if (score == MAX_SCORE) {
                endGame();
            }
        } else {
            // Unsuccessful Match - Tint Error, Wait, Play SFX_UNFLIP, Flip and Untint
            firstImage.setColorFilter(Color.parseColor("#DDFFCCCC"), PorterDuff.Mode.MULTIPLY);
            secondImage.setColorFilter(Color.parseColor("#DDFFCCCC"), PorterDuff.Mode.MULTIPLY);
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    soundPool.play(sfx_unflip, 1,1,0,0,1);
                    flipCard(mGridView.getChildAt(firstId));
                    flipCard(mGridView.getChildAt(secondId));
                    firstImage.clearColorFilter();
                    secondImage.clearColorFilter();
                }
            }, 600);
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