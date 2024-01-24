package com.grameenphone.wipro.fmfs.mfs_communicator.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;
import com.grameenphone.wipro.spring.boot.extra.ExternalPropertySource;

/**
 * to use log.dir variable in logback.xml from external property source
 */
public class LoggerStartupListener extends ContextAwareBase implements LoggerContextListener, LifeCycle {
	@Override
	public void start() {
		Context context = getContext();
		if(context.getProperty("log.dir") == null) { //Properties already set for this context
			ExternalPropertySource properties = new ExternalPropertySource();
			context.putProperty("log.dir", (String) properties.getProperty("log.dir"));
			context.putProperty("log.to.root", (String) properties.getProperty("log.to.root", "disable"));
			context.putProperty("root.log.level", (String) properties.getProperty("root.log.level", "off"));
			context.putProperty("hibernate.log.level.sql", (String) properties.getProperty("hibernate.log.level.sql", "off"));
			context.putProperty("hibernate.log.level.param.binder", (String) properties.getProperty("hibernate.log.level.param.binder", "off"));
			context.putProperty("hibernate.log.level.param.extractor", (String) properties.getProperty("hibernate.log.level.param.extractor", "off"));
		}
	}

	@Override
	public void stop() {
	}

	@Override
	public boolean isStarted() {
		Context context = getContext();
		return context.getProperty("log.dir") != null;
	}

	@Override
	public boolean isResetResistant() {
		return true;
	}

	@Override
	public void onStart(LoggerContext context) {
	}

	@Override
	public void onReset(LoggerContext context) {
	}

	@Override
	public void onStop(LoggerContext context) {
	}

	@Override
	public void onLevelChange(Logger logger, Level level) {
	}
}
