package com.company.random.testing.threadsafe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ThreadSafe {
    public static void main(String[] args) {
        ArrayList<String> strings = new ArrayList<>();
        strings.add("p");
        strings.add("a");
        strings.add("e");


        List<String> synlist = Collections.synchronizedList(strings);
        //strings.remove("a");
        synlist.add("lol");
        synchronized (synlist) {

        }
        for (String string : strings) {
            //strings.remove("a");
            synlist.remove("e");
            System.out.println(string);
        }
    }
}
