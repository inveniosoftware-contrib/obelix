package queue.impl;

import org.json.JSONObject;

public class ObelixQueueElement {

    private final JSONObject data;

    public final JSONObject getData() {
        return this.data;
    }

    public ObelixQueueElement(final JSONObject dataInput) {
        this.data = dataInput;
    }

    public ObelixQueueElement(final String key, final Object element) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(key, element);
        this.data = jsonObject;
    }

    public ObelixQueueElement(final String rawData) {

        String currentRawData = rawData;

        // Let's try to convert the json formatted as string..
        if (!currentRawData.startsWith("{")) {
            currentRawData = currentRawData.substring(1, rawData.length() - 1);
            currentRawData = currentRawData.replace("\\", "");
        }

        this.data = new JSONObject(currentRawData);
    }

    public final boolean equals(final Object object) {
        return object instanceof ObelixQueueElement && this.data.toString()
                .equals(((ObelixQueueElement) object).data.toString());
    }

    public final String toString() {
        return this.data.toString();
    }

    public final int hashCode() {
        return data.hashCode();
    }
}
