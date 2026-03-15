package com.example.kheehub.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.kheehub.model.Toilet;
import com.example.kheehub.repository.ToiletRepository;
import com.google.android.gms.maps.GoogleMap;

import java.util.List;

public class MapViewModel extends ViewModel {
    private final ToiletRepository repository = new ToiletRepository();
    private final MutableLiveData<Integer> mapType = new MutableLiveData<>(GoogleMap.MAP_TYPE_NORMAL);
    private LiveData<List<Toilet>> toilets;

    public LiveData<Integer> getMapType() {
        return mapType;
    }

    public void toggleMapType() {
        if (mapType.getValue() == null) return;
        
        if (mapType.getValue() == GoogleMap.MAP_TYPE_NORMAL) {
            mapType.setValue(GoogleMap.MAP_TYPE_SATELLITE);
        } else {
            mapType.setValue(GoogleMap.MAP_TYPE_NORMAL);
        }
    }

    public LiveData<List<Toilet>> getToilets() {
        if (toilets == null) {
            toilets = repository.getToilets();
        }
        return toilets;
    }
}
