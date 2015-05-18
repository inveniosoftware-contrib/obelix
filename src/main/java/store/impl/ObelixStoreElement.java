package store.impl;


import org.json.JSONObject;

public class ObelixStoreElement extends JSONObject {

    public final JSONObject data;

    public ObelixStoreElement(JSONObject data) {
        this.data = data;
    }

    public ObelixStoreElement(String key, Object element) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(key, element);
        this.data = jsonObject;
    }

    public ObelixStoreElement(String rawData) {

        // Let's try to convert the json formatted as string..
        if(!rawData.startsWith("{")) {
            rawData = rawData.substring(1, rawData.length()-1);
            rawData = rawData.replace("\\", "");
        }

        this.data = new JSONObject(rawData);
    }

    public boolean equals(Object object) {
        return object instanceof ObelixStoreElement && this.data.toString().equals(((ObelixStoreElement) object).data.toString());
    }

    public String toString() {
        return this.data.toString();
    }

}

