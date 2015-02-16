/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.cms.admin.validators;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.StringValidator;
import org.hippoecm.frontend.plugins.cms.admin.users.User;

public class UsernameValidator extends StringValidator {
    private static final long serialVersionUID = 1L;

    public void validate(IValidatable validatable) {
        super.validate(validatable);

        String username = (String) validatable.getValue();
        if (User.userExists(username)) {
            validatable.error(new ValidationError(this, "exists"));
        }
    }
}
