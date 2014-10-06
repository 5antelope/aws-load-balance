import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.DeleteAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.DeleteLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.InstanceMonitoring;
import com.amazonaws.services.autoscaling.model.PutScalingPolicyRequest;
import com.amazonaws.services.autoscaling.model.PutScalingPolicyResult;
import com.amazonaws.services.autoscaling.model.Tag;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest;


public class AutoScalingGroup {
	AmazonAutoScalingClient asg;
	BasicAWSCredentials bawsc;
	
	String scalUpArn;
	String scalDownArn;
	
	public AutoScalingGroup() throws Exception
	{
		init();
	}
	
	private void init() throws Exception
	{
		Properties property = new Properties();
		property.load(LoaderRequests.class.getResourceAsStream("/AwsCredentials.properties"));
		bawsc = new BasicAWSCredentials(property.getProperty("AWSAccessKeyId"), property.getProperty("AWSSecretKey"));
		
		asg = new AmazonAutoScalingClient(bawsc);
		
		createAutoScalingGroup();
		
		setScalingPolicy();
		
		setCloudWatch();
	}
	
	private void createAutoScalingGroup()
	{
		Tag tags = new Tag();
		tags
			.withKey("Project")
			.withValue("2.3");
		
		InstanceMonitoring instanceMonitor = new InstanceMonitoring();
		instanceMonitor.setEnabled(true);
		CreateLaunchConfigurationRequest launchConfigurationRequest = new CreateLaunchConfigurationRequest();
		launchConfigurationRequest
			.withImageId("ami-ec14ba84")
			.withInstanceType("m3.medium") //t1.micro
			.withInstanceMonitoring(instanceMonitor)
			.withLaunchConfigurationName("DataCenterConfig")
			.withSecurityGroups("TCP");
		asg.createLaunchConfiguration(launchConfigurationRequest);
		System.out.println("launch configuration completed...");
		
		CreateAutoScalingGroupRequest ASGRequest = new CreateAutoScalingGroupRequest();
		ASGRequest
			.withMaxSize(5)
			.withMinSize(3)
			.withDesiredCapacity(3)
			.withAutoScalingGroupName("cc2.2ASG") // auto scaling group name
			.withLaunchConfigurationName("DataCenterConfig")
			.withAvailabilityZones("us-east-1d")
			.withLoadBalancerNames("MySimpleELB")
			.withTags(tags);
		asg.createAutoScalingGroup(ASGRequest);
		System.out.println("auto scaling group completed...");
	}
	
	private void setScalingPolicy()
	{
		PutScalingPolicyRequest scalingUp = new PutScalingPolicyRequest();
		scalingUp.setAutoScalingGroupName("cc2.2ASG");
		scalingUp.setPolicyName("scaleUpPolicy");
		scalingUp.setScalingAdjustment(1);
		scalingUp.setAdjustmentType("ChangeInCapacity");
		scalingUp.setCooldown(60);
		asg.putScalingPolicy(scalingUp);
		
		PutScalingPolicyRequest scalingDown = new PutScalingPolicyRequest();
		scalingDown.setAutoScalingGroupName("cc2.2ASG");
		scalingDown.setPolicyName("scaleDownPolicy");
		scalingDown.setScalingAdjustment(-1);
		scalingDown.setAdjustmentType("ChangeInCapacity");
		scalingDown.setCooldown(60);
		asg.putScalingPolicy(scalingDown);
		
		PutScalingPolicyResult scaleUpResult = asg.putScalingPolicy(scalingUp);
		scalUpArn = scaleUpResult.getPolicyARN();
		PutScalingPolicyResult scaleDownResult = asg.putScalingPolicy(scalingDown);
		scalDownArn = scaleDownResult.getPolicyARN();
		
		if (scalUpArn==null && scalDownArn==null)
		{
			System.out.println("ARNs are NULL..");
			System.exit(0);
		}
		System.out.println("Policies setup completed...");
	}
	
	private void setCloudWatch()
	{
		AmazonCloudWatchClient cloudWatchClient = new AmazonCloudWatchClient(bawsc);
		
		List<Dimension> dimensions = new ArrayList<Dimension>();
        Dimension dimension = new Dimension();
        dimension.setName("AutoScalingGroupDimension");
        dimension.setValue("cc2.2ASG");
        
        PutMetricAlarmRequest upRequest = new PutMetricAlarmRequest();
        upRequest.setDimensions(dimensions);
		upRequest.setAlarmName("AlarmName-up");
		//upRequest.setMetricName("CPUUtilization");
		upRequest.setMetricName("NetworkIn");
		upRequest.setStatistic("Average");
		upRequest.setThreshold(75d);
		upRequest.setPeriod(1*60); //1 min
		upRequest.setComparisonOperator("GreaterThanOrEqualToThreshold");
		upRequest.setEvaluationPeriods(1);
		upRequest.setNamespace("AWS/EC2");
		upRequest.withAlarmActions(scalUpArn);
		
		PutMetricAlarmRequest downRequest = new PutMetricAlarmRequest();
		downRequest.setDimensions(dimensions);
		downRequest.setAlarmName("AlarmName-down");
		//downRequest.setMetricName("CPUUtilization");
		downRequest.setMetricName("NetworkIn");
		downRequest.setStatistic("Average");
		downRequest.setThreshold(50d);
		downRequest.setPeriod(3*60); //2 min
		downRequest.setComparisonOperator("LessThanOrEqualToThreshold");
		downRequest.setEvaluationPeriods(1);
		downRequest.setNamespace("AWS/EC2");
		downRequest.withAlarmActions(scalDownArn);
		
		cloudWatchClient.putMetricAlarm(upRequest);
		cloudWatchClient.putMetricAlarm(downRequest);
		
		System.out.println("Cloud watch setup completed...");
	}
	
	void shutDown() throws Exception
	{
		UpdateAutoScalingGroupRequest updateAutoScalingGroupRequest = new UpdateAutoScalingGroupRequest();
    	updateAutoScalingGroupRequest
    		.withAutoScalingGroupName("cc2.2ASG")
    		.withMaxSize(0)
    		.withMinSize(0)
    		.withDesiredCapacity(0);
    	DeleteAutoScalingGroupRequest deleteAutoScalingGroupRequest = new DeleteAutoScalingGroupRequest();
    	Thread.sleep(60*1000);
    	
    	deleteAutoScalingGroupRequest.withAutoScalingGroupName("cc2.2ASG");
    	DeleteLaunchConfigurationRequest deleteLaunchConfigurationRequest = new DeleteLaunchConfigurationRequest();
    	deleteLaunchConfigurationRequest.withLaunchConfigurationName("DataCenterConfig");
    	
    	asg.updateAutoScalingGroup(updateAutoScalingGroupRequest);
    	Thread.sleep(90*1000);
    	asg.deleteAutoScalingGroup(deleteAutoScalingGroupRequest);
    	Thread.sleep(30*1000);
    	asg.deleteLaunchConfiguration(deleteLaunchConfigurationRequest);
	}
}
