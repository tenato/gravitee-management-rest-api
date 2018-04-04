/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.management.service.exceptions;

import io.gravitee.repository.management.model.RoleScope;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RoleDeletionForbiddenException extends AbstractNotFoundException {

    private final RoleScope scope;
    private final String name;

    public RoleDeletionForbiddenException(RoleScope scope, String name) {
        this.scope = scope;
        this.name = name;
    }

    @Override
    public String getMessage() {
        return "Role [" + scope + "," + name + "] can not be deleted because marked as System or Default role.";
    }
}
