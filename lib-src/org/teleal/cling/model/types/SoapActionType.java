/*
 * Copyright (C) 2010 Teleal GmbH, Switzerland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.teleal.cling.model.types;

import org.teleal.cling.model.Constants;
import org.teleal.cling.model.ModelUtil;

import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class SoapActionType {

    public static final String MAGIC_CONTROL_NS = "schemas-upnp-org";
    public static final String MAGIC_CONTROL_TYPE = "control-1-0";

    public static final Pattern PATTERN_MAGIC_CONTROL =
            Pattern.compile(Constants.NS_UPNP_CONTROL_10 +"#("+Constants.REGEX_UDA_NAME+")");

    public static final Pattern PATTERN =
            Pattern.compile("urn:(" + Constants.REGEX_NAMESPACE + "):service:(" + Constants.REGEX_TYPE + "):([0-9]+)#("+Constants.REGEX_UDA_NAME+")");

    private String namespace;
    private String type;
    private String actionName;
    private Integer version;

    public SoapActionType(ServiceType serviceType, String actionName) {
        this(serviceType.getNamespace(), serviceType.getType(), serviceType.getVersion(), actionName);
    }

    public SoapActionType(String namespace, String type, Integer version, String actionName) {
        this.namespace = namespace;
        this.type = type;
        this.version = version;
        this.actionName = actionName;

        if (actionName != null && !ModelUtil.isValidUDAName(actionName)) {
            throw new IllegalArgumentException("Action name contains illegal characters: " + actionName);
        }
    }

    public String getActionName() {
        return actionName;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getType() {
        return type;
    }

    public Integer getVersion() {
        return version;
    }

    public static SoapActionType fromString(String s) throws RuntimeException {
        Matcher magicControlMatcher = SoapActionType.PATTERN_MAGIC_CONTROL.matcher(s);
        if (magicControlMatcher.matches()) {
            return new SoapActionType(MAGIC_CONTROL_NS, MAGIC_CONTROL_TYPE, null, magicControlMatcher.group(1));
        }
        Matcher matcher = SoapActionType.PATTERN.matcher(s);
        if (matcher.matches()) {
            return new SoapActionType(matcher.group(1), matcher.group(2), Integer.valueOf(matcher.group(3)), matcher.group(4));
        } else {
            throw new RuntimeException("Can't parse action type string (namespace/type/version#actionName): " + s);
        }
    }

    public ServiceType getServiceType() {
        if (version == null) return null;
        return new ServiceType(namespace, type, version);
    }

    @Override
    public String toString() {
        return getTypeString() + "#" + getActionName();
    }

    public String getTypeString() {
        if (version == null) {
            return "urn:" + getNamespace() + ":" + getType();
        } else {
            return "urn:" + getNamespace() + ":service:" + getType() + ":" + getVersion();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SoapActionType that = (SoapActionType) o;

        if (!actionName.equals(that.actionName)) return false;
        if (!namespace.equals(that.namespace)) return false;
        if (!type.equals(that.type)) return false;
        if (version != null ? !version.equals(that.version) : that.version != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = namespace.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + actionName.hashCode();
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }

}