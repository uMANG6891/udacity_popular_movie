package com.umang.popularmovies.data;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Created by umang on 08/12/15.
 */
public class FetchAsyncData extends AsyncTask<List, Void, Void> {

    public static final String LOG_TAG = FetchAsyncData.class.getSimpleName();

    Context con;

    public static String GET_CAST;
    public static String GET_VIDEO_LINK;
    public static String GET_REVIEWS;
    public static String GET_SIMILAR_MOVIES;

    public FetchAsyncData(Context con) {
        this.con = con;
    }

    @Override
    protected Void doInBackground(List... params) {
        String URL;
        for (int i = 0; i < params[0].size(); i++) {
            URL = params[0].get(i).toString();
            if (URL.contains("/credits")) {
                GET_CAST = getFromInternet(URL);
            }
            if (URL.contains("/videos")) {
                GET_VIDEO_LINK = getFromInternet(URL);
            }
            if (URL.contains("/reviews")) {
                GET_REVIEWS = getFromInternet(URL);
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void d) {
        super.onPostExecute(d);
        String movieId = null;

        JSONObject CAST = null;
        String LINK = null;
        JSONArray JA_REVIEWS = null;

        if (GET_CAST != null) {
            try {
                JSONObject jo = new JSONObject(GET_CAST);
                movieId = jo.getString("id");

                JSONArray jaCast = new JSONArray(jo.getString("cast"));
                JSONArray jaCrew = new JSONArray(jo.getString("crew"));

                JSONArray actors = new JSONArray();
                for (int i = 0; i < jaCast.length() && i < 6; i++) {
                    jo = jaCast.getJSONObject(i);
                    actors.put(jo.getString("name"));
                }
                JSONObject crew = new JSONObject();
                for (int i = 0; i < jaCrew.length(); i++) {
                    if (jo.has("director") && jo.has("producer") && jo.has("writer")) {
                        break;
                    }
                    jo = jaCrew.getJSONObject(i);
                    if (jo.getString("job").equalsIgnoreCase("Director")) {
                        crew.put("director", jo.getString("name"));
                    } else if (jo.getString("job").equalsIgnoreCase("Producer")) {
                        crew.put("producer", jo.getString("name"));
                    } else if (jo.getString("job").equalsIgnoreCase("Screenplay")) {
                        crew.put("writer", jo.getString("name"));
                    }
                }
                CAST = new JSONObject();
                CAST.put("actors", actors);
                CAST.put("crew", crew);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (GET_VIDEO_LINK != null) {
            try {
                JSONObject jo = new JSONObject(GET_VIDEO_LINK);
                movieId = jo.getString("id");
                JSONArray ja = new JSONArray(jo.getString("results"));
                for (int i = 0; i < ja.length(); i++) {
                    jo = ja.getJSONObject(i);
                    if (jo.getString("type").equals("Trailer") && jo.getString("site").equals("YouTube")) {
                        LINK = jo.getString("key");
                        break;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (GET_REVIEWS != null) {
            try {
                JSONObject jo = new JSONObject(GET_REVIEWS);
                movieId = jo.getString("id");
                JSONArray ja = new JSONArray(jo.getString("results"));
                if (ja.length() > 0) {
                    JA_REVIEWS = new JSONArray();
                    JSONObject joUser;
                    for (int i = 0; i < ja.length() && i < 3; i++) {
                        jo = ja.getJSONObject(i);
                        joUser = new JSONObject();
                        joUser.put("author", jo.getString("author"));
                        joUser.put("content", jo.getString("content"));
                        JA_REVIEWS.put(joUser);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
//        case GET_SIMILAR_MOVIES:
//        break;
//        default:
//        break;
        if (movieId != null) {
            ContentValues values = new ContentValues();
            if (CAST != null)
                values.put(MovieContract.MovieEntry.COLUMN_CAST, CAST.toString());
            if (LINK != null)
                values.put(MovieContract.MovieEntry.COLUMN_VIDEO_LINK, LINK);
            if (JA_REVIEWS != null)
                values.put(MovieContract.MovieEntry.COLUMN_REVIEWS, JA_REVIEWS.toString());
            con.getContentResolver().update(MovieContract.MovieEntry.CONTENT_URI,
                    values,
                    MovieContract.MovieEntry.COLUMN_MOVIE_ID + " = ?",
                    new String[]{movieId});
        }
    }

    public static String getFromInternet(String MOVIE_URL) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        String data = null;

        try {
            Uri builtUri = Uri.parse(MOVIE_URL);

            URL url = new URL(builtUri.toString());

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            StringBuilder buffer = new StringBuilder();
            if (inputStream == null) {
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }

            if (buffer.length() == 0) {
                return null;
            }
            data = buffer.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return data;
    }
}
