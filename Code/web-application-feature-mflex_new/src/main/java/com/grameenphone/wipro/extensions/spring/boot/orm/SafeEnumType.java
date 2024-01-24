package com.grameenphone.wipro.extensions.spring.boot.orm;

import org.hibernate.type.descriptor.java.EnumJavaType;

public class SafeEnumType<T extends Enum<T>> extends EnumJavaType<T>{
    

	public SafeEnumType(Class<T> type) {
        super(type);
    }

	@Override
    public T fromName(String relationalForm) {
        try {
            return super.fromName(relationalForm);
        } catch (IllegalArgumentException e) {
            return getDefaultValue();
        }
    }
	
	@Override
    public T getDefaultValue() {
        DefaultEnumValue defaultEnumValue = getJavaType().getAnnotation(DefaultEnumValue.class);
        if(defaultEnumValue == null) {
            throw new IllegalArgumentException();
        }
        return Enum.valueOf(getJavaType(), defaultEnumValue.value());
	}
}