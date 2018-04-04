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
package io.gravitee.management.rest.resource;

import io.gravitee.management.model.*;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.GroupService;
import io.gravitee.management.service.MembershipService;
import io.gravitee.management.service.UserService;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.swagger.annotations.Api;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.*;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static io.gravitee.common.http.MediaType.APPLICATION_JSON;

/**
 * Defines the REST resources to manage Users.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"User"})
public class UserResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private UserService userService;

    @Inject
    private MembershipService membershipService;

    @Inject
    private GroupService groupService;

    @GET
    @Produces(APPLICATION_JSON)
    @Permissions(
            @Permission(value = RolePermission.MANAGEMENT_USERS, acls = RolePermissionAction.READ)
    )
    public UserEntity getUser(@PathParam("id") String userId) {
        UserEntity user = userService.findByIdWithRoles(userId);

        // Delete password for security reason
        user.setPassword(null);
        user.setPicture(null);

        return user;
    }

    @DELETE
    @Permissions(
            @Permission(value = RolePermission.MANAGEMENT_USERS, acls = RolePermissionAction.DELETE)
    )
    public Response deleteUser(@PathParam("id") String userId) {
        userService.delete(userId);
        return Response.noContent().build();
    }

    @GET
    @Path("/groups")
    @Produces(APPLICATION_JSON)
    @Permissions(
            @Permission(value = RolePermission.MANAGEMENT_USERS, acls = RolePermissionAction.READ)
    )
    public List<UserGroupEntity> getGroups(@PathParam("id") String userId) {
        List<UserGroupEntity> groups = new ArrayList<>();
        RoleScope[] scopes = {RoleScope.API, RoleScope.APPLICATION};
        groupService.findByUser(userId).forEach(groupEntity -> {
            UserGroupEntity userGroupEntity = new UserGroupEntity();
            userGroupEntity.setId(groupEntity.getId());
            userGroupEntity.setName(groupEntity.getName());
            userGroupEntity.setRoles(new HashMap<>());
            for (RoleScope scope: scopes) {
                RoleEntity role = membershipService.getRole(MembershipReferenceType.GROUP, groupEntity.getId(), userId, scope);
                if (role != null) {
                    userGroupEntity.getRoles().put(role.getScope().name(), role.getName());
                }
            }
            groups.add(userGroupEntity);
        });

        return groups;
    }

    @GET
    @Path("/avatar")
    public Response getUserAvatar(@PathParam("id") String id, @Context Request request) {
        PictureEntity picture = userService.getPicture(id);

        if (picture == null) {
            throw new NotFoundException();
        }

        if (picture instanceof UrlPictureEntity) {
            return Response.temporaryRedirect(URI.create(((UrlPictureEntity)picture).getUrl())).build();
        }

        CacheControl cc = new CacheControl();
        cc.setNoTransform(true);
        cc.setMustRevalidate(false);
        cc.setNoCache(false);
        cc.setMaxAge(86400);

        InlinePictureEntity image = (InlinePictureEntity) picture;

        EntityTag etag = new EntityTag(Integer.toString(new String(image.getContent()).hashCode()));
        Response.ResponseBuilder builder = request.evaluatePreconditions(etag);

        if (builder != null) {
            // Preconditions are not met, returning HTTP 304 'not-modified'
            return builder
                    .cacheControl(cc)
                    .build();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(image.getContent(), 0, image.getContent().length);

        return Response
                .ok()
                .entity(baos)
                .cacheControl(cc)
                .tag(etag)
                .type(image.getType())
                .build();
    }
}
