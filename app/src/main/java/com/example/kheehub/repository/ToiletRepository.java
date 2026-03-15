package com.example.kheehub.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.kheehub.model.Toilet;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ToiletRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public LiveData<List<Toilet>> getToilets() {
        MutableLiveData<List<Toilet>> toiletsLiveData = new MutableLiveData<>();
        db.collection("toilets")
                .get()
                .addOnSuccessListener(result -> {
                    List<Toilet> toilets = new ArrayList<>();
                    for (DocumentSnapshot document : result) {
                        Toilet toilet = document.toObject(Toilet.class);
                        if (toilet != null) {
                            toilets.add(toilet);
                        }
                    }
                    toiletsLiveData.setValue(toilets);
                })
                .addOnFailureListener(e -> {
                    // Handle error
                });
        return toiletsLiveData;
    }
}
