package com.example.henry.newsreader;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> articles = new ArrayList<String>();
    ArrayList<String> content = new ArrayList<String>();

    ArrayAdapter arrayAdapter;

    SQLiteDatabase articlesDatabase;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        articlesDatabase = this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);
        articlesDatabase.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId INTEGER, title VARCHAR, content VARCHAR)");

        DownloadTask downloadTask = new DownloadTask();

        try {
            //downloadTask.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        } catch (Exception e) {
            e.printStackTrace();
        }
        ListView listView = findViewById(R.id.listView);


        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, articles);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(), newsActivity.class);
                intent.putExtra("content", content.get(i));
                startActivity(intent);
            }
        });
        updateListView();
    }

    public void updateListView() {
        Cursor c = articlesDatabase.rawQuery("SELECT * FROM articles" , null);

        int contentIndex = c.getColumnIndex("content");
        int titleIndex = c.getColumnIndex("title");

        if (c.moveToFirst()) {
            articles.clear();
            content.clear();

            do {
                articles.add(c.getString(titleIndex));
                content.add(c.getString(contentIndex));
            } while (c.moveToNext());

            arrayAdapter.notifyDataSetChanged();
        }

    }



    public class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            URL url;
            String result = "";
            HttpURLConnection urlConnection = null;

            try {
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();

                InputStream inputStream = urlConnection.getInputStream();

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);

                int data = inputStreamReader.read();

                while (data != -1) {
                    char curr = (char) data;

                    result += curr;

                    data = inputStreamReader.read();
                }
                JSONArray jsonArray = new JSONArray(result);

                int numOfArticles = 20;
                if (jsonArray.length() < 20) {
                    numOfArticles = jsonArray.length();
                }
                articlesDatabase.execSQL("DELETE FROM articles");
                for (int i = 0; i < numOfArticles; i++) {
                    String articleId = jsonArray.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articleId + ".json?print=pretty");
                    urlConnection = (HttpURLConnection) url.openConnection();

                    inputStream = urlConnection.getInputStream();

                    inputStreamReader = new InputStreamReader(inputStream);

                    data = inputStreamReader.read();

                    String articleInfo = "";
                    while (data != -1) {
                        char curr = (char) data;

                        articleInfo += curr;

                        data = inputStreamReader.read();
                    }
                    JSONObject jsonObject = new JSONObject(articleInfo);

                    if (!jsonObject.isNull("title") && !jsonObject.isNull("url")) {
                        String title = jsonObject.getString("title");
                        String articleUrl = jsonObject.getString("url");

                        url = new URL(articleUrl);

                        urlConnection = (HttpURLConnection) url.openConnection();

                        inputStream = urlConnection.getInputStream();
                        inputStreamReader = new InputStreamReader(inputStream);
                        data = inputStreamReader.read();
                        String articleHtml = "";
                        while (data != -1) {
                            char curr = (char) data;
                            articleHtml += curr;
                            data = inputStreamReader.read();
                        }
                        Log.i("HTML" , articleHtml);

                        String sql = "INSERT INTO articles (articleId, title, content) VALUES (?, ?, ?)";
                        SQLiteStatement statement = articlesDatabase.compileStatement(sql);
                        statement.bindString(1, articleId);
                        statement.bindString(2, title);
                        statement.bindString(3, articleHtml);

                        statement.execute();

                    }
                }

                Log.i("URL Content" , result);

                return result;

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListView();

        }
    }
}
