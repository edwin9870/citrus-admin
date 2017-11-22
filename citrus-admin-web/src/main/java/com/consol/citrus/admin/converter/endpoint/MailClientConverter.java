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

import com.consol.citrus.model.config.mail.MailClientModel;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author Christoph Deppisch
 */
@Component
public class MailClientConverter extends AbstractEndpointConverter<MailClientModel> {

    @Override
    protected String getId(MailClientModel model) {
        return model.getId();
    }

    @Override
    protected String[] getRequiredFields() {
        return new String[] { "host", "port" };
    }

    @Override
    protected Map<String, Object> getDefaultValueMappings() {
        Map<String, Object> mappings = super.getDefaultValueMappings();
        mappings.put("host", "localhost");
        mappings.put("port", "2222");
        mappings.put("protocol", JavaMailSenderImpl.DEFAULT_PROTOCOL);
        return mappings;
    }

    @Override
    public Class<MailClientModel> getSourceModelClass() {
        return MailClientModel.class;
    }

    @Override
    public String getEndpointType() {
        return "mail-client";
    }
}
