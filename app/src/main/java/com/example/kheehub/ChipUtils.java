package com.example.kheehub;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

// NEW CLASS: Extracts the repeated chip-collection loop that appeared in
// AddFragment, showEditDialog, and showFilterDialog — DRY principle applied.
public class ChipUtils {

    private ChipUtils() {
        // Utility class — no instances needed
    }

    // Returns lowercase text of all checked chips in a ChipGroup
    public static List<String> getSelectedTags(ChipGroup chipGroup) {
        List<String> selectedTags = new ArrayList<>();
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            Chip chip = (Chip) chipGroup.getChildAt(i);
            if (chip.isChecked()) {
                selectedTags.add(chip.getText().toString().toLowerCase());
            }
        }
        return selectedTags;
    }

    // Pre-checks chips whose text matches a list of saved tags
    public static void restoreSelectedTags(ChipGroup chipGroup, List<String> savedTags) {
        if (savedTags == null) return;
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            Chip chip = (Chip) chipGroup.getChildAt(i);
            chip.setChecked(savedTags.contains(chip.getText().toString().toLowerCase()));
        }
    }
}