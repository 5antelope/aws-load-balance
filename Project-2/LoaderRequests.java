import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.RequestSpotInstancesResult;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;

public class LoaderRequests {
	
	AmazonEC2Client ec2;
	RequestSpotInstancesResult requestResult;
	RunInstancesRequest runInstancesRequest;
	RunInstancesResult runInstancesResult;
	
	URLRequests urlRequests;
	String DNS;
	
	public LoaderRequests() throws Exception
	{
		
	}
	
	private void init() throws Exception
	{
		Properties property = new Properties();
		property.load(LoaderRequests.class.getResourceAsStream("/AwsCredentials.properties"));
		
		BasicAWSCredentials bawsc = 
				new BasicAWSCredentials(property.getProperty("AWSAccessKeyId"), property.getProperty("AWSSecretKey"));
		
		ec2 = new AmazonEC2Client(bawsc);
		// launch a loader
		runInstancesRequest = new RunInstancesRequest();
		runInstancesRequest.withImageId("ami-562d853e")
		.withInstanceType("m3.medium") //t1.micro
		.withMinCount(1)
		.withMaxCount(1)
		.withKeyName("Project_1.1")
		.withSecurityGroups("TCP");
		
		runInstancesResult = ec2.runInstances(runInstancesRequest);
		
		Instance instance = runInstancesResult.getReservation().getInstances().get(0);
		
		String insID = instance.getInstanceId();
		
		DNS = fetchDNS(insID);
		System.out.println("loader:"+DNS);
        
	}
	
	String fetchDNS(String id) throws Exception
	{
		Instance instance;
		while(true)
		{	
			List<Reservation> reservations = ec2.describeInstances().getReservations();
			//System.out.println("size: "+reservations.size());
			for(int i = reservations.size()-1; i>=0; i--)
			{
				List<Instance> instances = reservations.get(i).getInstances();
				for (int j = instances.size()-1; j>=0; j--)
				{
					instance = instances.get(j);
					//System.out.println("on id: "+ instance.getInstanceId()+" "+instance.getState().getName());
					if(instance.getState().getName().equals("running") 
							&& instance.getInstanceId().equals(id))
					{
						 if(instance.getPublicDnsName()!=null)
				            	return instance.getPublicDnsName();
						 else
							 return "error on fetch public DNS...";
					}
				}
			}
			//System.out.println("Instance pending");
		    Thread.sleep(10000);
		}
	}
	
	public String getDNS() {
		return DNS;
	}

	void active()
	{
		String publicDNS = "http://"+DNS+"/username?username=yangwu";
		 
		URL url = null;
		try {
			url = new URL(publicDNS);
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
		
		HttpURLConnection con = null;
		
		while(true)
		{
			try{
				con = (HttpURLConnection) url.openConnection();
				BufferedReader in = new BufferedReader(
				        new InputStreamReader(con.getInputStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();
		 
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();
		 
				System.out.println(response.toString());
				break;
			}catch (Exception e)
			{
				// do nothing
			}
		}
		
		System.out.println("Actived!");        
	}
	
	void register(String DB) throws InterruptedException
	{
		String address = "http://"+DNS+"/part/one/i/want/more?dns="+DB+"&testId=test";
		System.out.println(address);
		URL url = null;
		Thread.sleep(30000); //30s
		while(true)
		{
			try {
				url = new URL(address);
				HttpURLConnection con = (HttpURLConnection)url.openConnection();
				BufferedReader in = new BufferedReader(
				        new InputStreamReader(con.getInputStream()));
		 
				if (in.readLine() != null) 
				{
					System.out.println("registered!");
					in.close();
					break;
				}
			} catch (Exception e) 
			{
				//e.printStackTrace();
			}
			Thread.sleep(30000); //30s
		}
	}
	
	boolean check() throws InterruptedException
	{
		String http = "http://"+DNS+"/view-logs?name=result_yangwu_test.txt";
		Thread.sleep(60000); // 60s
		while(true)
		{
			try{
				URL url = new URL(http);
				HttpURLConnection con = (HttpURLConnection)url.openConnection();
				BufferedReader in = new BufferedReader(
				        new InputStreamReader(con.getInputStream()));
				String inputLine;
				String line = null;
		 
				while ((inputLine = in.readLine()) != null) {
					line = inputLine; // last line
				}
				in.close();
		 
				System.out.println(line);
				
				if(line.equals("Test completed"))
				{
					//System.out.println(line);
					return true;
				}
				else 
				{
					return false;
				}
			} catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	void warmUp(String ELBDNS) 
	{
		String http = "http://"+DNS+"/warmup?dns="+ELBDNS+"&testId=test";
		try {
			URL url = new URL(http);
			urlRequests = new URLRequests(url);
			Thread.sleep(5*61*1000); // 5 min check
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	void phase2(String ELBDNS)
	{
		System.out.println("start phase 2...");
		String http = "http://"+DNS+"/begin-phase-2?dns="+ELBDNS+"&testId=test";
		try {
			URL url = new URL(http);
			urlRequests = new URLRequests(url);
			phase2_result();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void phase2_result() throws Exception
	{
		System.out.println("wait 40 min..");
		System.out.println("http://"+DNS+"/view-logs?name=result_yangwu_test.txt");
		Thread.sleep(40*61*1000); // wait process to finish
		// see result
		URL resultURL;
		try {
			resultURL = new URL("http://"+DNS+"/view-logs?name=result_yangwu_test.txt");
			
			HttpURLConnection con = (HttpURLConnection)resultURL.openConnection();
			BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
			String inputLine;
			while ((inputLine = in.readLine()).length()<50) {}
			System.out.println("result: "+inputLine);
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void delete() throws Exception
	{
		TerminateInstancesRequest terminateInstancesRequest = new TerminateInstancesRequest();
		terminateInstancesRequest.withInstanceIds(instanceID);
		
		Thread.sleep(30*1000);
		System.out.println("ready to terminate loader...");
		
		ec2.terminateInstances(terminateInstancesRequest);
	}
}
