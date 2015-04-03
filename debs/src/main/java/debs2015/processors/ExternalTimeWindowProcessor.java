/*
* Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org)
* All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package debs2015.processors;

import debs2015.DebsEvent;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class ExternalTimeWindowProcessor {
    static final Logger log = Logger.getLogger(ExternalTimeWindowProcessor.class);
    private long timeToKeep;
    LinkedBlockingQueue<DebsEvent> expiredEventQueue = new LinkedBlockingQueue<DebsEvent>();


    public ExternalTimeWindowProcessor(long timeToKeep){
        this.timeToKeep = timeToKeep;
    }

    public List<DebsEvent> process(DebsEvent debsEvent) {

        List<DebsEvent> returnList = new ArrayList<DebsEvent>();

        long currentTime = debsEvent.getDropoff_datetime();

        DebsEvent clonedEvent = debsEvent.clone();
        clonedEvent.setCurrent(false);
        clonedEvent.setTimeStamp(currentTime + timeToKeep);

        Iterator<DebsEvent> iterator = expiredEventQueue.iterator();

        while (iterator.hasNext()){
            DebsEvent expiredEvent =iterator.next();
            long timeDiff = expiredEvent.getTimeStamp() - currentTime;
            if (timeDiff <= 0) {
                iterator.remove();
                returnList.add(expiredEvent);
            } else {
                break;
            }
        }

        expiredEventQueue.add(clonedEvent);
        returnList.add(debsEvent);
        return returnList;

    }


}
