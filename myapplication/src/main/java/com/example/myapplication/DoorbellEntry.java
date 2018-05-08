package com.example.myapplication;
import java.util.Map;

/**
 * Created by shilpi on 23/3/18.
 */

/**
 * Model class for Firebase data entries
 */

public class DoorbellEntry {

    Long timestamp;
    String image;
    Map<String, Float> annotations;

    public DoorbellEntry() {
    }

    public DoorbellEntry(Long timestamp, String image, Map<String, Float> annotations) {
        this.timestamp = timestamp;
        this.image = image;
        this.annotations = annotations;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getImage() {
        return image;
    }

    public Map<String, Float> getAnnotations() {
        return annotations;
    }
}
