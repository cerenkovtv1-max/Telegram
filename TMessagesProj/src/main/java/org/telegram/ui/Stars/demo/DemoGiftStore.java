package org.telegram.ui.Stars.demo;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;

/**
 * Where DEMO upgrade results are kept: a private preferences file of its own, never shared with any real gift, account,
 * database or server state. Each record is plain data (names, ids the server already used, colours); no request is made.
 */
public final class DemoGiftStore {

    /** Separate from every stock store on purpose. */
    private static final String PREFS = "demo_gift_results";
    private static final String KEY_COUNTER = "counter";
    private static final String KEY_RESULTS = "results";
    private static final int MAX_RECORDS = 200;

    public static final class Record {
        public int serial;
        public long createdAt;
        public long giftId;
        public String giftTitle;
        public String modelName;
        public long modelDocumentId;
        public String patternName;
        public long patternDocumentId;
        public String backdropName;
        public int backdropId;
        /** True when the backdrop came from the client's built-in table, i.e. its server id is unknown. */
        public boolean backdropBuiltIn;
        public int backdropCenter, backdropEdge, backdropPattern, backdropText;
    }

    private static DemoGiftStore instance;

    public static synchronized DemoGiftStore get() {
        if (instance == null) {
            instance = new DemoGiftStore();
        }
        return instance;
    }

    private DemoGiftStore() {
    }

    private SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** The number shown after the gift name ("Plush Pepe #3"): a local counter, not a real gift number. */
    public synchronized int nextSerial() {
        final SharedPreferences preferences = prefs();
        final int next = preferences.getInt(KEY_COUNTER, 0) + 1;
        preferences.edit().putInt(KEY_COUNTER, next).apply();
        return next;
    }

    /** Saves the record and returns how many DEMO results are stored now. */
    public synchronized int add(Record record) {
        final SharedPreferences preferences = prefs();
        JSONArray array = readArray(preferences);
        try {
            final JSONObject o = new JSONObject();
            o.put("serial", record.serial);
            o.put("createdAt", record.createdAt);
            o.put("giftId", record.giftId);
            o.put("giftTitle", record.giftTitle);
            o.put("modelName", record.modelName);
            o.put("modelDocumentId", record.modelDocumentId);
            o.put("patternName", record.patternName);
            o.put("patternDocumentId", record.patternDocumentId);
            o.put("backdropName", record.backdropName);
            o.put("backdropId", record.backdropId);
            o.put("backdropBuiltIn", record.backdropBuiltIn);
            o.put("center", record.backdropCenter);
            o.put("edge", record.backdropEdge);
            o.put("pattern", record.backdropPattern);
            o.put("text", record.backdropText);
            array.put(o);

            if (array.length() > MAX_RECORDS) {
                final JSONArray trimmed = new JSONArray();
                for (int i = array.length() - MAX_RECORDS; i < array.length(); ++i) {
                    trimmed.put(array.get(i));
                }
                array = trimmed;
            }
            preferences.edit().putString(KEY_RESULTS, array.toString()).apply();
        } catch (Exception e) {
            FileLog.e(e);
        }
        return array.length();
    }

    public synchronized int count() {
        return readArray(prefs()).length();
    }

    private static JSONArray readArray(SharedPreferences preferences) {
        final String json = preferences.getString(KEY_RESULTS, null);
        if (json == null) {
            return new JSONArray();
        }
        try {
            return new JSONArray(json);
        } catch (Exception e) {
            FileLog.e(e);
            return new JSONArray();
        }
    }
}
