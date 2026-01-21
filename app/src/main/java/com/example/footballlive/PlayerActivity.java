package com.example.footballlive;

import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlayerActivity extends AppCompatActivity {
    private PlayerView playerView;
    private ExoPlayer player;
    private ProgressBar progressBar;
    private TextView titleText;
    private ExecutorService executorService;
    private Handler checkHandler = new Handler();
    private Runnable checkVisibilityRunnable;
    private String streamUrl = "";
    private String matchId = "";
    private String clientIp = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
       
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
       
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
        
        setContentView(R.layout.activity_player);

        playerView = findViewById(R.id.playerView);
        progressBar = findViewById(R.id.progressBar);
        titleText = findViewById(R.id.titleText);

        matchId = getIntent().getStringExtra("match_id");
        clientIp = getIntent().getStringExtra("client_ip");
        String homeTeam = getIntent().getStringExtra("home_team");
        String awayTeam = getIntent().getStringExtra("away_team");

        titleText.setText(homeTeam + " vs " + awayTeam);

        executorService = Executors.newSingleThreadExecutor();

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        playerView.setKeepScreenOn(true);
        
       
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
               
                reconnectStream();
            }
            
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_ENDED) {
                   
                    reconnectStream();
                }
            }
        });
        
       
        startVisibilityCheck();

        getStreamUrl(matchId, clientIp);
    }

    private void reconnectStream() {
        runOnUiThread(() -> {
            Toast.makeText(PlayerActivity.this, "กำลังเชื่อมต่อใหม่...", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.VISIBLE);
        });
        
       
        checkHandler.postDelayed(() -> {
            if (!streamUrl.isEmpty()) {
                playStream(streamUrl);
            } else {
                getStreamUrl(matchId, clientIp);
            }
        }, 2000);
    }

    private void startVisibilityCheck() {
        checkVisibilityRunnable = new Runnable() {
            @Override
            public void run() {
                if (playerView.isControllerVisible()) {
                    if (titleText.getVisibility() != View.VISIBLE) {
                        titleText.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (titleText.getVisibility() != View.GONE) {
                        titleText.setVisibility(View.GONE);
                    }
                }
                checkHandler.postDelayed(this, 100);
            }
        };
        checkHandler.post(checkVisibilityRunnable);
    }

    private void getStreamUrl(String matchId, String clientIp) {
    progressBar.setVisibility(View.VISIBLE);
    
    executorService.execute(() -> {
        try {
            String apiUrl = "https://nutapi.work/api/get_stream/" + matchId + "?client_ip=" + clientIp;
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            
            conn.setRequestProperty("Origin", "https://168dooballth.com");
            conn.setRequestProperty("sec-ch-ua", "\"Android WebView\";v=\"143\", \"Chromium\";v=\"143\", \"Not A(Brand\";v=\"24\"");
            conn.setRequestProperty("sec-ch-ua-mobile", "?1");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("sec-ch-ua-platform", "\"Android\"");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setRequestProperty("Referer", "https://168dooballth.com/");
            
            int responseCode = conn.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                String responseStr = response.toString().trim();
                
                try {
                    JSONObject json = new JSONObject(responseStr);
                    
                    if (json.has("data")) {
                        streamUrl = json.getString("data");
                        
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            playStream(streamUrl);
                        });
                    } else {
                        throw new Exception("No 'data' key in response");
                    }
                    
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(PlayerActivity.this, "Error parsing response: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        finish();
                    });
                }
            } else {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(PlayerActivity.this, "Server error: " + responseCode, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(PlayerActivity.this, "Connection error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            });
        }
    });
}

    private void playStream(String url) {
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Referer", "https://168dooballth.com/");
            headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            
            DefaultHttpDataSource.Factory dataSourceFactory = new DefaultHttpDataSource.Factory()
                    .setDefaultRequestProperties(headers)
                    .setAllowCrossProtocolRedirects(true)
                    .setConnectTimeoutMs(10000)
                    .setReadTimeoutMs(10000);

            HlsMediaSource mediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(MediaItem.fromUri(Uri.parse(url)));

            player.setMediaSource(mediaSource);
            player.prepare();
            player.setPlayWhenReady(true);
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error playing stream", Toast.LENGTH_SHORT).show();
           
            checkHandler.postDelayed(this::reconnectStream, 3000);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null && !streamUrl.isEmpty()) {
            player.setPlayWhenReady(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
       
    }

    @Override
    protected void onStop() {
        super.onStop();
       
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        checkHandler.removeCallbacks(checkVisibilityRunnable);
        if (player != null) {
            player.release();
        }
        executorService.shutdown();
    }
}