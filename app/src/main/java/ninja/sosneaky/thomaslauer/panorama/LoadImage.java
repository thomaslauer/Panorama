package ninja.sosneaky.thomaslauer.panorama;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class LoadImage extends AppCompatActivity {

    private static final int LOAD_IMAGE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_image);
    }

    public void loadImage(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,
                "Select Picture"), LOAD_IMAGE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK) {
            if(requestCode == LOAD_IMAGE) {
                Uri panoURI = data.getData();
                Log.d("URI LOCATION", panoURI.toString());


                // here pass off to slicing activity
                Intent intent = new Intent(this, SliceActivity.class);
                intent.putExtra("imagePath", panoURI.toString());
                startActivity(intent);
            }
        }
    }
}
