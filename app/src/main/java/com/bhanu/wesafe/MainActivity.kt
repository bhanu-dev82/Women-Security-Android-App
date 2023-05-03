package com.bhanu.wesafe

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telephony.SmsManager
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

        private lateinit var addContactButton: Button
        private lateinit var removeContactButton: Button
        private lateinit var contact1: EditText
        private lateinit var contact2: EditText
        private lateinit var contact3: EditText
        private lateinit var emergencyContacts: ArrayList<String>
        private lateinit var sharedPreferences: SharedPreferences
        private lateinit var locationManager: LocationManager
        private lateinit var locationListener: LocationListener
        private var volumeButtonPressedTime: Long = 0
        private var location: Location? = null


    override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            // Initialize UI elements
            addContactButton = findViewById(R.id.add_contact_button)
            removeContactButton = findViewById(R.id.remove_contact_button)
            contact1 = findViewById(R.id.contact1)
            contact2 = findViewById(R.id.contact2)
            contact3 = findViewById(R.id.contact3)

            // Initialize emergency contacts
            emergencyContacts = ArrayList()

            // Initialize SharedPreferences
            sharedPreferences = getSharedPreferences("EmergencyContacts", Context.MODE_PRIVATE)

            // Load saved contacts from SharedPreferences
            val contact1Saved = sharedPreferences.getString("contact1", "")
            val contact2Saved = sharedPreferences.getString("contact2", "")
            val contact3Saved = sharedPreferences.getString("contact3", "")
            if (contact1Saved != "") {
                emergencyContacts.add(contact1Saved!!)
                contact1.setText(contact1Saved)
            }
            if (contact2Saved != "") {
                emergencyContacts.add(contact2Saved!!)
                contact2.setText(contact2Saved)
            }
            if (contact3Saved != "") {
                emergencyContacts.add(contact3Saved!!)
                contact3.setText(contact3Saved)
            }

            // Show saved contacts in a Toast message
            showSavedContacts()

            // Check for location permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1
                )
            }

            // Check for SMS permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.SEND_SMS), 2
                )
            }

            // Initialize location manager and listener
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    // Do nothing
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                    // Do nothing
                }

                override fun onProviderEnabled(provider: String) {
                    // Do nothing
                }

                override fun onProviderDisabled(provider: String) {
                    // Prompt user to enable location services
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                }
            }

            // Set click listener for add contact button
            addContactButton.setOnClickListener {
                // Check if all contact fields are filled
                if (contact1.text.toString().isNotEmpty() &&
                    contact2.text.toString().isNotEmpty() &&
                    contact3.text.toString().isNotEmpty()
                ) {
                    // Add contacts to emergency contacts list
                    emergencyContacts.clear()
                    emergencyContacts.add(contact1.text.toString())
                    emergencyContacts.add(contact2.text.toString())
                    emergencyContacts.add(contact3.text.toString())

                    // Save contacts to SharedPreferences
                    val editor = sharedPreferences.edit()
                    editor.putString("contact1", contact1.text.toString())
                    editor.putString("contact2", contact2.text.toString())
                    editor.putString("contact3", contact3.text.toString())
                    editor.apply()

                    // Display success message
                    Toast.makeText(this, "Emergency contacts added", Toast.LENGTH_SHORT).show()
                } else {
                    // Display error message
                    Toast.makeText(this, "Please fill all contact fields", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            // Set click listener for remove contact button
            removeContactButton.setOnClickListener {
                // Remove all contacts from emergency contacts list
                emergencyContacts.clear()

                // Clear contact fields
                contact1.text.clear()
                contact2.text.clear()
                contact3.text.clear()

                // Remove contacts from SharedPreferences
                val editor = sharedPreferences.edit()
                editor.remove("contact1")
                editor.remove("contact2")
                editor.remove("contact3")
                editor.apply()

                // Display success message
                Toast.makeText(this, "Emergency contacts removed", Toast.LENGTH_SHORT).show()
            }

            // Set volume button long press listener
            val handler = Handler(Looper.getMainLooper())
            val volumeButtonRunnable = Runnable {
                // Send SMS with location to emergency contacts
                val subscriptionId = SmsManager.getDefaultSmsSubscriptionId()
                val smsManager = SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
                val message = getLastKnownLocation()
                for (contact in emergencyContacts) {
                    smsManager.sendTextMessage(contact, null, message, null, null)
                }

                // Display success message
                Toast.makeText(this, "Emergency SMS sent", Toast.LENGTH_SHORT).show()
            }
            val volumeButtonLongPressListener = View.OnLongClickListener {
                Log.d("MainActivity", "Volume button long press detected")
                if (System.currentTimeMillis() - volumeButtonPressedTime >= 5000) {
                    handler.post(volumeButtonRunnable)
                    true
                } else {
                    false
                }
            }
            volumeControlStream = AudioManager.STREAM_MUSIC
            findViewById<View>(android.R.id.content).setOnTouchListener { view, event ->
                Log.d("MainActivity", "OnTouchListener called")
                if (event.action == MotionEvent.ACTION_DOWN) {
                    volumeButtonPressedTime = System.currentTimeMillis()
                } else if (event.action == MotionEvent.ACTION_UP) {
                    if (System.currentTimeMillis() - volumeButtonPressedTime >= 5000) {
                        handler.removeCallbacks(volumeButtonRunnable)
                    } else {
                        view.performClick()
                    }
                }
                false
            }

            // Set click listener for emergency button
            val emergencyButton = findViewById<Button>(R.id.emergency_button)
            emergencyButton.setOnLongClickListener(volumeButtonLongPressListener)
            emergencyButton.setOnClickListener {
                // Perform the same action as pressing the volume button for 5 seconds
                handler.postDelayed(volumeButtonRunnable, 5000)
            }
        }

        override fun onResume() {
            super.onResume()
            // Request location updates
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0,
                    0f,
                    locationListener
                )
            }
        }

        override fun onPause() {
            super.onPause()
            // Remove location updates
            locationManager.removeUpdates(locationListener)
        }

    private fun getLastKnownLocation(): String {
        // Check if location permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return "Location permission not granted"
        }

        // Check if location services are enabled
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return "Location services disabled"
        }

        // Get last known location
        var location: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (location == null) {
            // Request location updates
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0L,
                0f,
                object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        // Update location and remove listener
                        this@MainActivity.location = location
                        locationManager.removeUpdates(this)
                    }

                    override fun onProviderEnabled(provider: String) {}

                    override fun onProviderDisabled(provider: String) {}

                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                }
            )
            return "Location not available"
        }

        // Format location as a string
        return "I need help! My location is: https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
    }


    private fun showSavedContacts() {
            if (emergencyContacts.isNotEmpty()) {
                val message = "Saved emergency contacts:\n${emergencyContacts.joinToString("\n")}"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

