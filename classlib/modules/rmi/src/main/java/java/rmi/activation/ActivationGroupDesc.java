/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.rmi.activation;

import java.io.Serializable;
import java.rmi.MarshalledObject;
import java.util.Arrays;
import java.util.Properties;

public final class ActivationGroupDesc implements Serializable {
    private static final long serialVersionUID = -4936225423168276595L;

    /**
     * The group's fully package qualified class name.
     */
    private String className;

    /**
     * The location from where to load the group's class.
     */
    private String location;

    /**
     * The group's initialization data.
     */
    private MarshalledObject data;

    /**
     * The controlling options for executing the VM in another process.
     */
    private ActivationGroupDesc.CommandEnvironment env;

    /**
     * A properties map which will override those set by default in the
     * subprocess environment
     */
    private Properties props;

    public ActivationGroupDesc(Properties props, ActivationGroupDesc.CommandEnvironment env) {
        this(null, null, null, props, env);
    }

    public ActivationGroupDesc(String className, String codebase, MarshalledObject<?> data,
            Properties props, ActivationGroupDesc.CommandEnvironment env) {
        super();
        this.className = className;
        this.location = codebase;
        this.data = data;
        this.props = props;
        this.env = env;
    }

    public String getClassName() {
        return className;
    }

    public String getLocation() {
        return location;
    }

    public MarshalledObject<?> getData() {
        return data;
    }

    public ActivationGroupDesc.CommandEnvironment getCommandEnvironment() {
        return env;
    }

    public Properties getPropertyOverrides() {
        return props == null ? null : (Properties) props.clone();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((className == null) ? 0 : className.hashCode());
        result = prime * result + ((data == null) ? 0 : data.hashCode());
        result = prime * result + ((env == null) ? 0 : env.hashCode());
        result = prime * result + ((location == null) ? 0 : location.hashCode());
        result = prime * result + ((props == null) ? 0 : props.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ActivationGroupDesc)) {
            return false;
        }
        final ActivationGroupDesc that = (ActivationGroupDesc) obj;
        if (!(className == null ? that.className == null : className.equals(that.className))) {
            return false;
        }
        if (!(data == null ? that.data == null : data.equals(that.data))) {
            return false;
        }
        if (!(env == null ? that.env == null : env.equals(that.env))) {
            return false;
        }
        if (!(location == null ? that.location == null : location.equals(that.location))) {
            return false;
        }
        return (props == null ? that.props == null : props.equals(that.props));
    }

    public static class CommandEnvironment implements Serializable {
        private static final long serialVersionUID = 6165754737887770191L;

        private String command;

        private String options[];

        public CommandEnvironment(String command, String[] options) {
            super();
            this.command = command;
            if (options == null) {
                this.options = null;
            } else {
                this.options = new String[options.length];
                System.arraycopy(options, 0, this.options, 0, options.length);
            }
        }

        public String[] getCommandOptions() {
            if (options == null) {
                return new String[0];
            }
            return options.clone();
        }

        public String getCommandPath() {
            return this.command;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((command == null) ? 0 : command.hashCode());
            result = prime * result + Arrays.hashCode(options);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof CommandEnvironment)) {
                return false;
            }
            final CommandEnvironment that = (CommandEnvironment) obj;
            if (!(command == null ? that.command == null : command.equals(that.command))) {
                return false;
            }
            return Arrays.equals(options, that.options);
        }
    }
}
