# cloudinary-watson

## LAB 2
Photographer’s Assistant:

smart photography - Watson advising on taking a greater picture thru smart recommendations.
-- Watson Training Models for analysis with Cloudinary
-- the Android Selfie / photo app that provide feedback (on the typical picture mistakes)

## Web App
Here comes the details....

## Android
In order to start you would need the following:
- [Android Studio](https://developer.android.com/studio/index.html)
- an android device
- [Sign up to Bluemix](https://bluemix.net/registration/) to create Watson services:
    - credentials (API KEY) to Visual Recognition service, id of your custom classifier
    - Watson credentials (id and password) to Text to Speech service

Here comes the steps to create SmartSelfie Android app:

1. Create a typical Android application in Java
2. Add the Watson SDK: [Watson-Developer-Cloud SDK for Java](https://github.com/watson-developer-cloud/java-sdk#installation) and Android
3. Instantiate the Bluemix Watson services and get the keys/a token to them
4. Add some code in your Android app to invoke the cognitive services
5. Quick test of the app.


### Step 1. Create a typical Android application in Java
```java
package com.ibm.smartselfie;

import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

TextView textView;
Button button;
ImageView loadedImage;
File image;

//watson developer cloud java & android helpers


@Override
protected void onCreate(Bundle savedInstanceState) {
super.onCreate(savedInstanceState);
setContentView(R.layout.activity_main);

textView = (TextView) findViewById(R.id.textView);
button = (Button) findViewById(R.id.button);
loadedImage = (ImageView) findViewById(R.id.loaded_image);


//fire action when button is pressed
button.setOnClickListener(new View.OnClickListener() {
@Override
public void onClick(View v) {
System.out.println("Logging to the console that the button pressed");
System.out.println("Logging camera helper");
cameraHelper.dispatchTakePictureIntent();


}
});
}


/**
* On request permissions result.
*
* @param requestCode the request code
* @param permissions the permissions
* @param grantResults the grant results
*/
@Override
public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
switch (requestCode) {
case CameraHelper.REQUEST_PERMISSION: {
// permission granted
if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
cameraHelper.dispatchTakePictureIntent();
}
}

default: Toast.makeText(this, "yay!", Toast.LENGTH_SHORT).show();
}
}

private class WatsonTask extends AsyncTask<Integer, Void, String> {
@Override protected String doInBackground(Integer... integers) {

String ttsResponse = "This is Watson-Cloudinary Smart Selfie. Your picture looks great.";


}



//invoke text to speech service
System.out.println("Logging invoking Watson TTS");
System.out.println(ttsResponse);

return "Did visual";
}

//setting the value of UI outside of the thread
@Override
protected void onPostExecute(String result) {
textView.setText("The message is: " + result);
}
}



}
```


Delivered to you by Cloudinary and IBM 

Subscribe to our Twitter: @Dan and @blumareks
