/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
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

package org.apache.usergrid.persistence.index.impl;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.IndexOperationMessage;

import com.google.inject.Inject;
import com.google.inject.Singleton;


@Singleton
public class BufferQueueInMemoryImpl implements BufferQueue {

    private final ArrayBlockingQueue<IndexOperationMessage> messages;


    @Inject
    public BufferQueueInMemoryImpl( final IndexFig fig ) {
        messages = new ArrayBlockingQueue<>( fig.getIndexQueueSize() );
    }


    @Override
    public void offer( final IndexOperationMessage operation ) {
        messages.offer( operation );
        operation.done();
    }


    @Override
    public List<IndexOperationMessage> take( final int takeSize, final long timeout, final TimeUnit timeUnit ) {

        final List<IndexOperationMessage> response = new ArrayList<>( takeSize );

        final long endTime = System.currentTimeMillis() + timeUnit.toMillis( timeout );

        //loop until we're we're full or we time out
        do {
            try {

                final long remaining = endTime - System.currentTimeMillis();

                //we received 1, try to drain
                IndexOperationMessage polled = messages.poll( remaining, timeUnit );

                //drain
                if ( polled != null ) {
                    response.add( polled );
                    messages.drainTo( response, takeSize - response.size() );
                }
            }
            catch ( InterruptedException ie ) {
                //swallow

            }
        }
        while ( response.size() < takeSize && System.currentTimeMillis() < endTime );

        return response;
    }


    @Override
    public void ack( final List<IndexOperationMessage> messages ) {
         //no op for this
    }
}