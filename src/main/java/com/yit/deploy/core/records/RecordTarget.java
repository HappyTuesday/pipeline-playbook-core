package com.yit.deploy.core.records;

import java.util.Map;
import java.util.function.Function;

public interface RecordTarget<R extends Record> {
    /**
     * get the id of the target
     * @return id
     */
    String getId();

    /**
     * apply record to target
     * @param record record to apply
     */
    RecordTarget<R> withRecord(R record);

    static <R extends Record, T extends RecordTarget<R>> void applyRecordToMap(
        R record,
        String key,
        Map<String, T> target, Function<R, T> creator) {

        T value = target.get(key);
        if (value != null) {
            if (value.getId() != null && record.isDisabled()) {
                target.remove(key);
            } else {
                target.put(key, (T) value.withRecord(record));
            }
        } else {
            T newValue = creator.apply(record);
            target.put(key, (T) newValue.withRecord(record));
        }
    }

    static <R extends Record, T extends RecordTarget<R>> void applyRecordsToMap(
        Iterable<R> records,
        Function<R, String> getKey,
        Map<String, T> target, Function<R, T> creator) {

        for (R record : records) {
            applyRecordToMap(record, getKey.apply(record), target, creator);
        }
    }
}
