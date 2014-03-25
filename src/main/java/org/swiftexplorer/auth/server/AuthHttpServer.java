/*
 * Copyright 2014 Loic Merckel
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.swiftexplorer.auth.server;


import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class AuthHttpServer implements SynchronousDataProvider<Map<String, String> > {

	final private Logger logger = LoggerFactory.getLogger(AuthHttpServer.class);
	
	final private int port ;
	private HttpServer server = null ;
	private ExecutorService httpThreadPool = null ;
	
	private volatile BlockingQueue<Map<String, String> > sharedQueue = null;
	private final Object lock = new Object () ;
	
	private static final Map<String, String> emptyMap = new HashMap <String, String> () ;
	
	public AuthHttpServer (int port)
	{
		super () ;
		this.port = port ;
	}
	
	
	@Override
	public void stopWaiting () throws InterruptedException
	{
		synchronized (lock)
		{
			if (sharedQueue == null)
				return ;
			sharedQueue.put(emptyMap);
		}
	}
	
	
	@Override
	public Map<String, String> startAndWaitForData () throws IOException, InterruptedException
	{
		InetSocketAddress addr = new InetSocketAddress(port);	
		BlockingQueue<Map<String, String> > blockingQueue = new LinkedBlockingQueue<Map<String, String> >();
		synchronized (lock)
		{
			sharedQueue = blockingQueue ;
			server = HttpServer.create(addr, 0);
			server.createContext("/", new HandlerMapParameter(blockingQueue));
			httpThreadPool = Executors.newCachedThreadPool() ;
			server.setExecutor(httpThreadPool);
			server.start();
		}
		return blockingQueue.poll(10 * 60, TimeUnit.SECONDS);
	}
	
	
	public void stopServer ()
	{
		synchronized (lock)
		{
			if (server == null)
				return ;
			server.stop(0);
			
			if (httpThreadPool == null) // should never happen...
				return ;
			
			httpThreadPool.shutdown(); 
			try 
			{ 
				httpThreadPool.awaitTermination(60, TimeUnit.SECONDS); 
			} 
			catch (InterruptedException e) 
			{			
				logger.error("Error occurred while stopping the server", e);
			}
		}
	}
	
	
	static private Map<String, String> queryToParameterMap (String query)
	{
	    if (query == null || query.isEmpty())
	    	return emptyMap ;
	    Map<String, String> ret = new HashMap<String, String>();
	    for (String str : query.split("&")) 
	    {
	        String paramNameVal[] = str.split("=");
	        if (paramNameVal.length > 1) 
	            ret.put(paramNameVal[0], paramNameVal[1]);
	        else
	            ret.put(paramNameVal[0], "");
	    }
	    return ret;
	}
	
	
	static private class HandlerMapParameter implements HttpHandler 
	{
		final private Logger logger = LoggerFactory.getLogger(HandlerMapParameter.class);
		
		final BlockingQueue<Map<String, String> > sharedQueue ;
		public HandlerMapParameter (BlockingQueue<Map<String, String> > sharedQueue)
		{
			super () ;
			this.sharedQueue = sharedQueue ;
		}
		
		@Override
		public void handle(HttpExchange exchange) throws IOException 
		{
			String requestMethod = exchange.getRequestMethod();
			if (requestMethod.equalsIgnoreCase("GET")) 
			{
				Headers responseHeaders = exchange.getResponseHeaders();
				responseHeaders.set("Content-Type", "text/plain");
				exchange.sendResponseHeaders(200, 0);
				
				OutputStream responseBody = exchange.getResponseBody();
				Headers requestHeaders = exchange.getRequestHeaders();
				Set<String> keySet = requestHeaders.keySet();
				Iterator<String> iter = keySet.iterator();
				while (iter.hasNext()) {
					String key = iter.next();
					List<String> values = requestHeaders.get(key);
					String s = key + " = " + values.toString() + "\n";
					responseBody.write(s.getBytes());		
				}
				responseBody.close();
					
				if (sharedQueue != null)
				{
					String query = exchange.getRequestURI().getQuery() ;
					try 
					{
						sharedQueue.put(queryToParameterMap(query));
					} 
					catch (InterruptedException e) 
					{
						logger.error("Error occurred in the server handler", e);
					}
				}
			}
		}
	}
}
