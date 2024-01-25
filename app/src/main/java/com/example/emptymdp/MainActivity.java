package com.example.emptymdp;

import static android.app.PendingIntent.getActivity;

import static java.security.AccessController.getContext;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;

    HomeFragment homeFragment = new HomeFragment();
    BluetoothFragment bluetoothFragment = new BluetoothFragment();
    TestFragment testFragment = new TestFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission();

        if (!BluetoothPermissions.checkBluetoothConnectionPermission(this)) {
            BluetoothPermissions.requestBluetoothPermissions(MainActivity.this);
        }

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        getSupportFragmentManager().beginTransaction().replace(R.id.flFragment, homeFragment).commit();

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                if (item.getItemId() == R.id.miHome){
                    getSupportFragmentManager().beginTransaction().replace(R.id.flFragment, homeFragment).commit();
                    return true;
                } else if (item.getItemId() == R.id.miBluetooth){
                    getSupportFragmentManager().beginTransaction().replace(R.id.flFragment, bluetoothFragment).commit();
                    return true;
                } else if (item.getItemId() == R.id.miTestFragment){
                    getSupportFragmentManager().beginTransaction().replace(R.id.flFragment, testFragment).commit();
                    return true;
                }

                return false;
            }
        });
    }

    public void checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            } else {
                ActivityCompat.requestPermissions(this, new String[]{
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION,}, 1);
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
        } else {
            checkPermission();
        }
    }
}