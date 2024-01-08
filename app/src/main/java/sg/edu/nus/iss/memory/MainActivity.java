package sg.edu.nus.iss.memory;

import android.content.Intent;
import android.graphics.Color;
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

    TextView mSelectedText;
    EditText mUrlField;
    TextView mDownloadText;
    ProgressBar mDownloadProgress;
    Button mUrlBtn;
    Button mPlayBtn;

    // https://stocksnap.io/
    // https://www.istockphoto.com/photos/new-year

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
    }

    protected void initViews() {
        mSelectedText = findViewById(R.id.selectedText);
        mUrlField = findViewById(R.id.urlField);

        mDownloadText = findViewById(R.id.downloadText);
        mDownloadProgress = findViewById(R.id.downloadProgress);
        mDownloadProgress.setVisibility(View.INVISIBLE);

        mUrlBtn = findViewById(R.id.urlButton);
        mUrlBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getImgSrcfromUrl(mUrlField.getText().toString());
            }
        });

        mPlayBtn = findViewById(R.id.playButton);
        mPlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, GameActivity.class);
                intent.putIntegerArrayListExtra("imgIds", (ArrayList<Integer>) selectedIds);
                startActivity(intent);
            }
        });
        togglePlayBtn(false);
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

    protected void getImgSrcfromUrl(String url) {
        imgUrls.clear();
        imgFiles.clear();

        // Throws NetworkOnMainThreadException if not run on new Thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Document document = Jsoup.connect(url).get();
                    Elements imgElements = document.getElementsByTag("img");

                    // Check if src from 'img' elements are valid
                    for (Element elem : imgElements) {
                        String src = elem.absUrl("src");
                        if (isValidImgSrc(src))
                            imgUrls.add(src);

                        if (imgUrls.size() >= MAX_IMAGES)
                            break;
                    }

                    startDownloadImages(imgUrls);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                    makeUIThreadToast("Failed to connect to URL");
                }
            }
        }).start();
    }

    protected void startDownloadImages(List<String> imgUrls) {
        // Check if there are sufficient images to download
        if (imgUrls.size() < MIN_IMAGES) {
            makeUIThreadToast("Insufficient images in URL: " + imgUrls.size());
            return;
        }

        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < imgUrls.size(); i++) {
                    File destFile = new File(dir, i + ".jpg");
                    if (downloadImage(imgUrls.get(i), destFile)) {
                        imgFiles.add(destFile);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateProgress(imgFiles.size());
                            }
                        });
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateGridView();
                    }
                });
            }
        }).start();
    }

    protected boolean downloadImage(String imgUrl, File destFile) {
        System.out.println("Downloading: " + imgUrl);
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
        } catch (Exception e) {
            System.out.println("Download FAILED");
            e.printStackTrace();
            return false;
        }
    }

    // Bug: Do not create new adapter for gridview if download is clicked again
    // https://stackoverflow.com/questions/11786050/refresh-listview-from-arrayadapter
    protected void updateGridView() {
        GridAdapter adapter = new GridAdapter(this, imgFiles);
        GridView gridView = findViewById(R.id.gridView);
        if (gridView != null) {
            gridView.setAdapter(adapter);
            gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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

                    if (selectedIds.size() == 6) {
                        togglePlayBtn(true);
                    } else {
                        togglePlayBtn(false);
                    }
                }
            });
        }
    }

    protected void updateSelected(int count) {
        mSelectedText.setText("Selected: " + count + "/" + MIN_IMAGES);
    }

    protected void updateProgress(int count) {
        mDownloadProgress.setVisibility(View.VISIBLE);
        mDownloadProgress.setProgress(count);
        mDownloadText.setText("Downloading... " + count + "/" + MAX_IMAGES);

        if (count == MAX_IMAGES) {
            mDownloadText.setText("Download Complete " + count + "/" + MAX_IMAGES);
        }
    }

    // -- Helper Functions --
    protected void printImgUrls() {
        System.out.println(imgUrls.size() + " images found.");
        for (String url : imgUrls) {
            System.out.println(url);
        }
    }

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