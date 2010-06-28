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

package org.teleal.cling.binding.xml;

import org.teleal.cling.binding.staging.MutableAction;
import org.teleal.cling.binding.staging.MutableActionArgument;
import org.teleal.cling.binding.staging.MutableServiceDescriptor;
import org.teleal.cling.binding.staging.MutableStateVariable;
import org.teleal.cling.binding.xml.parser.ATTRIBUTE;
import org.teleal.cling.binding.xml.parser.ELEMENT;
import org.teleal.cling.binding.xml.parser.ServiceDOM;
import org.teleal.cling.binding.xml.parser.ServiceDOMParser;
import org.teleal.cling.binding.xml.parser.ServiceElement;
import org.teleal.cling.model.meta.Action;
import org.teleal.cling.model.meta.ActionArgument;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.meta.StateVariable;
import org.teleal.cling.model.meta.StateVariableAllowedValueRange;
import org.teleal.cling.model.meta.StateVariableEventDetails;
import org.teleal.cling.model.ValidationException;
import org.teleal.cling.model.types.Datatype;
import org.teleal.common.xml.ParserException;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * TODO: Move the validation rules into the model
 */
public class UDA10ServiceDescriptorBinderImpl implements ServiceDescriptorBinder {

    private static Logger log = Logger.getLogger(ServiceDescriptorBinder.class.getName());

    protected final ServiceDOMParser parser = new ServiceDOMParser();

    public ServiceDOMParser getParser() {
        return parser;
    }

    public <S extends Service> S read(Class<S> serviceClass, String descriptorXml)
            throws DescriptorBindingException, ValidationException {

        try {
            log.fine("Reading service from XML descriptor");

            ServiceDOM dom = getParser().parse(descriptorXml, false);

            // Read the XML into a mutable descriptor graph
            MutableServiceDescriptor descriptor = new MutableServiceDescriptor();
            ServiceElement rootElement = dom.getRoot(getParser().createXPath());
            hydrateRoot(descriptor, rootElement);

            // Build the immutable descriptor graph
            return descriptor.build(serviceClass);

        } catch (ValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DescriptorBindingException("Could not parse service descriptor: " + ex.toString(), ex);
        }
    }

    protected void hydrateRoot(MutableServiceDescriptor descriptor, ServiceElement rootElement)
            throws DescriptorBindingException, ParserException {

        // We don't check the XMLNS, nobody bothers anyway...

        if (!rootElement.getElementName().equals(ELEMENT.scpd.name())) {
            throw new DescriptorBindingException("Root element name is not <scpd>: " + rootElement.getElementName());
        }

        ServiceElement[] rootChildren = rootElement.getChildren();
        for (ServiceElement rootChild : rootChildren) {

            if (rootChild.equals(ELEMENT.specVersion)) {
                // We don't care about UDA major/minor specVersion anymore - whoever had the brilliant idea that
                // the spec versions can be declared on devices _AND_ on their services should have their fingers
                // broken so they never touch a keyboard again. But in the name of peace, let's read it...
                hydrateSpecVersion(descriptor, rootChild);
            } else if (rootChild.equals(ELEMENT.actionList)) {
                hydrateActionList(descriptor, rootChild);
            } else if (rootChild.equals(ELEMENT.serviceStateTable)) {
                hydrateServiceStateTableList(descriptor, rootChild);
            } else {
                log.finer("Ignoring unknown element: " + rootChild.getElementName());
            }
        }

    }

    public void hydrateSpecVersion(MutableServiceDescriptor descriptor, ServiceElement specVersionElement)
            throws DescriptorBindingException, ParserException {

        descriptor.udaMajorVersion =
                Integer.valueOf(specVersionElement.getRequiredChild(ELEMENT.major.name()).getContent());
        descriptor.udaMinorVersion =
                Integer.valueOf(specVersionElement.getRequiredChild(ELEMENT.minor.name()).getContent());
    }

    public void hydrateActionList(MutableServiceDescriptor descriptor, ServiceElement actionList) throws DescriptorBindingException {
        for (ServiceElement actionListChild : actionList.getChildren(ELEMENT.action.name())) {
            MutableAction action = new MutableAction();
            hydrateAction(action, actionListChild);
            descriptor.actions.add(action);
        }
    }

    public void hydrateAction(MutableAction action, ServiceElement actionElement) {
        for (ServiceElement actionChild : actionElement.getChildren()) {
            if (actionChild.equals(ELEMENT.name)) {
                action.name = actionChild.getContent();
            } else if (actionChild.equals(ELEMENT.argumentList)) {
                for (ServiceElement argumentElement : actionChild.getChildren(ELEMENT.argument.name())) {
                    MutableActionArgument actionArgument = new MutableActionArgument();
                    hydrateActionArgument(actionArgument, argumentElement);
                    action.arguments.add(actionArgument);
                }
            }
        }
    }

    public void hydrateActionArgument(MutableActionArgument actionArgument, ServiceElement actionArgumentElement) {
        for (ServiceElement actionArgumentChild : actionArgumentElement.getChildren()) {

            if (actionArgumentChild.equals(ELEMENT.name)) {
                actionArgument.name = actionArgumentChild.getContent();
            } else if (actionArgumentChild.equals(ELEMENT.direction)) {
                actionArgument.direction = ActionArgument.Direction.valueOf(actionArgumentChild.getContent().toUpperCase());
            } else if (actionArgumentChild.equals(ELEMENT.relatedStateVariable)) {
                actionArgument.relatedStateVariable = actionArgumentChild.getContent();
            } else if (actionArgumentChild.equals(ELEMENT.retval)) {
                actionArgument.retval = true;
            }
        }
    }

    public void hydrateServiceStateTableList(MutableServiceDescriptor descriptor, ServiceElement serviceStateTableElement) {
        for (ServiceElement stateVariableElement : serviceStateTableElement.getChildren(ELEMENT.stateVariable.name())) {
            MutableStateVariable stateVariable = new MutableStateVariable();
            hydrateStateVariable(stateVariable, stateVariableElement);
            descriptor.stateVariables.add(stateVariable);
        }
    }

    public void hydrateStateVariable(MutableStateVariable stateVariable, ServiceElement stateVariableElement) {

        stateVariable.eventDetails = new StateVariableEventDetails(
                stateVariableElement.getAttribute(ATTRIBUTE.sendEvents.name()) != null &&
                        stateVariableElement.getAttribute(ATTRIBUTE.sendEvents.name()).toUpperCase().equals("YES")
        );

        for (ServiceElement stateVariableChild : stateVariableElement.getChildren()) {

            if (stateVariableChild.equals(ELEMENT.name)) {
                stateVariable.name = stateVariableChild.getContent();
            } else if (stateVariableChild.equals(ELEMENT.dataType)) {
                stateVariable.dataType =
                        Datatype.Builtin.getByDescriptorName(stateVariableChild.getContent()).getDatatype();
            } else if (stateVariableChild.equals(ELEMENT.defaultValue)) {
                stateVariable.defaultValue = stateVariableChild.getContent();
            } else if (stateVariableChild.equals(ELEMENT.allowedValueList)) {

                List<String> allowedValues = new ArrayList();
                for (ServiceElement allowedValue : stateVariableChild.getChildren(ELEMENT.allowedValue.name())) {
                    allowedValues.add(allowedValue.getContent());
                }
                if (allowedValues.size() > 0) {
                    stateVariable.allowedValues = allowedValues.toArray(new String[allowedValues.size()]);
                }

            } else if (stateVariableChild.equals(ELEMENT.allowedValueRange)) {

                // TODO: UPNP VIOLATION: Some devices (Netgear Router again...) send empty elements, so use some sane defaults
                // TODO: UPNP VIOLATION: The WANCommonInterfaceConfig example XML is even wrong, it does not include a <maximum> element!
                Long minimum = 0l;
                Long maximum = Long.MAX_VALUE;
                Long step = 1l;

                for (ServiceElement allowedValueRangeChild : stateVariableChild.getChildren()) {
                    if (allowedValueRangeChild.equals(ELEMENT.minimum)) {
                        try {
                            minimum = Long.valueOf(allowedValueRangeChild.getContent());
                        } catch (Exception ex) {
                        }
                    } else if (allowedValueRangeChild.equals(ELEMENT.maximum)) {
                        try {
                            maximum = Long.valueOf(allowedValueRangeChild.getContent());
                        } catch (Exception ex) {
                        }
                    } else if (allowedValueRangeChild.equals(ELEMENT.step)) {
                        try {
                            step = Long.valueOf(allowedValueRangeChild.getContent());
                        } catch (Exception ex) {
                        }
                    }
                }

                stateVariable.allowedValueRange = new StateVariableAllowedValueRange(minimum, maximum, step);
            }
        }
    }

    public String generate(Service serviceModel) throws DescriptorBindingException {
        try {
            log.fine("Generating XML descriptor from service model: " + serviceModel);

            ServiceDOM dom = getParser().createDocument();
            generateScpd(serviceModel, dom);
            return getParser().print(dom, 4, true);

        } catch (Exception ex) {
            throw new DescriptorBindingException("Could not generate service descriptor: " + ex.toString(), ex);
        }
    }

    private void generateScpd(Service serviceModel, ServiceDOM dom) {

        ServiceElement scpd = dom.createRoot(getParser().createXPath(), ELEMENT.scpd);

        generateSpecVersion(serviceModel, scpd);

        if (serviceModel.getActions().length > 0) {
            generateActionList(serviceModel, scpd);
        }
        generateServiceStateTable(serviceModel, scpd);
    }

    private void generateSpecVersion(Service serviceModel, ServiceElement scpd) {

        ServiceElement specVersion = scpd.createChild(ELEMENT.specVersion);
        specVersion.createChild(ELEMENT.major).setContent(Integer.toString(serviceModel.getVersion().getMajor()));
        specVersion.createChild(ELEMENT.minor).setContent(Integer.toString(serviceModel.getVersion().getMinor()));
    }

    private void generateActionList(Service serviceModel, ServiceElement scpd) {

        ServiceElement actionListElement = scpd.createChild(ELEMENT.actionList);
        for (Action action : serviceModel.getActions()) {
            
            ServiceElement actionElement = actionListElement.createChild(ELEMENT.action);
            actionElement.createChild(ELEMENT.name).setContent(action.getName());
            if (action.hasInputArguments() || action.hasOutputArguments()) {
                ServiceElement argumentList = actionElement.createChild(ELEMENT.argumentList);
                for (ActionArgument actionArgument : action.getArguments()) {
                    generateActionArgument(actionArgument, argumentList);
                }
            }
        }
    }

    private void generateActionArgument(ActionArgument actionArgument, ServiceElement argumentList) {

        ServiceElement actionArgumentElement = argumentList.createChild(ELEMENT.argument);

        actionArgumentElement.createChild(ELEMENT.name).setContent(actionArgument.getName());
        actionArgumentElement.createChild(ELEMENT.direction).setContent(actionArgument.getDirection().toString().toLowerCase());
        actionArgumentElement.createChild(ELEMENT.relatedStateVariable).setContent(actionArgument.getRelatedStateVariableName());

        if (actionArgument.isReturnValue()) {
            actionArgumentElement.createChild(ELEMENT.retval);
        }
    }

    private void generateServiceStateTable(Service serviceModel, ServiceElement scpd) {

        ServiceElement serviceStateTableElement = scpd.createChild(ELEMENT.serviceStateTable);

        for (StateVariable stateVariable : serviceModel.getStateVariables()) {
            generateStateVariable(stateVariable, serviceStateTableElement);
        }
    }

    private void generateStateVariable(StateVariable stateVariable, ServiceElement serviceStateTable) {

        ServiceElement stateVariableElement = serviceStateTable.createChild(ELEMENT.stateVariable);

        stateVariableElement.createChild(ELEMENT.name).setContent(stateVariable.getName());
        stateVariableElement.createChild(ELEMENT.dataType).setContent(stateVariable.getTypeDetails().getDatatype().getBuiltin().getDescriptorName());

        if (stateVariable.getTypeDetails().getDefaultValue() != null) {
            stateVariableElement.createChild(ELEMENT.defaultValue).setContent(stateVariable.getTypeDetails().getDefaultValue());
        }

        if (stateVariable.getTypeDetails().getAllowedValues() != null) {
            ServiceElement allowedValueListElement = stateVariableElement.createChild(ELEMENT.allowedValueList);
            for (String allowedValue : stateVariable.getTypeDetails().getAllowedValues()) {
                allowedValueListElement.createChild(ELEMENT.allowedValue).setContent(allowedValue);
            }
        }

        if (stateVariable.getTypeDetails().getAllowedValueRange() != null) {
            ServiceElement allowedValueRangeElement = stateVariableElement.createChild(ELEMENT.allowedValueRange);

            allowedValueRangeElement.createChild(ELEMENT.minimum)
                    .setContent(Long.toString(stateVariable.getTypeDetails().getAllowedValueRange().getMinimum()));

            allowedValueRangeElement.createChild(ELEMENT.maximum)
                    .setContent(Long.toString(stateVariable.getTypeDetails().getAllowedValueRange().getMaximum()));

            // Default is a step of '1' so we don't have to generate this (really? does anybody read the spec?)
            if (stateVariable.getTypeDetails().getAllowedValueRange().getStep() > 1l) {
                allowedValueRangeElement.createChild(ELEMENT.step)
                        .setContent(Long.toString(stateVariable.getTypeDetails().getAllowedValueRange().getStep()));
            }
        }

        // The default is 'yes' but we generate it anyway just to be sure
        if (stateVariable.getEventDetails().isSendEvents()) {
            stateVariableElement.setAttribute(ATTRIBUTE.sendEvents.name(), "yes");
        } else {
            stateVariableElement.setAttribute(ATTRIBUTE.sendEvents.name(), "no");
        }

    }

}