package org.giriraj.pojo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Truck {
    String wheels;
    String color;
    String model;
    String make;


}
