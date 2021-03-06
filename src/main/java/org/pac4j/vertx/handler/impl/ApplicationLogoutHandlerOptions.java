/*
  Copyright 2015 - 2015 pac4j organization

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.pac4j.vertx.handler.impl;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Class with fluent API to wrap options which can be supplied to ApplicationLogoutHandler. This approach is consistent
 * with existing vert.x configuration mechanisms for optional configuration.
 * @since 2.1.0
 */
@Accessors(chain = true)
public class ApplicationLogoutHandlerOptions {

    @Getter @Setter
    private String defaultUrl = null;

    @Getter @Setter
    private String logoutUrlPattern = null;
}
