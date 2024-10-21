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
    void testTructWithPojoTester() throws Exception {
        new PojoTester<>(Truck.class)
                .test(PojoTester.Setters.class)
                .test(PojoTester.Getters.class)
                .test(PojoTester.ToString.class)
                .test(PojoTester.HashCode.class)
                .test(PojoTester.Equals.class)
                .build(); // Executes the tests
    }

}