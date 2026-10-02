package com.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

public class ComputeStats {
    public static void main(String[] args) {
        try {
            BufferedReader reader = new BufferedReader(
                    new FileReader("diagnosis14.txt"));
            HashMap<String, Integer> qnoToOriginalDi = new HashMap<String, Integer>();

            String line;
            while ((line = reader.readLine()) != null) {
                String splitArray[] = line.split(",");
                String qno = splitArray[0];
                if (qnoToOriginalDi.containsKey(qno)) {
                    qnoToOriginalDi.put(qno, qnoToOriginalDi.get(qno) + 1);
                } else {
                    qnoToOriginalDi.put(qno, 1);
                }
            }

            reader = new BufferedReader(new FileReader(
                    "out.txt"));
            HashMap<String, LinkedList<Item>> qnoToDi = new HashMap<String, LinkedList<Item>>();

            String lastQNO = "1";
            int rank = 0;
            int maxRank = 0;
            LinkedList<Item> list1 = new LinkedList<>();
            qnoToDi.put(lastQNO, list1);
            while ((line = reader.readLine()) != null) {
                String splitArray[] = line.split(",");
                if (splitArray[0].equals(lastQNO)) {
                    rank++;
                    if (rank > maxRank) {
                        maxRank = rank;
                    }
                } else {
                    lastQNO = splitArray[0];
                    rank = 1;
                    LinkedList<Item> list = new LinkedList<>();
                    qnoToDi.put(lastQNO, list);
                }

                if (!line.contains("NDI")) {
                    if (line.contains("DI")) {
                        if (splitArray[splitArray.length - 2].trim().contains("DI")) {
                            Item item = new Item();
                            item.diagnosis = splitArray[splitArray.length - 2].trim();
                            item.rank = rank;
                            qnoToDi.get(lastQNO).add(item);

                            Item item2 = new Item();
                            item2.diagnosis = splitArray[splitArray.length - 1].trim();
                            item2.rank = rank;
                            qnoToDi.get(lastQNO).add(item2);
                        } else {
                            Item item = new Item();
                            item.diagnosis = splitArray[splitArray.length - 1].trim();
                            item.rank = rank;
                            qnoToDi.get(lastQNO).add(item);
                        }

                    }
                }
            }

            double sumAtK = 0;
            double sumCoverage = 0;
            int k = 5;
            for (int i = 1; i <= 30; i++) {
                String qno = i + "";
                LinkedList<Item> list = qnoToDi.get(qno);
                boolean flag = false;
                double val = qnoToOriginalDi.get(qno);
                double counter = list.size();
                System.out.println("QNO = " + qno + " Diversity Coverage = " + (counter / val));
                sumCoverage += (counter / val);
                for (Item item : list) {
                    System.out.println("-item:" + item.diagnosis + " -rank:" + item.rank);
                    if (item.rank <= k) {
                        flag = true;
                    }
                }
                if (flag) {
                    sumAtK += 1;
                }
            }
            System.out.println("sumAtK = " + sumAtK/30.0);
            System.out.println("sumCoverage = " + sumCoverage/30.0);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
