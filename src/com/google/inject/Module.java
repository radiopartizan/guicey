/**
 * Copyright (C) 2006 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.inject;

/**
 * A module contributes a set of configuration data, typically interface
 * bindings, which will be used to create a {@link Injector}. A guice-based
 * application is ultimately composed of little but a set of Modules and some
 * bootstrapping code.
 *
 * <p>Your Module classes can use a more streamlined syntax by extending
 * {@link AbstractModule} rather than implementing this interface directly.
 */
public interface Module {

  /**
   * Contributes bindings and other configurations to a {@code Binder}.
   */
  void configure(Binder binder);
}
