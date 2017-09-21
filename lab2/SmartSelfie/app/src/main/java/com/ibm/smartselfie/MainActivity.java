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

import com.google.gson.Gson;
import com.ibm.watson.developer_cloud.android.library.audio.StreamPlayer;
import com.ibm.watson.developer_cloud.android.library.camera.CameraHelper;
import com.ibm.watson.developer_cloud.android.library.camera.GalleryHelper;
import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.Voice;
import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifyImagesOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.DetectedFaces;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.Face;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassification;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassifier;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualRecognitionOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    TextView textView;
    Button button;
    ImageView loadedImage;
    File image;

    //watson developer cloud java & android helpers
    private StreamPlayer streamPlayer;
    private CameraHelper cameraHelper;
    private GalleryHelper galleryHelper;
    private VisualRecognition visualService;
    private TextToSpeech textToSpeechService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.textView);
        button = (Button) findViewById(R.id.button);
        loadedImage = (ImageView) findViewById(R.id.loaded_image);
        cameraHelper = new CameraHelper(this);

        visualService = initVisualRecognitionService();
        textToSpeechService = initTextToSpeechService();

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


            ClassifyImagesOptions options = new ClassifyImagesOptions.Builder()
                    .images(image)
                    //.threshold(.3)
                    .build();

            VisualClassification result1 = visualService.classify(options).execute();
            System.out.println(result1);

            if (result1!=null && !result1.toString().equalsIgnoreCase("{}")) {
                System.out.println("Visual Classification obj not null");

                if (!result1.getImages().get(0).getClassifiers().isEmpty()) {
                    ttsResponse = ttsResponse + " And I see potentially the following : ";

                    for(int i=0; i< result1.getImages().get(0).getClassifiers().get(0).getClasses().size();i++) {
                        String nameClassifier = result1.getImages().get(0).getClassifiers().get(0).getClasses().get(i).getName() +
                                " with a score " + result1.getImages().get(0).getClassifiers().get(0).getClasses().get(i).getScore();
                        System.out.println("found class: " + nameClassifier);
                        ttsResponse = ttsResponse + nameClassifier + ", ";
                    }
                }
            }

            VisualRecognitionOptions options2 = new VisualRecognitionOptions.Builder()
                    .images(image)
                    .build();

            DetectedFaces faces = visualService.detectFaces(options2).execute();
            String showAgeMin, showAgeMax, showGender;
            System.out.println(faces);

            if (faces!=null && !faces.toString().equalsIgnoreCase("{}")) {
                System.out.println("faces obj not null");
                if (!faces.getImages().get(0).getFaces().isEmpty()) {
                    Face face = faces.getImages().get(0).getFaces().get(0);
                    Face.Age age = faces.getImages().get(0).getFaces().get(0).getAge();
                    showAgeMin = Integer.toString(age.getMin());
                    showAgeMax = Integer.toString(age.getMax());
                    showGender = face.getGender().getGender();

                    ttsResponse = ttsResponse + ". And I see you are taking picture of a person - probably " + showGender + " who is about " + showAgeMin + " years old.";
                }
                //when there are faces
                //lets add our smartSelfie
                //suggestions
                ArrayList<String> classifiers = new ArrayList<String>();
                classifiers.add(getString(R.string.visual_recognition_classifier));


                ClassifyImagesOptions optionsImg = new ClassifyImagesOptions.Builder()
                        .images(image)
                        .classifierIds(classifiers)
                        .threshold(.01)
                        .build();
                VisualClassification result = visualService.classify(optionsImg).execute();
                System.out.println(result);

                if (result != null && !result.toString().equalsIgnoreCase("{}")) {
                    System.out.println("Visual Classification obj not null");

                    if (!result.getImages().get(0).getClassifiers().isEmpty()) {


                        for (int i = 0; i < result.getImages().get(0).getClassifiers().get(0).getClasses().size(); i++) {
                            String nameClassifier = result.getImages().get(0).getClassifiers().get(0).getClasses().get(i).getName();
                            ttsResponse = ttsResponse + ". Finally Watson V.R. recommendation is to remove " + nameClassifier;
                        }
                    }
                }
            }



            //invoke text to speech service
            System.out.println("Logging invoking Watson TTS");
            System.out.println(ttsResponse);

            streamPlayer = new StreamPlayer();
            streamPlayer.playStream(textToSpeechService.synthesize(ttsResponse
                    , Voice.EN_MICHAEL).execute());

            return "Did visual";
        }

        //setting the value of UI outside of the thread
        @Override
        protected void onPostExecute(String result) {
            textView.setText("The message is: " + result);
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CameraHelper.REQUEST_IMAGE_CAPTURE) {
            loadedImage.setImageBitmap(cameraHelper.getBitmap(resultCode));
            System.out.println("-- getting a pic... set it up for screen");

            System.out.println("-------- got a pic... now Watson... in the thread");
            System.out.println(new WatsonTask().execute(resultCode));
        }

        System.out.println("------ now classify an image");
        if (requestCode == CameraHelper.REQUEST_IMAGE_CAPTURE) {
            image = cameraHelper.getFile(resultCode);
            System.out.println("image to string: " + image.toString());
        }
    }


    private VisualRecognition initVisualRecognitionService() {
        return new VisualRecognition(VisualRecognition.VERSION_DATE_2016_05_20,
                getString(R.string.visual_recognition_api_key));
    }

    private TextToSpeech initTextToSpeechService() {
        TextToSpeech service = new TextToSpeech();
        String username = getString(R.string.text_speech_username);
        String password = getString(R.string.text_speech_password);
        service.setUsernameAndPassword(username, password);
        return service;
    }



}
