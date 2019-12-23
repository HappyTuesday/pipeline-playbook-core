package com.yit.deploy.core.model;

import com.yit.deploy.core.function.ClosureWrapper;
import com.yit.deploy.core.function.Lambda;
import com.yit.deploy.core.info.VariableInfo;
import com.yit.deploy.core.variables.SimpleEntry;
import com.yit.deploy.core.variables.variable.*;

import java.util.List;
import java.util.Map;

public enum VariableType {
    abstracted {
        /**
         * convert variable info to variable
         *
         * @param info variable info
         * @return generated variable
         */
        @Override
        public Variable toVariable(VariableInfo info) {
            return new AbstractVariable(info.getVariableName(), info.getId());
        }
    },
    appendedList {
        /**
         * convert variable info to variable
         *
         * @param info variable info
         * @return generated variable
         */
        @Override
        public Variable toVariable(VariableInfo info) {
            return new AppendedListVariable<>((Variable<?>) info.getVariable().toVariable(), info.getVariableName(), info.getId());
        }
    },
    cached {
        /**
         * convert variable info to variable
         *
         * @param info variable info
         * @return generated variable
         */
        @Override
        public Variable toVariable(VariableInfo info) {
            return new CachedVariable<>((Variable<?>) info.getVariable().toVariable(), info.getVariableName(), info.getId());
        }
    },
    cascadeList {
        /**
         * convert variable info to variable
         *
         * @param info variable info
         * @return generated variable
         */
        @Override
        public Variable toVariable(VariableInfo info) {
            return new CascadeListVariable<>(info.getVariableName(), info.getId(), Lambda.map(info.getList(), v -> (Variable<?>) v.toVariable()), null);
        }
    },
    cascadeMap {
        /**
         * convert variable info to variable
         *
         * @param info variable info
         * @return generated variable
         */
        @Override
        public Variable toVariable(VariableInfo info) {
            return new CascadeMapVariable<>(info.getVariableName(), info.getId(), Lambda.map(
                info.getMap().entrySet(),
                entry -> new SimpleEntry<>(entry.getKey(), (Variable<?>) entry.getValue().getVariable().toVariable())),
                null);
        }
    },
    closure {
        /**
         * convert variable info to variable
         *
         * @param info variable info
         * @return generated variable
         */
        @Override
        public Variable toVariable(VariableInfo info) {
            return new ClosureVariable<>(info.getClosure().toClosure(), info.getVariableName(), info.getId());
        }
    },
    encrypted {
        /**
         * convert variable info to variable
         *
         * @param info variable info
         * @return generated variable
         */
        @Override
        public Variable toVariable(VariableInfo info) {
            return new EncryptedVariable(info.getVariable().toVariable(), info.getVariableName(), info.getId());
        }
    },
    expandableList {
        /**
         * convert variable info to variable
         *
         * @param info variable info
         * @return generated variable
         */
        @Override
        public Variable toVariable(VariableInfo info) {
            return new ExpandableListVariable<>((ListVariable<?>) info.getVariable().toVariable(), info.getVariableName(), info.getId());
        }
    },
    expandableMap {
        /**
         * convert variable info to variable
         *
         * @param info variable info
         * @return generated variable
         */
        @Override
        public Variable toVariable(VariableInfo info) {
            return new ExpandableMapVariable<>((MapVariable<?>) info.getVariable().toVariable(), info.getVariableName(), info.getId());
        }
    },
    filterList {
        /**
         * convert variable info to variable
         *
         * @param info variable info
         * @return generated variable
         */
        @Override
        public Variable toVariable(VariableInfo info) {
            return new FilterListVariable<>((ListVariable<?>) info.getVariable().toVariable(), info.getClosure().toClosure(), info.getVariableName(), info.getId());
        }
    },
    filterMap {
        /**
         * convert variable info to variable
         *
         * @param info variable info
         * @return generated variable
         */
        @Override
        public Variable toVariable(VariableInfo info) {
            return new FilterMapVariable<>((MapVariable<?>) info.getVariable().toVariable(), info.getClosure().toClosure(), info.getVariableName(), info.getId());
        }
    },
    lazyList {
        /**
         * convert variable info to variable
         *
         * @param info variable info
         * @return generated variable
         */
        @Override
        public Variable toVariable(VariableInfo info) {
            return new LazyListVariable<>((Variable<List<Object>>) info.getVariable().toVariable(), info.getVariableName(), info.getId());
        }
    },
    lazyMap {
        /**
         * convert variable info to variable
         *
         * @param info variable info
         * @return generated variable
         */
        @Override
        public Variable toVariable(VariableInfo info) {
            return new LazyMapVariable<>((Variable<Map<String,Object>>) info.getVariable().toVariable(), info.getVariableName(), info.getId());
        }
    },
    map2list {
        /**
         * convert variable info to variable
         *
         * @param info variable info
         * @return generated variable
         */
        @Override
        public Variable toVariable(VariableInfo info) {
            return new Map2ListVariable<>((MapVariable<?>) info.getVariable().toVariable(), info.getVariableName(), info.getId());
        }
    },
    simpleList {
        /**
         * convert variable info to variable
         *
         * @param info variable info
         * @return generated variable
         */
        @Override
        public Variable toVariable(VariableInfo info) {
            return new SimpleListVariable<>(Lambda.map(info.getList(), VariableInfo::toVariable));
        }
    },
    simpleMap {
        /**
         * convert variable info to variable
         *
         * @param info variable info
         * @return generated variable
         */
        @Override
        public Variable toVariable(VariableInfo info) {
            return new SimpleMapVariable<>(Lambda.mapValues(info.getMap(), VariableInfo::toVariable));
        }
    },
    simple {
        /**
         * convert variable info to variable
         *
         * @param info variable info
         * @return generated variable
         */
        @Override
        public Variable toVariable(VariableInfo info) {
            return new SimpleVariable<>(info.getValue(), info.getVariableName(), info.getId());
        }
    },
    lazy {
        /**
         * convert variable info to variable
         *
         * @param info variable info
         * @return generated variable
         */
        @Override
        public Variable toVariable(VariableInfo info) {
            return new LazyVariable<>(info.getClosure().toClosure(), info.getVariableName(), info.getId());
        }
    },
    transformList {
        /**
         * convert variable info to variable
         *
         * @param info variable info
         * @return generated variable
         */
        @Override
        public Variable toVariable(VariableInfo info) {
            return new TransformListVariable<>(
                (ListVariable<?>) info.getVariable().toVariable(),
                info.getClosure().toClosure(),
                info.getVariableName(), info.getId()
            );
        }
    },
    transformMap {
        /**
         * convert variable info to variable
         *
         * @param info variable info
         * @return generated variable
         */
        @Override
        public Variable toVariable(VariableInfo info) {
            return new TransformMapVariable<>(
                (MapVariable<?>) info.toVariable(),
                info.getClosure().toClosure(),
                info.getVariableName(),
                info.getId()
            );
        }
    },
    transform {
        /**
         * convert variable info to variable
         *
         * @param info variable info
         * @return generated variable
         */
        @Override
        public Variable toVariable(VariableInfo info) {
            return new TransformVariable<>(
                (Variable<?>) info.getVariable().toVariable(),
                info.getClosure().toClosure(),
                info.getVariableName(),
                info.getId()
            );
        }
    },
    userParameter {
        /**
         * convert variable info to variable
         *
         * @param info variable info
         * @return generated variable
         */
        @Override
        public Variable toVariable(VariableInfo info) {
            return new UserParameterVariable<>(
                (Variable<?>) info.getVariable().toVariable(),
                info.getVariableName(),
                info.getId()
            );
        }
    };

    /**
     * convert variable info to variable
     * @param info variable info
     * @return generated variable
     */
    public abstract Variable toVariable(VariableInfo info);
}
