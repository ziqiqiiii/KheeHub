package com.example.kheehub;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// NEW CLASS: Follows Single Responsibility Principle — this class owns ALL
// Firestore operations. MapFragment and AddFragment no longer talk to
// Firestore directly.
public class ToiletRepository {

    private static final String COLLECTION = "toilets";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Singleton so all fragments share one instance
    private static ToiletRepository instance;

    public static ToiletRepository getInstance() {
        if (instance == null) {
            instance = new ToiletRepository();
        }
        return instance;
    }

    private ToiletRepository() {}

    // Used by AddFragment to save a new toilet
    public void addToilet(Toilet toilet,
                          OnSuccessListener<Void> onSuccess,
                          OnFailureListener onFailure) {
        db.collection(COLLECTION)
                .document(toilet.getName())
                .set(toilet)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    // Used by MapFragment to load all toilets on startup
    public void getAllToilets(OnSuccessListener<List<Toilet>> onSuccess,
                              OnFailureListener onFailure) {
        db.collection(COLLECTION)
                .get()
                .addOnSuccessListener(result -> onSuccess.onSuccess(result.toObjects(Toilet.class)))
                .addOnFailureListener(onFailure);
    }

    // Used by MapFragment's edit dialog to update an existing toilet
    public void updateToilet(Toilet original,
                             String newName, String newFloor, double newRating,
                             String newHours, int newStatus, List<String> newTags,
                             OnSuccessListener<Void> onSuccess,
                             OnFailureListener onFailure) {

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("floor", newFloor);
        updates.put("rating", newRating);
        updates.put("openingHours", newHours);
        updates.put("status", newStatus);
        updates.put("tags", newTags);

        db.collection(COLLECTION)
                .whereEqualTo("name", original.getName())
                .whereEqualTo("lat", original.getLat())
                .whereEqualTo("lng", original.getLng())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String docId = querySnapshot.getDocuments().get(0).getId();
                        db.collection(COLLECTION)
                                .document(docId)
                                .update(updates)
                                .addOnSuccessListener(onSuccess)
                                .addOnFailureListener(onFailure);
                    }
                })
                .addOnFailureListener(onFailure);
    }
}