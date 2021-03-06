package com.elastic.support.diagnostics.commands;

import com.elastic.support.config.Version;
import com.elastic.support.diagnostics.chain.Command;
import com.elastic.support.diagnostics.chain.DiagnosticContext;
import com.elastic.support.rest.RestClient;
import com.elastic.support.rest.RestResult;
import com.elastic.support.util.JsonYamlUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class UserRoleCheckCmd implements Command {

    Logger logger = LogManager.getLogger(UserRoleCheckCmd.class);

    @Override
    public void execute(DiagnosticContext context) {

        RestClient restClient = context.getEsRestClient();
        String user = context.getDiagnosticInputs().getUser();

        if(StringUtils.isEmpty(user) ){
            return;
        }

        boolean hasAuthorization = false;

        Version version = context.getVersion();
        String url = null;
        int major = version.getMajor();
        int minor = version.getMinor();

        if (major <= 2) {
            url = "/_shield/user/" + user;
        } else if (major <= 6) {
            url = "/_xpack/security/user/" + user;
        } else {
            url = "/_security/user/" + user;
        }

        RestResult result = restClient.execQuery(url);

        if (result.getStatus() == 200) {
            String userJsonString = result.toString();
            JsonNode userNode = JsonYamlUtils.createJsonNodeFromString(userJsonString);
            hasAuthorization = checkForAuth(major, user, userNode);
        }

        context.setAuthorized(hasAuthorization);

    }

    public boolean checkForAuth(int major, String user, JsonNode userNode){

        JsonNode rolesNode = userNode.path(user).path("roles");
        List<String> roles = null;
        boolean hasAuthorization = false;

        if (rolesNode.isArray()) {
            ObjectMapper mapper = new ObjectMapper();
            roles = mapper.convertValue(rolesNode, List.class);

            if (major <= 2) {
                if (roles.contains("admin")) {
                    hasAuthorization = true;
                }
            } else {
                if (roles.contains("superuser")) {
                    hasAuthorization = true;
                }
            }
        }

        return hasAuthorization;

    }
}
