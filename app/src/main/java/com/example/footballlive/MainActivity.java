package com.example.footballlive;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MatchAdapter adapter;
    private ProgressBar progressBar;
    private List<Match> matchList = new ArrayList<>();
    private Handler refreshHandler = new Handler();
    private ExecutorService executorService;
    private String clientIp = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);

        androidx.recyclerview.widget.LinearLayoutManager layoutManager = 
            new androidx.recyclerview.widget.LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        
        adapter = new MatchAdapter(matchList);
        recyclerView.setAdapter(adapter);

        executorService = Executors.newCachedThreadPool();

        getClientIp();
        startAutoRefresh();
    }

    private void getClientIp() {
        executorService.execute(() -> {
            try {
                URL url = new URL("https://slave03.appball.vip/get-my-ip");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                JSONObject json = new JSONObject(response.toString());
                clientIp = json.getString("ip");
                
                loadMatches();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Error getting IP: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void loadMatches() {
        runOnUiThread(() -> progressBar.setVisibility(View.VISIBLE));
        
        executorService.execute(() -> {
            try {
                URL url = new URL("https://nutapi.work/api/getball/1");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                
                conn.setRequestProperty("Origin", "https://168dooballth.com");
                conn.setRequestProperty("sec-ch-ua", "\"Android WebView\";v=\"143\", \"Chromium\";v=\"143\", \"Not A(Brand\";v=\"24\"");
                conn.setRequestProperty("sec-ch-ua-mobile", "?1");
                conn.setRequestProperty("Accept", "*/*");
                conn.setRequestProperty("sec-ch-ua-platform", "\"Android\"");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                conn.setRequestProperty("Referer", "https://168dooballth.com/");
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                JSONArray mainArray = new JSONArray(response.toString());
                
                List<Match> newMatches = new ArrayList<>();
                Date currentDate = new Date();
                Calendar currentCal = Calendar.getInstance();
                currentCal.setTime(currentDate);
                
                for (int i = 0; i < mainArray.length(); i++) {
                    JSONObject dayObject = mainArray.getJSONObject(i);
                    
                    if (dayObject.has("match_day")) {
                        JSONArray matchDayArray = dayObject.getJSONArray("match_day");
                        
                        for (int j = 0; j < matchDayArray.length(); j++) {
                            JSONObject matchObj = matchDayArray.getJSONObject(j);
                            Match match = new Match();
                            
                            match.id = matchObj.optString("match_id", "");
                            match.league = matchObj.optString("league_name_th", 
                                         matchObj.optString("league_name_en", "Unknown League"));
                            match.leagueImage = matchObj.optString("league_image", "");
                            match.homeTeam = matchObj.optString("teama_name_th",
                                           matchObj.optString("teama_name_en", "Home"));
                            match.awayTeam = matchObj.optString("teamb_name_th",
                                           matchObj.optString("teamb_name_en", "Away"));
                            match.homeImage = matchObj.optString("teama_img", "");
                            match.awayImage = matchObj.optString("teamb_img", "");
                            match.matchTime = matchObj.optString("time_start", "");
                            match.status = matchObj.optString("match_status", "");
                            match.score = matchObj.optString("score", "");
                            match.tv = matchObj.optInt("tv", 0);
                            match.tvVip = matchObj.optInt("tv_vip", 0);
                            
                            match.matchDate = null;
                            if (!match.matchTime.isEmpty()) {
                                try {
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                                    match.matchDate = sdf.parse(match.matchTime);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            
                            match.isLive = false;
                            
                            if (match.matchDate != null) {
                                Calendar matchCal = Calendar.getInstance();
                                matchCal.setTime(match.matchDate);
                                
                                boolean isSameDay = (currentCal.get(Calendar.YEAR) == matchCal.get(Calendar.YEAR)) &&
                                                  (currentCal.get(Calendar.DAY_OF_YEAR) == matchCal.get(Calendar.DAY_OF_YEAR));
                                
                                long timeDiffMillis = currentDate.getTime() - match.matchDate.getTime();
                                long timeDiffMinutes = timeDiffMillis / (1000 * 60);
                                
                                if (isSameDay && timeDiffMinutes >= -10 && timeDiffMinutes <= 120) {
                                    try {
                                        if (!match.status.isEmpty()) {
                                            int statusCode = Integer.parseInt(match.status);
                                            if (statusCode >= 0 && statusCode <= 90) {
                                                match.isLive = true;
                                            }
                                        }
                                    } catch (NumberFormatException e) {
                                        if (timeDiffMinutes >= 0 && timeDiffMinutes <= 110) {
                                            match.isLive = true;
                                        }
                                    }
                                }
                            }
                            
                            //เปิดทุกอัน
                            match.hasStream = (match.tv > 0 || match.tvVip > 0);
                            //match.hasStream = true;
                            
                            newMatches.add(match);
                        }
                    }
                }
                
                Collections.sort(newMatches, new Comparator<Match>() {
                    @Override
                    public int compare(Match m1, Match m2) {
                        if (m1.isLive && !m2.isLive) return -1;
                        if (!m1.isLive && m2.isLive) return 1;
                        
                        if (m1.matchDate != null && m2.matchDate != null) {
                            return m1.matchDate.compareTo(m2.matchDate);
                        }
                        
                        if (m1.matchDate == null && m2.matchDate != null) return 1;
                        if (m1.matchDate != null && m2.matchDate == null) return -1;
                        
                        return 0;
                    }
                });
                
                runOnUiThread(() -> {
                    matchList.clear();
                    matchList.addAll(newMatches);
                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                    
                    if (newMatches.isEmpty()) {
                        Toast.makeText(MainActivity.this, "No matches found", Toast.LENGTH_SHORT).show();
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void startAutoRefresh() {
        refreshHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loadMatches();
                refreshHandler.postDelayed(this, 60000);
            }
        }, 60000);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_UP ||
            keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            return super.onKeyDown(keyCode, event);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        refreshHandler.removeCallbacksAndMessages(null);
        executorService.shutdown();
    }

    class Match {
        String id;
        String league;
        String leagueImage;
        String homeTeam;
        String awayTeam;
        String homeImage;
        String awayImage;
        String matchTime;
        String status;
        String score;
        int tv;
        int tvVip;
        boolean isLive;
        boolean hasStream;
        Date matchDate;
    }

    class MatchAdapter extends RecyclerView.Adapter<MatchAdapter.ViewHolder> {
        private List<Match> matches;

        MatchAdapter(List<Match> matches) {
            this.matches = matches;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_match, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Match match = matches.get(position);
            
            holder.leagueName.setText(match.league);
            holder.homeTeamName.setText(match.homeTeam);
            holder.awayTeamName.setText(match.awayTeam);
            
            if (!match.score.isEmpty() && !match.score.equals("0-0")) {
                holder.scoreText.setVisibility(View.VISIBLE);
                holder.scoreText.setText(match.score);
                holder.vsText.setVisibility(View.GONE);
            } else if (!match.score.isEmpty() && match.score.equals("0-0") && match.isLive) {
               
                holder.scoreText.setVisibility(View.VISIBLE);
                holder.scoreText.setText(match.score);
                holder.vsText.setVisibility(View.GONE);
            } else {
               
                holder.scoreText.setVisibility(View.GONE);
                holder.vsText.setVisibility(View.VISIBLE);
            }
            
            
            if (!match.leagueImage.isEmpty()) {
                String imageUrl = match.leagueImage;
                if (!imageUrl.startsWith("http")) {
                    imageUrl = "https://nutapi.work/" + imageUrl;
                }
                Glide.with(holder.itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_league_placeholder)
                        .into(holder.leagueImage);
            }
            
            
            if (!match.homeImage.isEmpty()) {
                String imageUrl = match.homeImage;
                if (!imageUrl.startsWith("http")) {
                    imageUrl = "https://nutapi.work/" + imageUrl;
                }
                Glide.with(holder.itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_team_placeholder)
                        .into(holder.homeTeamImage);
            }
            
            
            if (!match.awayImage.isEmpty()) {
                String imageUrl = match.awayImage;
                if (!imageUrl.startsWith("http")) {
                    imageUrl = "https://nutapi.work/" + imageUrl;
                }
                Glide.with(holder.itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_team_placeholder)
                        .into(holder.awayTeamImage);
            }
            
            
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                Date date = inputFormat.parse(match.matchTime);
                if (date != null) {
                    String formattedTime = outputFormat.format(date);
                    holder.matchTime.setText(formattedTime);
                }
            } catch (Exception e) {
                holder.matchTime.setText(match.matchTime);
            }
            
           
            if (match.isLive) {
                holder.liveIndicator.setVisibility(View.VISIBLE);
                startBlinkAnimation(holder.liveIndicator);
                
                
                if (!match.status.isEmpty()) {
                    try {
                        int minute = Integer.parseInt(match.status);
                        if (minute >= 0 && minute <= 90) {
                            holder.matchStatus.setVisibility(View.VISIBLE);
                            holder.matchStatus.setText(minute + "'");
                        } else {
                            holder.matchStatus.setVisibility(View.GONE);
                        }
                    } catch (NumberFormatException e) {
                        holder.matchStatus.setVisibility(View.GONE);
                    }
                } else {
                    holder.matchStatus.setVisibility(View.GONE);
                }
            } else {
                holder.liveIndicator.setVisibility(View.GONE);
                holder.matchStatus.setVisibility(View.GONE);
            }
            
           
            if (match.hasStream) {
                holder.cardView.setEnabled(true);
                holder.cardView.setAlpha(1.0f);
                holder.cardView.setOnClickListener(v -> {
                    Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                    intent.putExtra("match_id", match.id);
                    intent.putExtra("client_ip", clientIp);
                    intent.putExtra("home_team", match.homeTeam);
                    intent.putExtra("away_team", match.awayTeam);
                    startActivity(intent);
                });
            } else {
                holder.cardView.setEnabled(false);
                holder.cardView.setAlpha(0.5f);
                holder.cardView.setOnClickListener(v -> {
                    Toast.makeText(MainActivity.this, "ไม่มีการถ่ายทอดสดสำหรับแมทช์นี้", Toast.LENGTH_SHORT).show();
                });
            }
        }

        private void startBlinkAnimation(View view) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.3f, 1f);
            animator.setDuration(1500);
            animator.setRepeatCount(ObjectAnimator.INFINITE);
            animator.start();
        }

        @Override
        public int getItemCount() {
            return matches.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            CardView cardView;
            ImageView leagueImage, homeTeamImage, awayTeamImage;
            TextView leagueName, homeTeamName, awayTeamName, matchTime, liveIndicator, scoreText, vsText, matchStatus;

            ViewHolder(View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.cardView);
                leagueImage = itemView.findViewById(R.id.leagueImage);
                leagueName = itemView.findViewById(R.id.leagueName);
                homeTeamImage = itemView.findViewById(R.id.homeTeamImage);
                homeTeamName = itemView.findViewById(R.id.homeTeamName);
                awayTeamImage = itemView.findViewById(R.id.awayTeamImage);
                awayTeamName = itemView.findViewById(R.id.awayTeamName);
                matchTime = itemView.findViewById(R.id.matchTime);
                liveIndicator = itemView.findViewById(R.id.liveIndicator);
                scoreText = itemView.findViewById(R.id.scoreText);
                vsText = itemView.findViewById(R.id.vsText);
                matchStatus = itemView.findViewById(R.id.matchStatus);
            }
        }
    }
}