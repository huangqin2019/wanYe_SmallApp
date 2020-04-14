package com.mindor.serivce;


import com.mindor.entity.TimeDelay;

public interface TimeDelayService {
	public Integer addTimeDelay(TimeDelay timeDelay);
    public void updateTimeDelay(TimeDelay timeDelay);
    public TimeDelay selectTimeDelays(String equipmentId,String userId);
    public void deleteTimeDelay(int timeDelayId);
    public void deleteTimeDelays(String equipmentId,String userId);
    public void deleteTimeDelays(String equipmentId);
    public TimeDelay selectTimeDelay(int timeDelayId);
}