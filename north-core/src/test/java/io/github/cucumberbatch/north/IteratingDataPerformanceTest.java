package io.github.cucumberbatch.north;

import java.util.*;

public class IteratingDataPerformanceTest {

    int arraySize = 32768;

    // @Test
    void testPerformanceSortedArray() {
        int data[] = new int[arraySize];

        Random rnd = new Random(0);
        for (int c = 0; c < arraySize; ++c)
            data[c] = rnd.nextInt() % 256;

        // !!! With this, the next loop runs faster
        Arrays.sort(data);

        // Test
        long start = System.nanoTime();
        long sum = 0;
        for (int i = 0; i < 100000; ++i)
        {
            for (int c = 0; c < arraySize; ++c)
            {   // Primary loop
                if (data[c] >= 128)
                    sum += data[c];
            }
        }

        System.out.println((System.nanoTime() - start) / 1000000000.0);
        System.out.println("sum = " + sum);
    }


    // @Test
    void testPerformanceUnsortedArray() {
        int data[] = new int[arraySize];

        Random rnd = new Random(0);
        for (int c = 0; c < arraySize; ++c)
            data[c] = rnd.nextInt() % 256;

        // Test
        long start = System.nanoTime();
        long sum = 0;
        for (int i = 0; i < 100000; ++i)
        {
            for (int c = 0; c < arraySize; ++c)
            {   // Primary loop
                if (data[c] >= 128)
                    sum += data[c];
            }
        }

        System.out.println((System.nanoTime() - start) / 1000000000.0);
        System.out.println("sum = " + sum);
    }


    // @Test
    void testPerformanceSortedList() {
        List<Integer> data = new ArrayList<Integer>(arraySize);

        Random rnd = new Random(0);
        for (int c = 0; c < arraySize; ++c)
            data.add(rnd.nextInt() % 256);

        Collections.sort(data);

        // Test
        long start = System.nanoTime();
        long sum = 0;
        for (int i = 0; i < 100000; ++i)
        {
            for (int c = 0; c < arraySize; ++c)
            {   // Primary loop
                if (data.get(c) >= 128)
                    sum += data.get(c);
            }
        }

        System.out.println((System.nanoTime() - start) / 1000000000.0);
        System.out.println("sum = " + sum);
    }

    // @Test
    void testPerformanceUnsortedList() {
        List<Integer> data = new ArrayList<Integer>(arraySize);

        Random rnd = new Random(0);
        for (int c = 0; c < arraySize; ++c)
            data.add(rnd.nextInt() % 256);

        // Test
        long start = System.nanoTime();
        long sum = 0;
        for (int i = 0; i < 100000; ++i)
        {
            for (int c = 0; c < arraySize; ++c)
            {   // Primary loop
                if (data.get(c) >= 128)
                    sum += data.get(c);
            }
        }

        System.out.println((System.nanoTime() - start) / 1000000000.0);
        System.out.println("sum = " + sum);
    }

    // @Test
    void testPerformanceQueue() {
        Queue<Integer> data = new LinkedList<>();

        Random rnd = new Random(0);
        for (int c = 0; c < arraySize; ++c)
            data.add(rnd.nextInt() % 256);

        // Test
        long start = System.nanoTime();
        long sum = 0;
       for (int c = 0; c < arraySize; ++c)
        {   // Primary loop
            if (data.peek() >= 128)
                sum += data.poll();
        }

        System.out.println((System.nanoTime() - start) / 1000000000.0);
        System.out.println("sum = " + sum);

    }


    // @Test
    void testPerformanceLinkedList() {
        List<Integer> data = new LinkedList<>();

        Random rnd = new Random(0);
        for (int c = 0; c < arraySize; ++c)
            data.add(rnd.nextInt() % 256);

        // Test
        long start = System.nanoTime();
        long sum = 0;
        for (int c = 0; c < arraySize; ++c)
        {   // Primary loop
//            if (data.peek() >= 128)
//                sum += data.poll();
        }

        System.out.println((System.nanoTime() - start) / 1000000000.0);
        System.out.println("sum = " + sum);

    }
}
