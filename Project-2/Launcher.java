public class Launcher 
{
	public static void main(String args[]) throws Exception
	{
		ELBRequests elb= new ELBRequests();
		
    	String ELBDNS = elb.getDNS();
    	
    	Thread.sleep(60000); //60 sec
    	
		  LoaderRequests loaderRequests = new LoaderRequests();
    	loaderRequests.active();
    	
    	AutoScalingGroup asg = new AutoScalingGroup();
    	
    	System.out.println("start check");
    	
    	Thread.sleep(120*1000);
    	elb.checkELB();
    	
    	Thread.sleep(120*1000);
    	
    	loaderRequests.warmUp(ELBDNS);
    	
    	Thread.sleep(30*1000);
    	
    	loaderRequests.phase2(ELBDNS);
    	
    	asg.shutDown();
    	System.out.println("cleaned");
	}
}
