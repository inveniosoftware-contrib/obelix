package store.impl;

import org.json.JSONObject;
import store.interfaces.ObelixStore;

import java.util.HashMap;
import java.util.Map;

public class InternalObelixStore implements ObelixStore {

    private Map<String, JSONObject> store = new HashMap<>();

    @Override
    public final void set(final String key, final ObelixStoreElement value) {
        this.store.put(key, value.getData());
    }

    @Override
    public final ObelixStoreElement get(final String key) {
        return new ObelixStoreElement(this.store.get(key));
    }

}
