package com.grameenphone.wipro.extensions.spring.orm;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;

/**
 * Custom hibernate naming strategy to convert camel case identifiers to underscore case
 * configured in application.yml
 */
public class NamingStrategy extends SpringImplicitNamingStrategy {
	@Override
	protected Identifier toIdentifier(String stringForm, MetadataBuildingContext buildingContext) {
		return convertToSnakeCase(super.toIdentifier(stringForm, buildingContext));
	}

	private Identifier convertToSnakeCase(final Identifier identifier) {
		final String regex = "([^A-Z])([A-Z])";
		final String replacement = "$1_$2";
		final String newName = identifier.getText()
				.replaceAll(regex, replacement)
				.toLowerCase();
		return Identifier.toIdentifier(newName);
	}
}