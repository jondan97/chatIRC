package com.company.random.testing.time;

import com.company.entity.Record;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class Tracker {
    public static void main(String[] args) {
        //Record record = new Record(new User("xixis"));
        while (true) {
            //long diff = calculateMinutes(record);
            //System.out.println(diff);
        }
    }

    public static long calculateMinutes(Record record) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime recordTime = record.getTime();
        return ChronoUnit.MINUTES.between(recordTime, now);
    }
}
