package com.purpletea.cameratoraw;

import android.app.ProgressDialog;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.purpletea.cameratoraw.databinding.ActivityShowRawBinding;
import com.purpletea.cameratoraw.network.ImageInterface;
import com.purpletea.cameratoraw.network.ResponseModel;
import com.purpletea.cameratoraw.network.RetrofitRequset;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShowRawActivity extends AppCompatActivity {


    String path;
    Uri uri;
    Call<ResponseModel> call;

    FrameLayout rawContainer;
    ActivityShowRawBinding binding;
    ProgressDialog progressDialog;
    String newUrl;
    RequestOptions requestOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_show_raw);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading please wait...!");
        if (getIntent().getExtras() != null) {
            path = getIntent().getExtras().getString("path");
            uri = (Uri) getIntent().getParcelableExtra("uri");
        }
        Toast.makeText(this, "Loading raw image", Toast.LENGTH_SHORT).show();
        requestOptions = new RequestOptions();
        requestOptions.diskCacheStrategy(DiskCacheStrategy.NONE);
        requestOptions.skipMemoryCache(true);
        Glide.with(this).load(path).apply(requestOptions).into(binding.imgRaw);

        binding.btnEnhance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               new SendFileTask().execute();
            }
        });

        binding.btncompare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Glide.with(ShowRawActivity.this).load(path).apply(requestOptions).into(binding.imgCompareOld);
                Glide.with(ShowRawActivity.this).load(newUrl).apply(requestOptions).
                        into(binding.imgCompareNew);
                binding.newImageContainer.setVisibility(View.GONE);
                binding.comparisonContainer.setVisibility(View.VISIBLE);
            }
        });

    }

    public void uploadFile() {
        progressDialog.show();
        RequestBody desc = RequestBody.create(MultipartBody.FORM, "test.ARW");
        File file = new File(path);
        RequestBody filePart = RequestBody.create(MediaType.parse(getContentResolver().getType(uri)), new File(path));
        MultipartBody.Part fileObj = MultipartBody.Part.createFormData("image", file.getName(), filePart);
        ImageInterface imageInterface = RetrofitRequset.getRetrofit().create(ImageInterface.class);
        call = imageInterface.uploadImage(desc, fileObj);
        call.enqueue(new Callback<ResponseModel>() {
            @Override
            public void onResponse(Call<ResponseModel> call, Response<ResponseModel> response) {
                progressDialog.dismiss();
                Log.e("response", response.raw().toString());
                if (response.body() != null) {
                    ResponseModel responseModel = response.body();
                    if (responseModel.getUrl() != null) {
                        Glide.with(ShowRawActivity.this).load(responseModel.getUrl()).apply(requestOptions).into(binding.imgNew);
                        binding.rawContainer.setVisibility(View.GONE);
                        binding.newImageContainer.setVisibility(View.VISIBLE);
                        newUrl = responseModel.getUrl();
                    } else {
                        Toast.makeText(ShowRawActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseModel> call, Throwable t) {
                progressDialog.dismiss();
                Log.e("response", Log.getStackTraceString(t));
                Toast.makeText(ShowRawActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
            }
        });
    }

    String filename;
    class SendFileTask extends AsyncTask<Void, Void, Void>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            sendFileToServer();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressDialog.dismiss();
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(response);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Toast.makeText(ShowRawActivity.this, jsonObject.toString(), Toast.LENGTH_SHORT).show();
            if(responseCode == 200){
                String imageUrl = jsonObject.optString("url");
                Glide.with(ShowRawActivity.this).load(imageUrl).apply(requestOptions).into(binding.imgNew);
                binding.rawContainer.setVisibility(View.GONE);
                binding.newImageContainer.setVisibility(View.VISIBLE);
                newUrl = imageUrl;
            }
        }
    }
    int responseCode;
    String response;

    public String sendFileToServer() {
        String response = "error";
        HttpURLConnection connection = null;
        DataOutputStream outputStream = null;
        // DataInputStream inputStream = null;

        String pathToOurFile = path;
        String urlServer = Constants.base_url+"/convert";
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        DateFormat df = new SimpleDateFormat("yyyy_MM_dd_HH:mm:ss");

        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024;
        try {
            FileInputStream fileInputStream = new FileInputStream(new File(
                    path));

            URL url = new URL(urlServer);
            connection = (HttpURLConnection) url.openConnection();

            // Allow Inputs & Outputs
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setChunkedStreamingMode(1024);
            // Enable POST method
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(500000);
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type",
                    "multipart/form-data;boundary=" + boundary);

            outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(twoHyphens + boundary + lineEnd);

            String connstr = null;
            connstr = "Content-Disposition: form-data; name=\"image\";filename=\""
                    + "image.DNG" + "\"" + lineEnd;
            Log.i("Connstr", connstr);

            outputStream.writeBytes(connstr);
            outputStream.writeBytes(lineEnd);

            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            // Read file
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            Log.e("Image length", bytesAvailable + "");
            try {
                while (bytesRead > 0) {
                    try {
                        outputStream.write(buffer, 0, bufferSize);
                    } catch (OutOfMemoryError e) {
                        e.printStackTrace();
                        response = "outofmemoryerror";
                        return response;
                    }
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }
            } catch (Exception e) {
                e.printStackTrace();
                response = "error";
                return response;
            }
            outputStream.writeBytes(lineEnd);
            outputStream.writeBytes(twoHyphens + boundary + twoHyphens
                    + lineEnd);

            // Responses from the server (code and message)
            int serverResponseCode = connection.getResponseCode();
            String serverResponseMessage = connection.getResponseMessage();
            Log.i("Server Response Code ", "" + serverResponseCode);


            if (serverResponseCode == 200) {
                response = "true";
            }
            BufferedReader bufferedReader;
            if (200 <= serverResponseCode && serverResponseCode <= 399) {
                bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            } else {
                bufferedReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            }
            responseCode = serverResponseCode;
            String line = "";
            StringBuilder linebuilder = new StringBuilder();
            while((line = bufferedReader.readLine()) != null){
                linebuilder.append(line);
            }
            this.response = linebuilder.toString();
            Log.i("Server Response Message", linebuilder.toString());
            String CDate = null;
            Date serverTime = new Date(connection.getDate());
            try {
                CDate = df.format(serverTime);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("Date Exception", e.getMessage() + " Parse Exception");
            }
            Log.i("Server Response Time", CDate + "");

//            filename = CDate
//                    + filename.substring(filename.lastIndexOf("."),
//                    filename.length());
//            Log.i("File Name in Server : ", filename);

            fileInputStream.close();
            outputStream.flush();
            outputStream.close();
            outputStream = null;
        } catch (Exception ex) {
            // Exception handling
            response = "error";
            Log.e("Send file Exception", ex.getMessage() + "");
            ex.printStackTrace();
        }
        return response;
    }
}
