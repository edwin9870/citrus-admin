/*
 * Copyright 2006-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.consol.citrus.admin.converter.endpoint;

import com.consol.citrus.admin.converter.AbstractObjectConverter;
import com.consol.citrus.admin.exception.ApplicationRuntimeException;
import com.consol.citrus.admin.model.EndpointModel;
import com.consol.citrus.admin.model.Property;
import com.consol.citrus.message.MessageConverter;
import com.consol.citrus.message.MessageCorrelator;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import javax.xml.bind.annotation.XmlSchema;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

/**
 * Abstract endpoint converter provides basic endpoint property handling by Java reflection on JAXb objects.
 *
 * @author Christoph Deppisch
 */
public abstract class AbstractEndpointConverter<S> extends AbstractObjectConverter<EndpointModel, S> implements EndpointConverter<S> {

    protected static final String TRUE = "true";
    protected static final String FALSE = "false";

    @Override
    public EndpointModel convert(S model) {
        EndpointModel endpointModel = new EndpointModel(getEndpointType(), getId(model), getSourceModelClass());

        ReflectionUtils.doWithFields(getSourceModelClass(), field -> {
            Property property = property(getFieldName(field), getDisplayName(getFieldName(field)), model, getDefaultValue(field), isRequiredField(field))
                    .options(getFieldOptions(field))
                    .optionType(getOptionType(field));

            endpointModel.add(property);
        });

        return endpointModel;
    }

    /**
     * Subclasses must provide id from model.
     * @param model
     * @return
     */
    protected abstract String getId(S model);

    @Override
    public String getEndpointType() {
        String endpointNamespace = getSourceModelClass().getPackage().getAnnotation(XmlSchema.class).namespace();
        return endpointNamespace.substring("http://www.citrusframework.org/schema/".length(), endpointNamespace.indexOf("/config"));
    }

    @Override
    public S convertBack(EndpointModel definition) {
        try {
            S instance = getSourceModelClass().newInstance();

            ReflectionUtils.invokeMethod(findSetter(getSourceModelClass(), "id"), instance, definition.getId());

            for (Property property : definition.getProperties()) {
                if (property.getValue() != null) {
                    Method setter = findSetter(getSourceModelClass(), property.getFieldName());
                    ReflectionUtils.invokeMethod(setter, instance, getMethodArgument(setter.getParameterTypes()[0], property.getValue()));
                }
            }

            return instance;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ApplicationRuntimeException("Failed to instantiate model class", e);
        }
    }

    @Override
    public Class<EndpointModel> getTargetModelClass() {
        return EndpointModel.class;
    }

    /**
     * Evaluates if field is required.
     * @param field
     * @return
     */
    protected boolean isRequiredField(Field field) {
        return Stream.of(getRequiredFields()).parallel().anyMatch(name -> field.getName().equals(name));
    }

    /**
     * Optional list of required field names.
     * @return
     */
    protected String[] getRequiredFields() {
        return new String[] {};
    }

    /**
     * Evaluates the target field name.
     * @param field
     * @return
     */
    protected Object getDefaultValue(Field field) {
        return getDefaultValueMappings().entrySet().stream()
                .filter(entry -> entry.getKey().equals(field.getName()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseGet(() -> {
                    if (field.getType().equals(Boolean.class)) {
                        return FALSE;
                    } else {
                        return null;
                    }
                });
    }

    /**
     * Optional mappings for field names to default values.
     * @return
     */
    protected Map<String, Object> getDefaultValueMappings() {
        Map<String, Object> mappings = new HashMap<>();
        mappings.put("timeout", "5000");
        mappings.put("pollingInterval", "500");
        mappings.put("autoStart", TRUE);
        return mappings;
    }

    /**
     * Evaluates the target field name.
     * @param field
     * @return
     */
    protected String getFieldName(Field field) {
        return getFieldNameMappings().entrySet().stream()
                .filter(entry -> entry.getKey().equals(field.getName()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(field.getName());
    }

    /**
     * Optional mappings for field names.
     * @return
     */
    protected Map<String, String> getFieldNameMappings() {
        return Collections.emptyMap();
    }

    /**
     * Evaluates the display name.
     * @param fieldName
     * @return
     */
    protected String getDisplayName(String fieldName) {
        return getDisplayNameMappings().entrySet().stream()
                .filter(entry -> entry.getKey().equals(fieldName))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(StringUtils.capitalize(fieldName));
    }

    /**
     * Optional mappings for field names.
     * @return
     */
    protected Map<String, String> getDisplayNameMappings() {
        Map<String, String> mappings = new HashMap<>();
        mappings.put("actor", "TestActor");
        return mappings;
    }

    /**
     * Evaluates option type for given field or null if not applicable.
     * @param field
     * @return
     */
    protected Class<?> getOptionType(Field field) {
        return getOptionTypeMappings().entrySet().stream()
                .filter(entry -> entry.getKey().equals(field.getName()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * Optional type mappings for fields. Keys are field names.
     * @return
     */
    protected Map<String, Class<?>> getOptionTypeMappings() {
        Map<String, Class<?>> mappings = new HashMap<>();
        mappings.put("messageCorrelator", MessageCorrelator.class);
        mappings.put("messageConverter", MessageConverter.class);
        return mappings;
    }

    /**
     * Evaluates field options if any.
     * @param field
     * @return
     */
    protected String[] getFieldOptions(Field field) {
        return getFieldOptionMappings().entrySet().stream()
                .filter(entry -> entry.getKey().equals(field.getName()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseGet(() -> {
                    if (field.getType().equals(Boolean.class)) {
                        return new String[] {TRUE, FALSE};
                    } else {
                        return new String[]{};
                    }
                });
    }

    /**
     * Optional field options. Keys are field names.
     * @return
     */
    protected Map<String, String[]> getFieldOptionMappings() {
        return Collections.emptyMap();
    }

    /**
     * Finds setter method on class for given field name.
     * @param modelClass
     * @param fieldName
     * @return
     */
    private Method findSetter(Class<S> modelClass, String fieldName) {
        final Method[] setter = {null};

        ReflectionUtils.doWithMethods(modelClass, method -> {
            if (method.getName().equals("set" + StringUtils.capitalize(fieldName))) {
                setter[0] = method;
            }
        }, method -> method.getName().startsWith("set"));

        if (setter[0] == null) {
            throw new ApplicationRuntimeException(String.format("Unable to find proper setter for field '%s' on model class '%s'", fieldName, modelClass));
        }

        return setter[0];
    }

    /**
     * Gets properly typed method argument.
     * @param parameterType
     * @param value
     * @return
     */
    private <T> T getMethodArgument(Class<T> parameterType, Object value) {
        if (parameterType.isInstance(value)) {
            return parameterType.cast(value);
        }

        try {
            return new SimpleTypeConverter().convertIfNecessary(value, parameterType);
        } catch (ConversionNotSupportedException e) {
            if (String.class.equals(parameterType)) {
                return (T) String.valueOf(value);
            }

            throw new ApplicationRuntimeException("Unable to convert method argument type", e);
        }
    }
}
