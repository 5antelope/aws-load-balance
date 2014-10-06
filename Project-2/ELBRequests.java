import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.ConfigureHealthCheckRequest;
import com.amazonaws.services.elasticloadbalancing.model.ConfigureHealthCheckResult;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthResult;
import com.amazonaws.services.elasticloadbalancing.model.HealthCheck;
import com.amazonaws.services.elasticloadbalancing.model.InstanceState;
import com.amazonaws.services.elasticloadbalancing.model.Listener;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.elasticloadbalancing.model.Tag;


public class ELBRequests{
	
	AmazonElasticLoadBalancingClient elb;
	String DNS;
	
	public ELBRequests() throws Exception
	{
		init();	
	}
	
	private void init() throws Exception
	{
		Properties property = new Properties();
		property.load(LoaderRequests.class.getResourceAsStream("/AwsCredentials.properties"));
		
		BasicAWSCredentials bawsc = 
				new BasicAWSCredentials(property.getProperty("AWSAccessKeyId"), property.getProperty("AWSSecretKey"));
		
		elb = new AmazonElasticLoadBalancingClient(bawsc);
		
		CreateLoadBalancerRequest lbRequest = new CreateLoadBalancerRequest();
		lbRequest.setLoadBalancerName("MySimpleELB");
		lbRequest.withSecurityGroups("sg-b2d8f4d7");
		List<Listener> listeners = new ArrayList<Listener>(1);
        listeners.add(new Listener("HTTP", 80, 80));
        lbRequest.withAvailabilityZones("us-east-1d");
        lbRequest.setListeners(listeners);
        Tag tag = new Tag();
        tag.setKey("Project");
        tag.setValue("2.2");
		List<Tag> tagList = new ArrayList<Tag>();
		tagList.add(tag);
        lbRequest.setTags(tagList);

        CreateLoadBalancerResult lbResult = elb.createLoadBalancer(lbRequest);
        System.out.println("created load balancer completed. ELB: "+lbResult.getDNSName());
        
        healthCheck();
        fetchDNS();
	}
	
	private void healthCheck()
	{
		HealthCheck healthCheck = new HealthCheck()
			.withTarget("HTTP:80/heartbeat?username=yangwu")
			.withHealthyThreshold(2)
			.withUnhealthyThreshold(10)
	        .withInterval(120)
	        .withTimeout(60);
		
		ConfigureHealthCheckRequest checkrequest = new ConfigureHealthCheckRequest("MySimpleELB", healthCheck);
		ConfigureHealthCheckResult healthResult = elb.configureHealthCheck(checkrequest);
		System.out.println("HealthCheck completed. Ping: "+healthResult.getHealthCheck().getTarget());
	}
	
	private void fetchDNS()
	{
        List<LoadBalancerDescription> loadBalancers = elb.describeLoadBalancers().getLoadBalancerDescriptions();
        for(int i = 0; i<loadBalancers.size(); i++)
        {
        	if(loadBalancers.get(i).getLoadBalancerName().equals("MySimpleELB"))
        	{
        		System.out.println("ELB DNS:"+loadBalancers.get(i).getDNSName());
        		DNS = loadBalancers.get(i).getDNSName();
        	}
        }
	}

	public String getDNS() 
	{
		return DNS;
	}
	
	public void checkELB()
	{
		boolean isInService = false;
		
		while(!isInService)
		{
			DescribeInstanceHealthRequest describeInstanceHealthRequest = 
					new DescribeInstanceHealthRequest().withLoadBalancerName("MySimpleELB");
			DescribeInstanceHealthResult describeInstanceHealthResult= elb.describeInstanceHealth(describeInstanceHealthRequest);
			List<InstanceState> instanceStates = describeInstanceHealthResult.getInstanceStates();
			
			isInService = true;
			
			if(instanceStates.size()==0)
				isInService = false;
			else{
				for(int i = instanceStates.size()-1; i>=0; i--)
				{
					boolean temp = instanceStates.get(i).toString().contains("InService");
					isInService = isInService && temp;
				}
			}
		}
	}
	
	public void delete() throws Exception
	{
		DeleteLoadBalancerRequest deleteLoadBalancerRequest = new DeleteLoadBalancerRequest();
		deleteLoadBalancerRequest.withLoadBalancerName("MySimpleELB");
		System.out.println("deleting load-balancer...");
		Thread.sleep(30*1000);
		elb.deleteLoadBalancer(deleteLoadBalancerRequest);
	}
}
