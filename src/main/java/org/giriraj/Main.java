package org.giriraj;

import com.codebox.bean.JavaBeanTester;
import org.giriraj.pojo.Truck;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");
        JavaBeanTester.builder(Truck.class)
                .checkEquals()
                .checkConstructor()
                .checkClear()
                .loadData()
                .test();

    }
}