package store.impl;


import org.json.JSONObject;

public class ObelixStoreElement extends JSONObject {

    private final JSONObject data;

    public final JSONObject getData() {
        return this.data;
    }

    public ObelixStoreElement(final JSONObject dataInput) {
        this.data = dataInput;
    }

    public ObelixStoreElement(final String key, final Object element) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(key, element);
        this.data = jsonObject;
    }

    public ObelixStoreElement(final String rawData) {

        String currentRawData = rawData;

        // Let's try to convert the json formatted as string..
        if (!currentRawData.startsWith("{")) {
            currentRawData = currentRawData.substring(1, rawData.length() - 1);
            currentRawData = currentRawData.replace("\\", "");
        }

        this.data = new JSONObject(currentRawData);
    }

    public final boolean equals(final Object object) {
        return object instanceof ObelixStoreElement && this.data.toString().equals(
                ((ObelixStoreElement) object).data.toString());
    }

    public final int hashCode() {
        return this.data.toString().hashCode();
    }

    public final String toString() {
        return this.data.toString();
    }

}

