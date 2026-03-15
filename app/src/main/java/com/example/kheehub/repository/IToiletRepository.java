package com.example.kheehub.repository;

import androidx.lifecycle.LiveData;
import com.example.kheehub.model.Toilet;
import java.util.List;

public interface IToiletRepository {
    LiveData<List<Toilet>> getToilets();
}
