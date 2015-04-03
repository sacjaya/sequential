/*
 * Copyright (c) 2005 - 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package debs2015;

import com.google.common.base.Splitter;
import debs2015.performance.PerfStats;
import debs2015.processors.*;
import debs2015.processors.maxK.MaxKQ1Processor;
import debs2015.processors.maxK.MaxKQ2Processor;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


public class ManagerWithFileWriteThread_Handler_2 {

    volatile long events = 0;
    private long startTime;
    private static PerfStats perfStats1 = new PerfStats();
    private static PerfStats perfStats2 = new PerfStats();
    private static long lastEventTime1 = 0;
    private static long lastEventTime2 = 0;
    final boolean performanceLoggingFlag = true;// Config.getConfigurationInfo("org.wso2.siddhi.debs2015.flags.perflogging").equals("true") ? true : false;
    final boolean printOutputFlag = true;// Config.getConfigurationInfo("org.wso2.siddhi.debs2015.flags.printoutput").equals("true") ? true : false;
    static String fileName;
    private static String COMMA = ",";
    private static String CARRIAGERETURN_NEWLINE = "\r\n";
    String logDir = null;
    FileWriter fw = null;
    BufferedWriter bw = null;
    FileWriter fw2 = null;
    BufferedWriter bw2 = null;




    public static void main(String[] args) {
        fileName  = args[0];

        ManagerWithFileWriteThread_Handler_2 manager = new ManagerWithFileWriteThread_Handler_2();

        manager.run();

        System.exit(0);
    }

    public ManagerWithFileWriteThread_Handler_2(){
        logDir = System.getProperty("user.dir") + "/logs";

        System.out.println(logDir);

        File logDirectory = new File(logDir);
        if(!logDirectory.isDirectory()) {
            logDirectory.mkdir();
        }
        try {
            fw = new FileWriter(new File(logDir + "/output-1-" + System.currentTimeMillis() + ".csv").getAbsoluteFile());
            bw = new BufferedWriter(fw);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            fw2 = new FileWriter(new File(logDir + "/output-2-" + System.currentTimeMillis() + ".csv").getAbsoluteFile());
            bw2 = new BufferedWriter(fw2);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void run() {



        loadData(fileName);

        while (true) {
            try {
                if (lastEventTime1 == perfStats1.lastEventTime && lastEventTime2 == perfStats2.lastEventTime) {

                    System.out.println();
                    System.out.println("***** Query 1 *****");
                    long timeDifferenceFromStart = perfStats1.lastEventTime - startTime;

                    System.out.println("event outputed :" + perfStats1.count);
                    System.out.println("time to process (ms) :" + timeDifferenceFromStart);
                    System.out.println("over all throughput (events/s) :" + ((perfStats1.count * 1000) / timeDifferenceFromStart));
                    System.out.println("over all avg latency (ms) :" + (perfStats1.totalLatency / perfStats1.count));
                    System.out.println();
                    System.out.println("***** Query 2 *****");
                    timeDifferenceFromStart = perfStats2.lastEventTime - startTime;

                    System.out.println("event outputed :" + perfStats2.count);
                    System.out.println("time to process (ms) :" + timeDifferenceFromStart);
                    System.out.println("over all throughput (events/s) :" + ((perfStats2.count * 1000) / timeDifferenceFromStart));
                    System.out.println("over all avg latency (ms) :" + (perfStats2.totalLatency / perfStats2.count));
                    break;
                } else {
                    lastEventTime1 = perfStats1.lastEventTime;
                    lastEventTime2 = perfStats2.lastEventTime;
                    Thread.sleep(10*1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void loadData(String fileName) {
        Splitter splitter = Splitter.on(',');
        BufferedReader br;
        int count = 0;
        HashMap<String, Integer> medallionMap = new HashMap<String, Integer>();
        int medallionCount = 1;

        try {
            br = new BufferedReader(new FileReader(fileName), 10 * 1024 * 1024);
            String line = br.readLine();
            startTime = System.currentTimeMillis();
            while (line != null) {

                events++;

                //We make an assumption here that we do not get empty strings due to missing values that may present in the input data set.
                Iterator<String> dataStrIterator = splitter.split(line).iterator();
                String medallion = dataStrIterator.next();

                Integer medallionIntVal = medallionMap.get(medallion);
                if (medallionIntVal == null) {
                    medallionIntVal = medallionCount;
                    medallionMap.put(medallion, medallionCount++);
                }
                dataStrIterator.next();//This is hack_license. Since we are not using hack_license,
                                       //we do this just for the sake of moving the curser to next attribute.
                String pickup_datetime = dataStrIterator.next();
                String dropoff_datetime = dataStrIterator.next();
                dataStrIterator.next();//This is trip_time_in_secs. But again we do not use it.
                dataStrIterator.next();//trip_distance
                String pickup_longitude = dataStrIterator.next();
                String pickup_latitude = dataStrIterator.next();
                String dropoff_longitude = dataStrIterator.next();
                String dropoff_latitude = dataStrIterator.next();
                dataStrIterator.next();//payment_type
                String fare_amount = dataStrIterator.next();
                dataStrIterator.next();//surcharge
                dataStrIterator.next();//mta_tax
                String tip_amount = dataStrIterator.next();

                //Although there are other attributes in this line after tip_amount (e.g., tolls_amount
                //and total_amount) we do not use those.
                
                long currentTIme = System.currentTimeMillis();
                float pickupLongitude = Float.parseFloat(pickup_longitude);
                float pickupLatitude;
                float dropoffLongitude;
                float dropoffLatitude;
                try {
                    if (-74.916578f > pickupLongitude || -73.120778f < pickupLongitude) {
                        line = br.readLine();
                        continue;
                    }

                    pickupLatitude = Float.parseFloat(pickup_latitude);

                    if (40.129715978f > pickupLatitude || 41.477182778f < pickupLatitude) {
                        line = br.readLine();
                        continue;
                    }


                    dropoffLongitude = Float.parseFloat(dropoff_longitude);

                    if (-74.916578f > dropoffLongitude || -73.120778f < dropoffLongitude) {
                        line = br.readLine();
                        continue;
                    }

                    dropoffLatitude = Float.parseFloat(dropoff_latitude);

                    if (40.129715978f > dropoffLatitude || 41.477182778f < dropoffLatitude) {
                        line = br.readLine();
                        continue;
                    }
                } catch (NumberFormatException e) {
                    //We do nothing here. This is due having odd values for lat, lon values.
                    line = br.readLine();
                    continue;
                }

                float fareAmount = Float.parseFloat(fare_amount);
                float tipAmount = Float.parseFloat(tip_amount);
                float totalAmount;

                //This is to address the issue where we may get fare or tip as negative values due to
                //erroneous records in the input data set.
                if (fareAmount < 0 || tipAmount < 0) {
                    totalAmount = -1f;
                } else {
                    totalAmount = fareAmount + tipAmount;
                }


                    DebsEvent eventHolder = new DebsEvent();
                    eventHolder.setMedallion(medallionIntVal);
                    eventHolder.setPickup_datetime_org(pickup_datetime);
                    eventHolder.setDropoff_datetime_org(dropoff_datetime);
                    eventHolder.setPickup_longitude(pickupLongitude);
                    eventHolder.setPickup_latitude(pickupLatitude);
                    eventHolder.setDropoff_longitude(dropoffLongitude);
                    eventHolder.setDropoff_latitude(dropoffLatitude);
                    eventHolder.setFare_plus_ip_amount(totalAmount);
                    eventHolder.setIij_timestamp(currentTIme);

                    count++;
                   process(eventHolder);

                line = br.readLine();
            }

            long currentTime = System.currentTimeMillis();
            System.out.println("****** Input ******");
            System.out.println("events read : " + events);
            System.out.println("time to read (ms) : " + (currentTime - startTime));
            System.out.println("read throughput (events/s) : " + (events * 1000 / (currentTime - startTime)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.out.println("Now exiting from data loader");
    }

    CellIdProcessor cellIdProcessor = new CellIdProcessor();
    TimeStampProcessor timeStampProcessor = new TimeStampProcessor();

    public void process(DebsEvent debsEvent){

        debsEvent.setStartCellNo(cellIdProcessor.execute(debsEvent.getPickup_longitude(), debsEvent.getPickup_latitude()));
        debsEvent.setEndCellNo(cellIdProcessor.execute(debsEvent.getDropoff_longitude(), debsEvent.getDropoff_latitude()));
        debsEvent.setPickup_datetime(timeStampProcessor.execute(debsEvent.getPickup_datetime_org()));
        debsEvent.setDropoff_datetime(timeStampProcessor.execute(debsEvent.getDropoff_datetime_org()));

        sendForQ1(debsEvent);
        sendForQ2(debsEvent);
    }

    ExternalTimeWindowProcessor externalTimeWindowProcessor1 = new ExternalTimeWindowProcessor(30 * 60 * 1000);
    MaxKQ1Processor maxKQ1Processor = new MaxKQ1Processor();
    StringBuilder stringBuilder2 = new StringBuilder();

    public  void sendForQ1(DebsEvent debsEvent){

        List<DebsEvent> afterThirtyMinWindow =externalTimeWindowProcessor1.process(debsEvent);

        for (DebsEvent eve : afterThirtyMinWindow) {
            Object[] topK = maxKQ1Processor.process(eve);
            long currentTime = System.currentTimeMillis();
            if (topK != null) {
                if (performanceLoggingFlag) {
                    long eventOriginationTime = debsEvent.getIij_timestamp();
                    long latency = currentTime - eventOriginationTime;

                    stringBuilder2.append(eve.pickup_datetime_org);
                    stringBuilder2.append(COMMA);
                    stringBuilder2.append(eve.dropoff_datetime_org);
                    stringBuilder2.append(COMMA);

                    for(Object item:topK){
                        stringBuilder2.append(item);
                        stringBuilder2.append(COMMA);
                    }

                    stringBuilder2.append(latency);
                    stringBuilder2.append(CARRIAGERETURN_NEWLINE);

                    perfStats1.count++;
                    perfStats1.totalLatency += latency;
                    perfStats1.lastEventTime = currentTime;
                }
            }
        }

        if (printOutputFlag) {
            try {
                bw.write(stringBuilder2.toString());
                bw.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                stringBuilder2.setLength(0);
            }
        }
    }

    ExternalTimeWindowProcessor externalTimeWindowProcessor = new ExternalTimeWindowProcessor(15 * 60 * 1000);
    GroupByExecutor groupByExecutor = new GroupByExecutor();
    EmptyTaxiProcessor emptyTaxiProcessor = new EmptyTaxiProcessor();
    MaxKQ2Processor maxKQ2Processor = new MaxKQ2Processor();
    StringBuilder stringBuilder = new StringBuilder();

    public  void sendForQ2(DebsEvent debsEvent) {
        List<DebsEvent> windowOutputList = externalTimeWindowProcessor.process(debsEvent);
        for (DebsEvent event : windowOutputList) {
            float profit = groupByExecutor.execute(event.getStartCellNo(), event.getFare_plus_ip_amount(), event.isCurrent());
            event.setProfit(profit);
        }

        debsEvent.setListAfterFirstWindow(windowOutputList);
        for (DebsEvent event : debsEvent.getListAfterFirstWindow()) {
            emptyTaxiProcessor.process(event);
        }

        List<DebsEvent> after = debsEvent.getListAfterFirstWindow();
        for (DebsEvent eve : after) {
            for (ProfitObj profitObj : eve.getProfitObjList()) {
                Object[] maxKOutPut = maxKQ2Processor.processEventForMaxK(profitObj, eve.isCurrent());
                long currentTime = System.currentTimeMillis();
                if (maxKOutPut != null) {
                    if (performanceLoggingFlag) {

                        long eventOriginationTime = debsEvent.getIij_timestamp();
                        long latency = currentTime - eventOriginationTime;

                        stringBuilder.append(debsEvent.pickup_datetime_org);
                        stringBuilder.append(COMMA);
                        stringBuilder.append(eve.dropoff_datetime_org);
                        stringBuilder.append(COMMA);

                        for(Object item:maxKOutPut){
                            stringBuilder.append(item);
                            stringBuilder.append(COMMA);
                        }

                        stringBuilder.append(latency);
                        stringBuilder.append(CARRIAGERETURN_NEWLINE);

                        perfStats2.count++;
                        perfStats2.totalLatency += latency;
                        perfStats2.lastEventTime = currentTime;
                    }
                }
            }
        }

        if (printOutputFlag) {
            try {
                bw2.write(stringBuilder.toString());
                bw2.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                stringBuilder.setLength(0);
            }
        }
    }





}