package com.peterzaki.movieapp;



import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.linearlistview.LinearListView;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by peterzaki on 4/16/2016.
 */
public class MovieDetailsFragment extends Fragment implements View.OnClickListener, LinearListView.OnItemClickListener{
    TextView title;
    ImageView poster;
    TextView releasedTime;
    TextView time;
    Button favourite;
    TextView overview;
    MyMovie movie;
    TextView voteAverage;

    LinearListView trailers;
    LinearListView reviewsListView;

    List<Trailer> trailerList;
    List<Review> reviewList;

    DatabaseAdapter db;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.movie_details, container, false);
        title = (TextView) v.findViewById(R.id.movieTitle);
        releasedTime = (TextView) v.findViewById(R.id.movieReleasedTime);
        time = (TextView) v.findViewById(R.id.movieTime);
        overview = (TextView) v.findViewById(R.id.movieOverview);
        poster = (ImageView) v.findViewById(R.id.movieImage);
        favourite = (Button) v.findViewById(R.id.favouriteButton);
        favourite.setOnClickListener(this);
        voteAverage = (TextView) v.findViewById(R.id.movieVoteAverage);

        trailers = (LinearListView) v.findViewById(R.id.trailersList);
        reviewsListView = (LinearListView) v.findViewById(R.id.reviewsList);

        db = new DatabaseAdapter(getActivity());

        movie = null;

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);


    }

    public void change(MyMovie movie) {
        title.setText(movie.title);
        releasedTime.setText(movie.releaseDate.substring(0, 4));
        time.setText(movie.time + " min");
        overview.setText(movie.overview);
        voteAverage.setText(movie.vote_average + "");
        Picasso.with(getActivity()).load(movie.poster).into(poster);
        this.movie = movie;


        if(db.search(movie.id) == 1){
            favourite.setBackgroundColor(Color.parseColor("#FFD700"));
            favourite.setText("It is a\nfavourite");
        }
        else{
            favourite.setBackgroundColor(Color.parseColor("#00aadd"));
            favourite.setText("make as \nfavourite");
        }


        FetchTrailers fetchTrailers = new FetchTrailers();
        trailerList = null;
        try {
            trailerList = fetchTrailers.execute(movie.id).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        List<String> trailersNames = new ArrayList<>();
        for (Trailer t : trailerList) {
            trailersNames.add(t.name);
        }

        ArrayAdapter adapter = new ArrayAdapter(getActivity(), R.layout.trailer, R.id.trailerName, trailersNames);
        trailers.setAdapter(adapter);
        trailers.setOnItemClickListener(this);







        FetchReviews fetchReviews = new FetchReviews();
        reviewList = null;
        try {
            reviewList = fetchReviews.execute(movie.id).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        List<String> authors = new ArrayList<>();
        List<String> contents = new ArrayList<>() ;


        for (Review r : reviewList) {
            authors.add(r.author);
            contents.add(r.content);
        }
       ReviewAdapter reviewAdapter = new ReviewAdapter();
        reviewsListView.setAdapter(reviewAdapter);
        reviewsListView.setOnItemClickListener(this);



    }

    @Override
    public void onItemClick(LinearListView parent, View view, int position, long id) {
        if (parent.getId() == R.id.trailersList) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(trailerList.get(position).url));
            intent = Intent.createChooser(intent, "");
            startActivity(intent);
        }
        else if(parent.getId() == R.id.reviewsList){

        }
    }

    class ReviewAdapter extends BaseAdapter{


        @Override
        public int getCount() {
            return reviewList.size();
        }

        @Override
        public Object getItem(int position) {
            return reviewList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ReviewHolder holder = null;
            View v = convertView;
            if (v == null){
                LayoutInflater inflater = MovieDetailsFragment.this.getActivity().getLayoutInflater();
                v = inflater.inflate(R.layout.review, parent, false);
                holder = new ReviewHolder(v);
                v.setTag(holder);
            }
            else{
                holder = (ReviewHolder) convertView.getTag();
            }
            holder.author.setText(reviewList.get(position).author);
            holder.content.setText(reviewList.get(position).content);
            return v;
        }
    }

    class ReviewHolder{
        TextView author;
        TextView content;

        ReviewHolder(View v){
            author = (TextView) v.findViewById(R.id.authorTextView);
            content = (TextView) v.findViewById(R.id.reviewTextView);
        }
    }


    @Override
    public void onClick(View v) {



        if (v.getId() == R.id.favouriteButton) {
            int flag = db.search(movie.id);

            if (flag == 0) {
                long id = db.insert(movie.id, movie.poster);
                favourite.setBackgroundColor(Color.parseColor("#FFD700"));
                favourite.setText("It is a\nfavourite");
            } else if (flag == 1) {
                favourite.setBackgroundColor(Color.parseColor("#2299ff"));
                favourite.setText("make as \nfavourite");
                db.deleteName(movie.id);
            }
        }
    }




}

class FetchTrailers extends AsyncTask<Integer, Void, List<Trailer>> {

    @Override
    protected List<Trailer> doInBackground(Integer... params) {
        String base = "https://api.themoviedb.org/3/movie/" + params[0] + "/videos?";
        String json = getJSON(base);

        return getTrailers(json);
    }

    private List<Trailer> getTrailers(String json) {
        List<Trailer> trailers = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONArray results = jsonObject.getJSONArray("results");
            for (int i = 0; i < results.length(); i++) {
                JSONObject resultsJSONObject = results.getJSONObject(i);
                String key = resultsJSONObject.getString("key");
                String name = resultsJSONObject.getString("name");
                Trailer trailer = new Trailer(name, "https://www.youtube.com/watch?v=" + key);
                trailers.add(trailer);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return trailers;
    }

    public String getJSON(String base) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String jsonstr = null;


        String api_key = "c9d5b152a06f9f0ebceb82fbace5c84c";
        try {
            String baseUrl = base;

            final String API_param = "api_key";
            Uri builtUri = Uri.parse(baseUrl).buildUpon()
                    .appendQueryParameter(API_param, api_key).build();

            URL url = new URL(builtUri.toString());
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            if (inputStream == null) {
                return null;
            }

            StringBuffer buffer = new StringBuffer();

            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }
            if (buffer.length() == 0) {
                return null;
            }
            jsonstr = buffer.toString();
            return jsonstr.toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
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
        return null;
    }
}

class Trailer {
    String name;
    String url;

    public Trailer(String name, String url) {
        this.name = name;
        this.url = url;
    }

}

class FetchReviews extends AsyncTask<Integer, Void, List<Review>> {


    @Override
    protected List<Review> doInBackground(Integer... params) {
        String base = "https://api.themoviedb.org/3/movie/" + params[0] + "/reviews?";
        String json = getJSON(base);

        return getTrailers(json);
    }

    private List<Review> getTrailers(String json) {
        List<Review> reviews = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONArray results = jsonObject.getJSONArray("results");
            for (int i = 0; i < results.length(); i++) {
                JSONObject resultsJSONObject = results.getJSONObject(i);
                String author = resultsJSONObject.getString("author");
                String content = resultsJSONObject.getString("content");
                Review review = new Review(author, content);
                reviews.add(review);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return reviews;
    }

    public String getJSON(String base) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String jsonstr = null;


        String api_key = "c9d5b152a06f9f0ebceb82fbace5c84c";
        try {
            String baseUrl = base;

            final String API_param = "api_key";
            Uri builtUri = Uri.parse(baseUrl).buildUpon()
                    .appendQueryParameter(API_param, api_key).build();

            URL url = new URL(builtUri.toString());
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            if (inputStream == null) {
                return null;
            }

            StringBuffer buffer = new StringBuffer();

            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }
            if (buffer.length() == 0) {
                return null;
            }
            jsonstr = buffer.toString();
            return jsonstr.toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
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
        return null;
    }
}

class Review {
     String author;
    String content;
    public Review(String author, String content){
        this.author = author;
        this.content = content;
    }
}

