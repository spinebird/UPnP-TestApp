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

package org.teleal.cling.model.meta;

import org.teleal.cling.model.Validatable;
import org.teleal.cling.model.ValidationError;
import org.teleal.cling.model.types.Datatype;
import org.teleal.cling.model.ModelUtil;

import java.util.ArrayList;
import java.util.List;


public class ActionArgument<S extends Service> implements Validatable {

    public enum Direction {
        IN, OUT
    }

    final private String name;
    final private String relatedStateVariableName;
    final private Direction direction;
    final private boolean returnValue;     // TODO: What is this stuff good for anyway?

    // Package mutable state
    private Action<S> action;

    public ActionArgument(String name, String relatedStateVariableName, Direction direction) {
        this(name, relatedStateVariableName, direction, false);
    }

    public ActionArgument(String name, String relatedStateVariableName, Direction direction, boolean returnValue) {
        this.name = name;
        this.relatedStateVariableName = relatedStateVariableName;
        this.direction = direction;
        this.returnValue = returnValue;
    }

    public String getName() {
        return name;
    }

    public String getRelatedStateVariableName() {
        return relatedStateVariableName;
    }

    public Direction getDirection() {
        return direction;
    }

    public boolean isReturnValue() {
        return returnValue;
    }

    public Action<S> getAction() {
        return action;
    }

    void setAction(Action<S> action) {
        if (this.action != null)
            throw new IllegalStateException("Final value has been set already, model is immutable");
        this.action = action;
    }

    public Datatype getDatatype() {
        return getAction().getService().getDatatype(this);
    }

    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList();

        if (!ModelUtil.isValidUDAName(getName())) {
            errors.add(new ValidationError(
                    getClass(),
                    "name",
                    "Name '"+getName()+"' is not valid, see UDA specification"
            ));
        }

        if (getDirection() == null) {
            errors.add(new ValidationError(
                    getClass(),
                    "direction",
                    "Argument '"+getName()+"' requires a direction, either IN or OUT"
            ));
        }

        if (isReturnValue() && getDirection() != ActionArgument.Direction.OUT) {
            errors.add(new ValidationError(
                    getClass(),
                    "direction",
                    "Return value argument '" + getName() + "' must be direction OUT"
            ));
        }

        return errors;
    }


    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ", " + getDirection() + ") " + getName();
    }
}