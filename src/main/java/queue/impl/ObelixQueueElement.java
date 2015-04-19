package queue.impl;

import org.json.JSONObject;

public class ObelixQueueElement {

    public final JSONObject data;

    public ObelixQueueElement(JSONObject data) {
        this.data = data;
    }

    public ObelixQueueElement(String key, Object element) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(key, element);
        this.data = jsonObject;
    }

    public ObelixQueueElement(String rawData) {
        this.data = new JSONObject(rawData);
    }

    public boolean equals(Object object) {
        return object instanceof ObelixQueueElement && this.data.toString().equals(((ObelixQueueElement) object).data.toString());
    }

    public String toString() {
        return this.data.toString();
    }

}
