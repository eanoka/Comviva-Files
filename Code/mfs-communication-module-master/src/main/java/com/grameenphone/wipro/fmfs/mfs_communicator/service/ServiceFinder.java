package com.grameenphone.wipro.fmfs.mfs_communicator.service;

import org.springframework.stereotype.Service;

import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillPayer;
import com.grameenphone.wipro.utility.spring.ContextUtil;

@Service
public class ServiceFinder {
    public <T> T getServiceWithPrefix(String beanPrefix, String beanName, Class<T> serviceClass) {
        int sepIndex;
        do {
            var service = ContextUtil.getBean(beanPrefix + beanName, serviceClass);
            if (service != null)
                return service;
            sepIndex = beanPrefix.lastIndexOf(":");
            if (sepIndex != -1) {
                beanPrefix = beanPrefix.substring(0, sepIndex);
            }
        } while (sepIndex != -1);
        return null;
    }
}