package com.example.monitoringlocation;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.*;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private String token;

    LocationDatabaseHelper dbHelper ;

    private EditText tokenEditText;
    private Button confirmButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        dbHelper = new LocationDatabaseHelper(getApplicationContext());

        tokenEditText = findViewById(R.id.tokenEditText);
        confirmButton = findViewById(R.id.confirmButton);


        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                 token = tokenEditText.getText().toString();
                Handler handler = new Handler();
                Runnable sendLocationRunnable = new Runnable() {
                    @Override
                    public void run() {
                        locationCallback = new LocationCallback() {
                            @Override
                            public void onLocationResult(LocationResult locationResult) {
                                // Xử lý thông tin vị trí được trả về
                                Location location = locationResult.getLastLocation();
                                assert location != null;
                                double latitude = location.getLatitude();
                                double longitude = location.getLongitude();
                                Toast.makeText(getApplicationContext(), latitude + ":" + longitude, Toast.LENGTH_SHORT).show();

                                if (isConnectedToInternet()) {
                                    sendLocationInDatabase();
                                    sendLocationToServer(latitude, longitude);
                                } else {
                                    saveLocationToLocalStorage(latitude, longitude);
                                }
                            }
                        };

                        // Kiểm tra và yêu cầu quyền truy cập vị trí
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                                ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            LocationRequest locationRequest = LocationRequest.create();
                            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                            locationRequest.setInterval(5000); // Thời gian giữa các cập nhật vị trí (5 giây)
                            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
                            Toast.makeText(getApplicationContext(), " co quyen ", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "khong co quyen ", Toast.LENGTH_SHORT).show();
                            //yeu cau them quyen truy cap vi tri
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                        }
                        handler.postDelayed(this, 5000);
                    }
                };
                handler.postDelayed(sendLocationRunnable, 5000);
            }
        });


    }

    private boolean isConnectedToInternet() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void saveLocationToLocalStorage(double latitude, double longitude) {
        String time = getCurrentTime();
        dbHelper.insertLocation(new LocationData(token, time, latitude, longitude));
        Toast.makeText(getApplicationContext(), "Lưu vị trí vào CSDL cục bộ", Toast.LENGTH_SHORT).show();
    }

    private void sendLocationInDatabase(){
        //check xem data base co du lieu khong, neu co thi se gui du lieu co trong database truoc, sau do xoa
        Cursor cursor = dbHelper.getAllLocations();
        if (cursor != null && cursor.moveToFirst()) {
            int latitudeColumnIndex = cursor.getColumnIndex(LocationDatabaseHelper.COLUMN_LATITUDE);
            int longitudeColumnIndex = cursor.getColumnIndex(LocationDatabaseHelper.COLUMN_LONGITUDE);
            int tokenColumnIndex = cursor.getColumnIndex(LocationDatabaseHelper.COLUMN_TOKEN);
            int timeColumnIndex = cursor.getColumnIndex(LocationDatabaseHelper.COLUMN_TIME);
            List<LocationData> listLocation = new ArrayList<>();

            while (!cursor.isAfterLast()) {
                double latitude = cursor.getDouble(latitudeColumnIndex);
                double longitude = cursor.getDouble(longitudeColumnIndex);
                String token = cursor.getString(tokenColumnIndex);
                String time = cursor.getString(timeColumnIndex);
                listLocation.add(new LocationData(token, time, latitude, longitude));
                cursor.moveToNext();
            }
            // Khởi tạo Retrofit
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://b605-113-161-41-60.ngrok-free.app/monitoring-location/api/v1/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            Toast.makeText(getApplicationContext(), " send database", Toast.LENGTH_SHORT).show();

            // Tạo interface cho các yêu cầu API
            LocationAPI locationAPI = retrofit.create(LocationAPI.class);

            // Thực hiện yêu cầu POST dữ liệu vị trí
            Call<Void> call = locationAPI.sendListLocation(listLocation);
            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {

                    if (response.isSuccessful()) {
                        // Xử lý thành công
                        Toast.makeText(getApplicationContext(), " ket noi thanh cong", Toast.LENGTH_SHORT).show();
                    } else {
                        // Xử lý lỗi
                        int statusCode = response.code();
                        String errorMessage = "Yêu cầu không thành công. Mã phản hồi: " + statusCode;

                        // Xử lý lỗi dựa trên mã phản hồi
                        if (statusCode == 401) {
                            // Lỗi xác thực
                            errorMessage = "Lỗi xác thực. Vui lòng đăng nhập lại.";
                        } else if (statusCode == 404) {
                            // Lỗi không tìm thấy tài nguyên
                            errorMessage = "Không tìm thấy tài nguyên yêu cầu.";
                        }
                        Toast.makeText(getApplicationContext(), String.valueOf(statusCode), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    // Xử lý lỗi kết nối
                    Toast.makeText(getApplicationContext(), " lỗi kết nối", Toast.LENGTH_SHORT).show();
                }
            });

        }

    }

 //   private void send(Ca)

    private void sendLocationToServer(double latitude, double longitude) {

        // Lấy thời gian hiện tại
        String time = getCurrentTime();
        Toast.makeText(getApplicationContext(), time, Toast.LENGTH_SHORT).show();

        // Tạo đối tượng LocationData từ dữ liệu
        LocationData locationData = new LocationData(token, time, latitude, longitude);

        // Khởi tạo Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://d1cf-113-161-41-79.ngrok-free.app/monitoring-location/api/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Tạo interface cho các yêu cầu API
        LocationAPI locationAPI = retrofit.create(LocationAPI.class);

        // Thực hiện yêu cầu POST dữ liệu vị trí
        Call<Void> call = locationAPI.sendLocation(locationData);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {

                if (response.isSuccessful()) {
                    // Xử lý thành công
                    Toast.makeText(getApplicationContext(), " ket noi thanh cong", Toast.LENGTH_SHORT).show();
                } else {
                    // Xử lý lỗi
                    int statusCode = response.code();
                    String errorMessage = "Yêu cầu không thành công. Mã phản hồi: " + statusCode;

                    // Xử lý lỗi dựa trên mã phản hồi
                    if (statusCode == 401) {
                        // Lỗi xác thực
                        errorMessage = "Lỗi xác thực. Vui lòng đăng nhập lại.";
                    } else if (statusCode == 404) {
                        // Lỗi không tìm thấy tài nguyên
                        errorMessage = "Không tìm thấy tài nguyên yêu cầu.";
                    }
                    Toast.makeText(getApplicationContext(), String.valueOf(statusCode), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Xử lý lỗi kết nối
                Toast.makeText(getApplicationContext(), " lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getCurrentTime() {
        DateTime currentTime = DateTime.now();
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss");
        return currentTime.toString(formatter);
    }

    public static void main(String[] args) {
    }

    // Lấy Advertising ID
    public String getAdvertisingId() {
        return "1";
    }

}