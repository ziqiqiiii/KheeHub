package com.example.kheehub;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import android.util.Log;
import android.os.Bundle;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        //addSampleToilets();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("toilets")
                .get()
                .addOnSuccessListener(result -> {
                    for (DocumentSnapshot document : result) {
                        System.out.println(document.getId() + " => " + document.getData());
                    }
                });
    }

    public void addSampleToilets() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Object[][] toilets = {
                {"Upper Changi MRT L1",  1.3428, 103.9614, "L1", "06:00-23:30", 1, Arrays.asList("handicap", "family"), 4.3},
                {"SUTD Campus Block 1",  1.3415, 103.9620, "G",  "07:00-21:00", 1, Arrays.asList("family"),              4.1},
                {"Upper Changi Park WC", 1.3450, 103.9665, "G",  "05:30-22:00", 1, Arrays.asList("handicap"),            3.8},
                {"Summer Gardens L1",    1.3442, 103.9623, "L1", "07:00-20:00", 1, Arrays.asList("family"),              3.9},
                {"Changi Rd Eatery WC",  1.3448, 103.9641, "G",  "06:00-22:30", 1, Arrays.asList("family"),              4.0},
                {"Blk 723 UC Rd East",   1.3441, 103.9618, "G",  "24:00-23:59", 1, Arrays.asList("handicap"),            3.7},
                {"Blk 725 UC Rd East",   1.3442, 103.9619, "G",  "24:00-23:59", 1, Arrays.asList("family"),              3.8},
                {"Tampines St 21 WC",    1.3430, 103.9655, "G",  "06:00-22:00", 1, Arrays.asList("family"),              3.6},
                {"Simei MRT WC",         1.3420, 103.9560, "L1", "06:00-23:00", 1, Arrays.asList("handicap"),            4.2},
                {"Tampines Mall WC",     1.3425, 103.9595, "B1", "10:00-22:00", 1, Arrays.asList("family"),              4.4}
        };

        for (Object[] t : toilets) {
            Map<String, Object> toilet = new HashMap<>();
            toilet.put("name",       t[0]);
            toilet.put("lat",        t[1]);
            toilet.put("lng",        t[2]);
            toilet.put("floor",      t[3]);
            toilet.put("openingHours",t[4]);
            toilet.put("status",     t[5]);      // 1=open, 0=closed
            toilet.put("tags",       t[6]);
            toilet.put("rating",     t[7]);
//            db.collection("toilets")
//                    .add(toilet);
            db.collection("toilets").document((String) t[0]).set(toilet); // uses fixed ID
        }
    }
}

