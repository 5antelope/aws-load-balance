
public class Launcher 
{
	public static void main(String args[]) throws Exception
	{
		ELBRequests elb= new ELBRequests();
		
    	String ELBDNS = elb.getDNS();
    	
    	Thread.sleep(60000); //60 sec
    	System.out.println("finish sleep");
    	
		LoaderRequests loaderRequests = new LoaderRequests();
    	loaderRequests.active();
    	
    	System.out.println("auto scale..");
    	
    	AutoScalingGroup asg = new AutoScalingGroup();
    	
    	System.out.println("start check");
    	
    	Thread.sleep(120*1000);
    	elb.checkELB();
    	System.out.println("checked elb...");
    	Thread.sleep(120*1000);
    	
    	loaderRequests.warmUp(ELBDNS);
    	
    	System.out.println("warmed up...");
    	Thread.sleep(30*1000);
    	
    	loaderRequests.phase2(ELBDNS);
    	
    	asg.shutDown();
    	System.out.println("cleaned");
	}
}
