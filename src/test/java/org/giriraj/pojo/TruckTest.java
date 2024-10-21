package org.giriraj.pojo;

import com.codebox.bean.JavaBeanTester;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TruckTest {
    @Test
    void testTruck(){
        JavaBeanTester.builder(Truck.class)
                .checkEquals()
                .checkConstructor()
                .checkClear()
                .loadData()
                .test();
    }
    @Test
    void testTructWithPojoTester(){
        PojoTester.testPojo(Truck.class);
    }

}