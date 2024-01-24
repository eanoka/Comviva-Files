package com.grameenphone.wipro.fmfs.mfs_communicator.controller;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.grameenphone.wipro.annot.SchedulerContainingRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.ManualSchedulerResolver;

@RestController
@RequestMapping("/schedulers")
public class ManualSchedulerController {
    
    @Autowired
    public BeanFactory beanFactory;
    
    @Autowired
    private List<ManualSchedulerResolver> schedulerList;
    
    @RequestMapping(value = "/list")
	public Map<String, String> getSchedulerList() {
    	Map<String, String> map = new HashMap<>();

		for (ManualSchedulerResolver schedular : schedulerList) {
			Class myClass = schedular.getClass();
			Method[] methods = myClass.getMethods();

			for (Method method : methods) {
				if (method.isAnnotationPresent(SchedulerContainingRequest.class)) {
					SchedulerContainingRequest schedulerAnno = method.getAnnotation(SchedulerContainingRequest.class);
					map.put(schedulerAnno.name(), schedulerAnno.value());
				}
			}
		}
		return map;
	}
    
	@RequestMapping(value = "/trigger")
	public String triggerScheduler(@RequestParam String schedulerId) {
		String[] split = schedulerId.split("_");
		String className = split[0];
		String methodName = split[1];
		try {
			Object executorBean = beanFactory.getBean(className);
			if (executorBean instanceof ManualSchedulerResolver) {
				for (Method m : executorBean.getClass().getMethods()) {
					if (m.getName().equals(methodName) && m.isAnnotationPresent(SchedulerContainingRequest.class)) {
						m.invoke(executorBean, null);
					}
				}
			}
		} catch (Exception e) {

		}
		return "Ok";
	}
}