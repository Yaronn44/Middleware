package com.alma.pay2bid.server;

import com.alma.pay2bid.client.IClient;

import java.util.Comparator;
import java.util.HashMap;

// a comparator that compares Strings
class ValueComparator implements Comparator<IClient> {
    HashMap<IClient, Integer> map = new HashMap<IClient, Integer>();
    public ValueComparator(HashMap<IClient, Integer> map){
        this.map.putAll(map);
    }
    @Override
    public int compare(IClient s1, IClient s2) {
        if(map.get(s1) >= map.get(s2)){
            return -1;
        }else{
            return 1;
        }
    }
}
