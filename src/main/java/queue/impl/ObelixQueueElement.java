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

        // Let's try to convert the json formatted as string..
        if(!rawData.startsWith("{")) {
            rawData = rawData.substring(1, rawData.length()-1);
            rawData = rawData.replace("\\", "");
        }

        this.data = new JSONObject(rawData);
    }

    public boolean equals(Object object) {
        return object instanceof ObelixQueueElement && this.data.toString().equals(((ObelixQueueElement) object).data.toString());
    }

    public String toString() {
        return this.data.toString();
    }

}
