package org.giriraj.pojo;

import lombok.Builder;
import lombok.Data;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
public class Truck  {
    private String wheels;


    public static Map<String,String> getAbc(){
        Map<String,String> map = new HashMap<>();
        return map;
    }


}
