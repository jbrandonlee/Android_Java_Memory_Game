package sg.edu.nus.iss.memory;

import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final int MIN_IMAGES = 6;
    private final int MAX_IMAGES = 20;
    private List<String> imgUrls = new ArrayList<>();
    private List<File> imgFiles = new ArrayList<>();
    private List<Integer> selectedIds = new ArrayList<>();

    private Thread urlThread;
    private Thread fileThread;

    private GridAdapter mGridAdapter;
    private GridView mGridView;
    private TextView mSelectedText;
    private EditText mUrlField;
    private TextView mDownloadText;
    private ProgressBar mDownloadProgress;
    private Button mDownloadBtn;
    private Button mPlayBtn;

    private MediaPlayer bgmPlayer;

    // -- Demo Details --
    // Insufficient Images:  https://blank.page/
    // Connection Failed: https://qwerty/
    // Working: https://stocksnap.io/
    // Working: https://www.istockphoto.com/photos/new-year

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
    }

    @Override
    protected void onStart() {
        super.onStart();
        bgmPlayer = MediaPlayer.create(this, R.raw.bgm_main);
        bgmPlayer.setLooping(true);
        bgmPlayer.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        bgmPlayer.stop();
        bgmPlayer.release();
    }

    // -- Initialize Views --
    protected void initViews() {
        initGridAdapter();

        mSelectedText = findViewById(R.id.selectedText);
        mSelectedText.setVisibility(View.INVISIBLE);
        mUrlField = findViewById(R.id.urlField);

        mDownloadText = findViewById(R.id.downloadText);
        mDownloadProgress = findViewById(R.id.downloadProgress);
        mDownloadProgress.setVisibility(View.INVISIBLE);
        mDownloadBtn = findViewById(R.id.downloadButton);
        mDownloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Interrupt Ongoing Threads (if any)
                if (urlThread != null) {
                    urlThread.interrupt();
                }
                if (fileThread != null) {
                    fileThread.interrupt();
                }
                // Start Download
                getImgUrlfromWeb(mUrlField.getText().toString());
            }
        });

        mPlayBtn = findViewById(R.id.playButton);
        mPlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Start Game
                Intent intent = new Intent(MainActivity.this, GameActivity.class);
                intent.putIntegerArrayListExtra("imgIds", (ArrayList<Integer>) selectedIds);
                startActivity(intent);
            }
        });
        togglePlayBtn(false);
    }

    protected void initGridAdapter() {
        mGridAdapter = new GridAdapter(this, imgFiles);
        mGridView = findViewById(R.id.gridView);
        mGridView.setAdapter(mGridAdapter);
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (!selectedIds.contains(i)) {
                    selectedIds.add(i);
                    view.setBackgroundColor(Color.parseColor("#BF4F51"));
                } else {
                    selectedIds.remove(Integer.valueOf(i));
                    view.setBackgroundColor(Color.TRANSPARENT);
                }
                updateSelected(selectedIds.size());
                togglePlayBtn(selectedIds.size() == 6);
            }
        });
    }

    // -- Image Download --
    protected void getImgUrlfromWeb(String webUrl) {
        resetDataAndView();

        urlThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Document document = Jsoup.connect(webUrl).get();
                    Elements imgElements = document.getElementsByTag("img");

                    // Check if src from 'img' elements are valid
                    for (Element elem : imgElements) {
                        if (Thread.interrupted()) {
                            System.out.println("URL Thread Interrupted");
                            return;
                        }

                        String src = elem.absUrl("src");
                        if (isValidImgSrc(src)) {
                            // System.out.println("Found Image: " + src);
                            imgUrls.add(src);
                        }

                        // Stop once enough valid ImageUrls are found
                        if (imgUrls.size() == MAX_IMAGES) {
                            startDownloadImages(imgUrls);
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    makeUIThreadToast("Failed to connect to URL");
                }
            }
        });
        urlThread.start();
    }

    protected void startDownloadImages(List<String> imgUrls) {
        // Check if there are sufficient images to download
        if (imgUrls.size() < MIN_IMAGES) {
            makeUIThreadToast("Insufficient images in URL: " + imgUrls.size());
            return;
        }

        fileThread = new Thread(new Runnable() {
            @Override
            public void run() {
                File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                for (int i = 0; i < imgUrls.size(); i++) {
                    if (Thread.interrupted()) {
                        System.out.println("File Thread Interrupted");
                        return;
                    }
                    File destFile = new File(dir, i + ".jpg");
                    try {
                        if (downloadImage(imgUrls.get(i), destFile)) {
                            imgFiles.add(destFile);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateProgress(imgFiles.size());
                                    updateGridView();
                                }
                            });
                        }
                    } catch (InterruptedIOException e) {
                        System.out.println("File Thread Interrupted from Exception");
                        return;
                    }
                }
            }
        });
        fileThread.start();
    }

    protected boolean downloadImage(String imgUrl, File destFile) throws InterruptedIOException {
        // System.out.println("Downloading: " + imgUrl);
        try {
            URL url = new URL(imgUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            InputStream in = conn.getInputStream();
            FileOutputStream out = new FileOutputStream(destFile);

            byte[] buf = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = in.read(buf)) != -1) {
                out.write(buf, 0, bytesRead);
            }

            out.close();
            in.close();
            return true;
        } catch (InterruptedIOException e) {
            throw new InterruptedIOException();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // -- View Management --
    protected void resetDataAndView() {
        imgUrls.clear();
        imgFiles.clear();
        updateGridView();
        mDownloadText.setText("");
        mDownloadProgress.setVisibility(View.INVISIBLE);
        mSelectedText.setVisibility(View.INVISIBLE);
    }

    protected void updateGridView() {
        mGridAdapter.updateImages(imgFiles);
    }

    protected void updateSelected(int count) {
        mSelectedText.setText("Selected: " + count + "/" + MIN_IMAGES);
    }

    protected void updateProgress(int count) {
        mDownloadProgress.setVisibility(View.VISIBLE);
        mDownloadProgress.setProgress(MAX_IMAGES);
        mDownloadText.setText("Downloading... " + count + "/" + MAX_IMAGES);

        if (count == MAX_IMAGES) {
            mDownloadText.setText("Download Complete " + count + "/" + MAX_IMAGES);
            mSelectedText.setVisibility(View.VISIBLE);
        }
    }

    protected void togglePlayBtn(boolean enabled) {
        if (enabled) {
            mPlayBtn.setBackgroundColor(Color.parseColor("#90EE90"));
            mPlayBtn.setTextColor(Color.parseColor("#000000"));
        } else {
            mPlayBtn.setBackgroundColor(Color.parseColor("#AAAAAA"));
            mPlayBtn.setTextColor(Color.parseColor("#DDDDDD"));
        }
        mPlayBtn.setEnabled(enabled);
    }

    // -- Helper Functions --
    protected static boolean isValidImgSrc(String imgUrl) {
        if (imgUrl.indexOf('?') > 0)
            imgUrl = imgUrl.substring(0, imgUrl.indexOf('?'));

        String fileType = imgUrl.substring(imgUrl.lastIndexOf("."));
        if (!fileType.equals(".jpg") && !fileType.equals(".png") && !fileType.equals(".bmp"))
            return false;

        return true;
    }

    protected void makeUIThreadToast(String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }
}