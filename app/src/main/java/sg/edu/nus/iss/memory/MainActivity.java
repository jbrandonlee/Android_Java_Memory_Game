package sg.edu.nus.iss.memory;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
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

    // Debug Only
    String url = "https://stocksnap.io/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getImgSrcfromUrl(url);
    }

    protected void getImgSrcfromUrl(String url) {
        imgUrls.clear();
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

    protected void updateGridView() {
        GridAdapter adapter = new GridAdapter(this, imgFiles);
        GridView gridView = findViewById(R.id.gridView);
        if (gridView != null) {
            gridView.setAdapter(adapter);
            gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    // Select Image
                }
            });
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