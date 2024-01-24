package com.grameenphone.wipro.utility.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;
import com.grameenphone.wipro.extensions.spring.boot.extra.ExternalPropertySource;

import java.util.Properties;

/**
 * to use log.dir variable in logback.xml from external property source
 */
public class LoggerStartupListener extends ContextAwareBase implements LoggerContextListener, LifeCycle {
	private boolean started = false;

	@Override
	public void start() {
		if (started) return;
		Properties externalPropertySource = ExternalPropertySource.loadConfigProps();
		Context context = getContext();
		context.putProperty("log.dir", externalPropertySource.getProperty("log.dir"));
		context.putProperty("root.log.level", externalPropertySource.getProperty("root.log.level", "off"));
		context.putProperty("hibernate.log.level.sql", externalPropertySource.getProperty("hibernate.log.level.sql", "off"));
		context.putProperty("hibernate.log.level.param.binder", externalPropertySource.getProperty("hibernate.log.level.param.binder", "off"));
		context.putProperty("hibernate.log.level.param.extractor", externalPropertySource.getProperty("hibernate.log.level.param.extractor", "off"));
		started = true;
	}

	@Override
	public void stop() {
	}

	@Override
	public boolean isStarted() {
		return started;
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